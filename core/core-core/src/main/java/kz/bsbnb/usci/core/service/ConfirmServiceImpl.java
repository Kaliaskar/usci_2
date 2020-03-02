package kz.bsbnb.usci.core.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import kz.bsbnb.usci.core.dao.ConfirmDao;
import kz.bsbnb.usci.core.dao.ConfirmStageDao;
import kz.bsbnb.usci.eav.model.meta.PeriodType;
import kz.bsbnb.usci.eav.service.ProductService;
import kz.bsbnb.usci.mail.client.MailClient;
import kz.bsbnb.usci.mail.model.dto.MailMessageDto;
import kz.bsbnb.usci.model.adm.Position;
import kz.bsbnb.usci.model.adm.User;
import kz.bsbnb.usci.model.batch.Product;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.respondent.*;
import kz.bsbnb.usci.model.util.Text;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import kz.bsbnb.usci.util.client.ConfigClient;
import kz.bsbnb.usci.util.client.TextClient;
import kz.bsbnb.usci.util.json.ext.ExtJsList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jandos Iskakov
 * @author Olzhas Kaliaskar
 */

@Service
public class ConfirmServiceImpl implements ConfirmService {
    private static final Logger logger = LoggerFactory.getLogger(ConfirmServiceImpl.class);

    private final ConfirmDao confirmDao;
    private final ConfigClient configClient;
    private final RespondentService respondentService;
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ConfirmStageDao confirmStageDao;
    private final UserService userService;
    private final ProductService productService;
    private final PositionService positionService;
    private final SignatureChecker signatureChecker;
    private final MailClient mailClient;
    private final TextClient textClient;

    public ConfirmServiceImpl(ConfirmDao confirmDao,
                              ConfigClient configClient,
                              RespondentService respondentService,
                              NamedParameterJdbcTemplate npJdbcTemplate,
                              JdbcTemplate jdbcTemplate,
                              ConfirmStageDao confirmStageDao,
                              UserService userService,
                              ProductService productService,
                              PositionService positionService,
                              SignatureChecker signatureChecker,
                              MailClient mailClient,
                              TextClient textClient) {
        this.npJdbcTemplate = npJdbcTemplate;
        this.confirmDao = confirmDao;
        this.configClient = configClient;
        this.respondentService = respondentService;
        this.jdbcTemplate = jdbcTemplate;
        this.confirmStageDao = confirmStageDao;
        this.userService = userService;
        this.productService = productService;
        this.positionService = positionService;
        this.signatureChecker = signatureChecker;
        this.mailClient = mailClient;
        this.textClient = textClient;
    }

    /**
     * Определяет отчетную дату подтверждения
     */
    private LocalDate getConfirmDate(LocalDate reportDate, long subjectTypeId, long productId) {
        PeriodType periodType;

        try {
            periodType = PeriodType.getPeriodType(jdbcTemplate.queryForObject("select PERIOD_ID\n " +
                    "  from USCI_RSPDENT.SUBJECT_TYPE_PERIOD\n " +
                    "where SUBJECT_TYPE_ID = ?\n" +
                    "  and PRODUCT_ID = ?", new Object[]{subjectTypeId, productId}, Long.class));
        } catch (EmptyResultDataAccessException e) {
            throw new UsciException("Тип субьекта не настроен на периодичность по продукту");
        }

        if (reportDate.getDayOfMonth() == 1)
            return reportDate;

        LocalDate confirmDate = reportDate;

        if (periodType == PeriodType.DAY) {
            confirmDate = reportDate;
        } else if (periodType == PeriodType.MONTH) {
            confirmDate = confirmDate.plusMonths(1);
            confirmDate = confirmDate.withDayOfMonth(1);
        } else if (periodType == PeriodType.QUARTER) {
            int month = (confirmDate.getMonthValue()/3) + 1;
            confirmDate = LocalDate.of(confirmDate.getYear(), month, 1);
        } else if (periodType == PeriodType.YEAR) {
            confirmDate = confirmDate.plusYears(1);
            confirmDate = confirmDate.withDayOfYear(1);
        }

        return confirmDate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateConfirm(long respondentId, LocalDate repDate, long productId, long userId) {
        Map<String, Object> params = new HashMap<>();

        Respondent respondent = respondentService.getRespondent(respondentId);

        long subjectTypeId = respondent.getSubjectType().getId();

        LocalDate confirmDate = getConfirmDate(repDate, subjectTypeId, productId);
        Optional<Confirm> optConfirm = confirmDao.getConfirm(respondentId, confirmDate, productId);

        if (optConfirm.isPresent()) {
            Confirm confirm = optConfirm.get();
            params.put("id", confirm.getId());

            // обновляем состояние подтверждения; сбрасываем статус на "не подтвержден"
            // устанавливаем последнюю дату загрузки батча
            confirm.setUserId(userId);
            confirm.setChangeDate(LocalDateTime.now());
            confirm.setLastBatchLoadTime(LocalDateTime.now());

            confirmDao.updateConfirm(confirm);
        } else {
            Confirm newConfirm = new Confirm();
            newConfirm.setRespondentId(respondentId);
            newConfirm.setStatus(ConfirmStatus.NOT_CONFIRMED);
            newConfirm.setChangeDate(LocalDateTime.now());
            // дата первой и последней загрузки батча за отчетный период
            newConfirm.setFirstBatchLoadTime(LocalDateTime.now());
            newConfirm.setLastBatchLoadTime(LocalDateTime.now());
            newConfirm.setProductId(productId);
            newConfirm.setUserId(userId);
            newConfirm.setReportDate(confirmDate);

            confirmDao.insertConfirm(newConfirm);

            params.put("id", newConfirm.getId());
        }

        return params;
    }

    @Override
    public ExtJsList getConfirmJsonList(long userId, Boolean isNb, List<Long> respondentIds, LocalDate reportDate,
                                        Integer pageIndex, Integer pageSize) {
        if (!isNb) {
            // респонденты имеют право на просмотр только своих записей
            Respondent respondent = respondentService.getRespondentByUserId(userId, false);
            if (respondent == null)
                throw new UsciException("Ошибка определения респондента пользователя");

            respondentIds = new ArrayList<>(Collections.singletonList(respondent.getId()));
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("CREDITOR_IDS", respondentIds)
                .addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(reportDate))
                .addValue("PAGE_OFFSET", (pageIndex - 1) * pageSize)
                .addValue("PAGE_SIZE", pageSize)
                .addValue("USER_ID", userId);

        List<Respondent> respondents = respondentService.getRespondentList();
        Map<Long, Respondent> creditorMap = respondents.stream().collect(Collectors.toMap(Respondent::getId, o-> o));

        String countQuery = "select count(1)\n" +
                "  from USCI_RSPDENT.CONFIRM cf\n" +
                " where cf.CREDITOR_ID in (:CREDITOR_IDS)\n";

        String fetchQuery = "select cf.*, sts.NAME_RU as STATUS_NAME,\n" +
                            "       u.FIRST_NAME || ' ' || u.LAST_NAME || ' ' || u.MIDDLE_NAME as USER_NAME,\n" +
                            "       pd.NAME as PRODUCT_NAME\n" +
                            "  from USCI_RSPDENT.CONFIRM cf,\n" +
                            "       USCI_ADM.USERS u,\n" +
                            "       USCI_UTIL.CONSTANT_ sts,\n" +
                            "       EAV_CORE.EAV_M_PRODUCT pd\n" +
                            " where cf.CREDITOR_ID in (:CREDITOR_IDS)\n" +
                            "   and cf.USER_ID = u.USER_ID(+)\n" +
                            "   and cf.STATUS_ID = sts.ID(+)\n" +
                            "   and cf.PRODUCT_ID = pd.ID(+)\n";

        //if (!isNb) {
            fetchQuery += "  and cf.PRODUCT_ID in (select up.PRODUCT_ID from USCI_ADM.USER_PRODUCT up where USER_ID = :USER_ID)\n";
            countQuery += "  and cf.PRODUCT_ID in (select up.PRODUCT_ID from USCI_ADM.USER_PRODUCT up where USER_ID = :USER_ID)\n";
        //}

        if (reportDate != null) {
            fetchQuery += "   and cf.REPORT_DATE = :REPORT_DATE\n";
            countQuery += "   and cf.REPORT_DATE = :REPORT_DATE\n";
        }

        fetchQuery += "order by cf.REPORT_DATE desc\n";

        String pagedQuery = "select * from (" + fetchQuery + ")\n" +
                "offset :PAGE_OFFSET rows fetch next :PAGE_SIZE rows only\n";

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(pagedQuery, params);

        List<ConfirmJson> list = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            ConfirmJson confirm = getConfirmJsonFromJdbcMap(row);
            confirm.setRespondentName(creditorMap.get(confirm.getRespondentId()).getName());

            list.add(confirm);
        }

        int count = npJdbcTemplate.queryForObject(countQuery, params, Integer.class);

        return new ExtJsList(list, count);
    }

    @Override
    public ConfirmJson getConfirmJson(long confirmId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select cf.*, sts.NAME_RU as STATUS_NAME,\n" +
                "       u.FIRST_NAME || ' ' || u.LAST_NAME || ' ' || u.MIDDLE_NAME as USER_NAME,\n" +
                "       pd.NAME as PRODUCT_NAME\n" +
                "  from USCI_RSPDENT.CONFIRM cf,\n" +
                "       USCI_ADM.USERS u,\n" +
                "       USCI_UTIL.CONSTANT_ sts,\n" +
                "       EAV_CORE.EAV_M_PRODUCT pd\n" +
                " where cf.ID = ?\n" +
                "   and cf.USER_ID = u.USER_ID(+)\n" +
                "   and cf.STATUS_ID = sts.ID(+)" +
                "   and cf.PRODUCT_ID = pd.ID(+)", new Object[] {confirmId});

        if (rows.isEmpty())
            throw new UsciException("Ошибка получения данных из таблицы USCI_BATCH.USCI_RSPDENT.CONFIRM");

        ConfirmJson confirm = getConfirmJsonFromJdbcMap(rows.get(0));

        try {
            Long crossCheckStatusId = jdbcTemplate.queryForObject("select STATUS_ID\n " +
                    "from (select STATUS_ID\n" +
                    "        from REPORTER.CROSS_CHECK\n" +
                    "       where CREDITOR_ID = ?\n" +
                    "         and REPORT_DATE = ?\n " +
                    "       order by DATE_BEGIN desc)\n" +
                    " where rownum <= 1",
                    new Object[] {confirm.getRespondentId(), SqlJdbcConverter.convertToSqlDate(confirm.getReportDate())},
                    Long.class);

            confirm.setCrossCheckStatusId(crossCheckStatusId);
            confirm.setCrossCheckStatusName(crossCheckStatusId == 2? "Отконтролирован с ошибками": "Отконтролирован без ошибок");
        } catch (EmptyResultDataAccessException e) {
            confirm.setCrossCheckStatusId(-1L);
            confirm.setCrossCheckStatusName("Не проводился");
        }

        return confirm;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(long userId, long confirmId, String documentHash, String signature, String userName) {
        Confirm confirm = confirmDao.getConfirm(confirmId);

        if (confirm.getStatus() == ConfirmStatus.CONFIRMED)
            throw new UsciException("Отчет уже подтвержден.");

        Respondent respondent = respondentService.getRespondent(confirm.getRespondentId());

        ConfirmStage lastConfirmStage = confirmStageDao.getLastConfirmStage(confirm.getId());

        ConfirmStage newConfirmStage = new ConfirmStage()
                .setConfirmId(confirm.getId())
                .setStageDate(LocalDateTime.now())
                .setSignature(signature)
                .setStatus(ConfirmStatus.CONFIRMED)
                .setUserId(userId)
                .setDocHash(documentHash);

        Product product = productService.getProductById(confirm.getProductId());

        // проверяем предусматривает ли продукт чтобы подтверждение подписывали
        if (product.isConfirmWithSignature()) {
            Map<String, Object> params = signatureChecker.checkAndUpdate(respondent.getBin(), documentHash, signature);

            newConfirmStage.setSignInfo((String)params.get("info"));
            newConfirmStage.setDocument(createConfirmDocument(confirmId, userId, userName));
        }

        Map<Long, Position> positions = positionService.getPositionList().stream()
                .collect(Collectors.toMap(Position::getId, o-> o));

        List<Position> productPositions = product.getConfirmPositionIds().stream()
                .map(positions::get)
                .sorted(Comparator.comparing(Position::getLevel))
                .collect(Collectors.toList());

        if (productPositions.isEmpty())
            throw new UsciException("Ошибка подтверждения. Продукт не настроен на подтверждение");

        // если юзера не завели в таблице; он не имеет права подтверждать по продукту
        Optional<Set<Long>> optUserPositionIds = userService.getUserProductPositionIds(userId, product.getId());
        if (!optUserPositionIds.isPresent())
            throw new UsciException(String.format("Пользователь не имеет права подтверждать отчеты по данному продукту. s%", userId));

        Set<Long> userPositionIds = optUserPositionIds.get();
        if (userPositionIds.isEmpty())
            throw new UsciException("Настройки пользователя по должностям подтверждения не установлены");

        // проверим есть ли в списке должностей юзер у продукта
        // список должностей должен быть отсортирован по уровню возрастающем
        Set<Position> userPositions = userPositionIds.stream()
                .map(positions::get)
                .collect(Collectors.toSet());

        if (userPositions.stream().noneMatch(productPositions::contains))
            throw new UsciException("Пользователь не имеет права подтверждать отчеты по данному продукту");

        // определим какая должность должна быть следующей после текущего
        Position mustPosition = null;
        if (confirm.getStatus() == ConfirmStatus.NOT_CONFIRMED) {
            mustPosition = productPositions.get(0);
        } else {
            // первый в списке вид подписанта в продукте который больше текущего является следующим подписантом
            Position lastUserPosition = positions.get(lastConfirmStage.getUserPosId());

            for (Position position : productPositions) {
                if (position.getLevel() > lastUserPosition.getLevel()) {
                    mustPosition = position;
                    break;
                }
            }
        }

        if (mustPosition == null)
            throw new UsciException("Ошибка определения следующей должности подтверждающего");

        // если юзер хочет подтвердить перед другим должностным лицом то выдаем ошибку
        // подтверждение выполняется строго по порядку должностных лиц от наименьшего к большому
        if (!userPositions.contains(mustPosition)) {
            throw new UsciException("У должностного лица не достаточно прав на подтверждение." +
                    "Должностное лицо " + mustPosition.getNameRu() + " должно подтвердить");
        }

        // если юзер имеет право подтверждать по нескольким должностям
        Position userPosition = null;
        for (Position position : productPositions) {
            if (position.getLevel() == mustPosition.getLevel()) {
                if (!userPositionIds.contains(position.getId()))
                    break;

                userPosition = position;
            }
        }

        newConfirmStage.setUserPosId(userPosition.getId());
        confirmStageDao.insertConfirmStage(newConfirmStage);

        // обновляем статус самого подтверждения
        confirm.setStatus(ConfirmStatus.CONFIRMING);
        confirm.setUserId(userId);
        confirm.setChangeDate(LocalDateTime.now());

        // если это последний подписант то статус подтверждения завершен
        if (productPositions.get(productPositions.size() - 1).equals(userPosition)) {
            confirm.setStatus(ConfirmStatus.CONFIRMED);

            // сбрасываем признак переподтверждения
            confirm.setReconfirm(false);

            // оповещаем пользователей что отчет подтвержден
            notifyOnConfirmed(confirm);
        }

        confirmDao.updateConfirm(confirm);
    }

    @Override
    public List<ConfirmStageJson> getConfirmStageJsonList(long confirmId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("LANG_ID", 1)
                .addValue("CONFIRM_ID", confirmId);

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList("select cfs.*, \n" +
                "       decode(:LANG_ID, 1, sts.NAME_RU, sts.NAME_KZ) as STATUS_NAME,\n" +
                "       u.FIRST_NAME || ' ' || u.LAST_NAME || ' ' || u.MIDDLE_NAME as USER_NAME,\n" +
                "       decode(:LANG_ID, 1, rp.NAME_RU, rp.NAME_KZ) as USER_POS_NAME\n" +
                "  from USCI_RSPDENT.CONFIRM_STAGE cfs,\n" +
                "       USCI_ADM.REF_POS rp,\n" +
                "       USCI_UTIL.CONSTANT_ sts,\n" +
                "       USCI_ADM.USERS u\n" +
                " where cfs.CONFIRM_ID = :CONFIRM_ID\n" +
                "   and cfs.USER_POS_ID = rp.ID(+)\n" +
                "   and cfs.USER_ID = u.USER_ID(+)\n" +
                "   and cfs.STATUS_ID = sts.ID(+)\n" +
                " order by cfs.STAGE_DATE desc\n", params);

        List<ConfirmStageJson> list = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            ConfirmStageJson confirmStageJson = new ConfirmStageJson();
            confirmStageJson.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            confirmStageJson.setUserId(SqlJdbcConverter.convertToLong(row.get("USER_ID")));
            confirmStageJson.setUserName(SqlJdbcConverter.convertObjectToString(row.get("USER_NAME")));
            confirmStageJson.setConfirmId(SqlJdbcConverter.convertToLong(row.get("ID")));
            confirmStageJson.setUserPosId(SqlJdbcConverter.convertToLong(row.get("USER_POS_ID")));
            confirmStageJson.setUserPosName(SqlJdbcConverter.convertObjectToString(row.get("USER_POS_NAME")));
            confirmStageJson.setStatusId(SqlJdbcConverter.convertToLong(row.get("STATUS_ID")));
            confirmStageJson.setStatusName(SqlJdbcConverter.convertObjectToString(row.get("STATUS_NAME")));
            confirmStageJson.setStageDate(SqlJdbcConverter.convertToLocalDateTime(row.get("STAGE_DATE")));
            list.add(confirmStageJson);
        }

        return list;
    }

    private ConfirmJson getConfirmJsonFromJdbcMap(Map<String, Object> row) {
        ConfirmJson confirmJson = new ConfirmJson();
        confirmJson.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
        confirmJson.setRespondentId(SqlJdbcConverter.convertToLong(row.get("CREDITOR_ID")));
        confirmJson.setUserId(SqlJdbcConverter.convertToLong(row.get("USER_ID")));
        confirmJson.setReportDate(SqlJdbcConverter.convertToLocalDate(row.get("REPORT_DATE")));
        confirmJson.setFirstBatchLoadTime(SqlJdbcConverter.convertToLocalDateTime(row.get("BEG_DATE")));
        confirmJson.setLastBatchLoadTime(SqlJdbcConverter.convertToLocalDateTime(row.get("END_DATE")));
        confirmJson.setReportDate(SqlJdbcConverter.convertToLocalDate(row.get("REPORT_DATE")));
        confirmJson.setStatusId(SqlJdbcConverter.convertToLong(row.get("STATUS_ID")));
        confirmJson.setStatusName(SqlJdbcConverter.convertObjectToString(row.get("STATUS_NAME")));
        confirmJson.setUserName(SqlJdbcConverter.convertObjectToString(row.get("USER_NAME")));
        confirmJson.setProductId(SqlJdbcConverter.convertToLong(row.get("PRODUCT_ID")));
        confirmJson.setProductName(SqlJdbcConverter.convertObjectToString(row.get("PRODUCT_NAME")));
        return confirmJson;
    }

    @Override
    public void addConfirmMessage(ConfirmMessage message) {
        message.setConfirm(confirmDao.getConfirm(message.getConfirmId()));

        confirmDao.addConfirmMessage(message);

        // оповещаем пользователей о новом сообщении
        notifyOnNewMessage(message);
    }

    @Override
    public List<ConfirmMessageJson> getMessagesByConfirmId(long confirmId) {
        return  confirmDao.getMessagesByConfirmId(confirmId);
    }

    @Override
    public List<ConfirmMsgFileJson> getFilesByMessageId(long messageId) {
        return confirmDao.getFilesByMessage(messageId);
    }

    @Override
    public byte[] createConfirmDocument(long confirmId, long userId, String userName) {
        Confirm confirm = confirmDao.getConfirm(confirmId);

        Product product = productService.getProductById(confirm.getProductId());

        User user = userService.getUser(userId);
        if (user == null)
            throw new UsciException("Пользователь не синхронизирован с БД АИП ЕССП");

        String confirmText = configClient.getConfirmText();
        confirmText = confirmText.replace("${REP_DATE}", confirm.getReportDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        confirmText = confirmText.replace("${USER_NAME}", userName);
        confirmText = confirmText.replace("${PROD_NAME}", product.getName());

        byte[] documentBytes = null;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);

            document.open();
            BaseFont baseFont = BaseFont.createFont("fonts/arial.ttf",
                    BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);

            Font font = new Font(baseFont, 16);

            Paragraph header = new Paragraph("Соглашение", font);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Paragraph preface = new Paragraph();
            preface.add(new Paragraph(confirmText, font));

            document.addTitle("Соглашение");
            document.addSubject("Соглашение");
            document.add(preface);

            document.close();

            documentBytes = baos.toByteArray();
        } catch (Exception e) {
            throw new UsciException("Ошибка создания соглашения к подтверждению", e);
        }

        return documentBytes;
    }

    @Override
    public String getConfirmDocumentHash(long confirmId, long userId, String userName) {
        return DigestUtils.md5DigestAsHex(createConfirmDocument(confirmId, userId, userName));
    }

    @Override
    public byte[] getConfirmDocument(long id) {
        return confirmStageDao.getConfirmStage(id).getDocument();
    }

    @Override
    public byte[] getMessageFileContent(long fileId) {
        return confirmDao.getMessageFileContent(fileId);
    }

    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void notifyOnNewMessage(ConfirmMessage confirmMessage) {
        Confirm confirm = confirmMessage.getConfirm();
        Respondent respondent = respondentService.getRespondent(confirm.getRespondentId());
        User user = userService.getUser(confirmMessage.getUserId());

        Text status = textClient.getTextById(confirm.getStatus().getId());

        Map<String, String> params = new HashMap<>();
        params.put("RESPONDENT", respondent.getName());
        params.put("REPORT_DATE", confirm.getReportDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        params.put("STATUS", status.getNameRu());
        params.put("USERNAME", user.getFullName());
        params.put("UPDATE_TIME", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm:ss")));
        params.put("TEXT", confirmMessage.getText());

        List<User> notifyUsers = userService.getRespondentUserList(confirm.getRespondentId());

        for (User receiver : notifyUsers) {
            MailMessageDto mailMessageDto = new MailMessageDto();
            mailMessageDto.setReceiver(receiver);
            mailMessageDto.setMailTemplate("CONFIRM_UPDATE");
            mailMessageDto.setParams(params);

            //TODO: временно отключен в рамках тестирования
            /*try {
                mailClient.sendMail(mailMessageDto);
            } catch (Exception e) {
                logger.error(String.format("Ошибка отправки email %s", mailMessageDto), e);
            }*/
        }
    }

    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void notifyOnConfirmed(Confirm confirm) {
        Respondent respondent = respondentService.getRespondent(confirm.getRespondentId());
        Text status = textClient.getTextById(confirm.getStatus().getId());

        Map<String, String> params = new HashMap<>();
        params.put("RESPONDENT", respondent.getName());
        params.put("REPORT_DATE", confirm.getReportDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        params.put("STATUS", status.getNameRu());

        List<User> notifyUsers = userService.getRespondentUserList(confirm.getRespondentId());

        for (User receiver : notifyUsers) {
            MailMessageDto mailMessageDto = new MailMessageDto();
            mailMessageDto.setReceiver(receiver);
            mailMessageDto.setMailTemplate("REPORT_CONFIRMED");
            mailMessageDto.setParams(params);

            //TODO: временно отключен в рамках тестирования
            /*try {
                mailClient.sendMail(mailMessageDto);
            } catch (Exception e) {
                logger.error(String.format("Ошибка отправки email %s", mailMessageDto), e);
            }*/
        }
    }

}
