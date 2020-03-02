package kz.bsbnb.usci.receiver.processor;

import kz.bsbnb.usci.core.client.RespondentClient;
import kz.bsbnb.usci.core.client.UserClient;
import kz.bsbnb.usci.eav.client.ProductClient;
import kz.bsbnb.usci.model.Constants;
import kz.bsbnb.usci.model.adm.User;
import kz.bsbnb.usci.model.batch.Product;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.respondent.Respondent;
import kz.bsbnb.usci.receiver.batch.service.BatchService;
import kz.bsbnb.usci.receiver.model.*;
import kz.bsbnb.usci.receiver.model.exception.ReceiverException;
import kz.bsbnb.usci.receiver.queue.BatchQueueHolder;
import kz.bsbnb.usci.receiver.reader.ManifestReader;
import kz.bsbnb.usci.receiver.service.MailService;
import kz.bsbnb.usci.receiver.validator.BatchValidator;
import kz.bsbnb.usci.util.client.ConfigClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Aibek Bukabayev
 * @author Maksat Nussipzhan
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

@Service
public class BatchReceiverImpl implements BatchReceiver {
    private static final Logger logger = LoggerFactory.getLogger(BatchReceiverImpl.class);

    private static final DateTimeFormatter BATCH_FILE_DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd-HH.mm.ss.SSS");

    @Value("${batch.uploads.dir}")
    private String uploadsDir;

    private final JdbcTemplate jdbcTemplate;
    private final BatchService batchService;
    private final ReceiverStatusSingleton receiverStatus;
    private final BatchValidator batchValidator;
    private final ProductClient productClient;
    private final UserClient userClient;
    private final RespondentClient respondentClient;
    private final ManifestReader manifestReader;
    private final RestTemplate restTemplate;
    private final BatchQueueHolder batchQueueHolder;
    private final ConfigClient configClient;
    private final MailService mailService;

    public BatchReceiverImpl(JdbcTemplate jdbcTemplate,
                             BatchService batchService,
                             ReceiverStatusSingleton receiverStatus,
                             BatchValidator batchValidator,
                             ProductClient productClient,
                             UserClient userClient,
                             RespondentClient respondentClient,
                             ManifestReader manifestReader,
                             RestTemplate restTemplate,
                             BatchQueueHolder batchQueueHolder,
                             ConfigClient configClient,
                             MailService mailService) {
        this.jdbcTemplate = jdbcTemplate;
        this.batchService = batchService;
        this.receiverStatus = receiverStatus;
        this.batchValidator = batchValidator;
        this.productClient = productClient;
        this.userClient = userClient;
        this.respondentClient = respondentClient;
        this.manifestReader = manifestReader;
        this.restTemplate = restTemplate;
        this.batchQueueHolder = batchQueueHolder;
        this.configClient = configClient;
        this.mailService = mailService;
    }

    @Override
    public boolean processBatch(Batch batch) {
        logger.info("Батч запущен в обработку id = {}", batch.getId());
        putBatchToQueue(batch);
        return true;
    }

    @Override
    public boolean processBatch(long batchId) {
        logger.info("Батч запущен в обработку id = {}", batchId);

        Batch batch = batchService.getBatch(batchId);

        if (batch.isMaintenance() && !batch.isMaintenanceApproved()) {
            mailService.notifyRegulatorMaintenance(batch);
        }
        putBatchToQueue(batch);
        return true;
    }

    @Override
    public void declineMaintenanceBatch(long batchId) {
        batchService.addBatchStatus(new BatchStatus(batchId, BatchStatusType.MAINTENANCE_DECLINED));
    }

    @Override
    public void cancelBatch(long batchId) {
        batchService.addBatchStatus(new BatchStatus(batchId, BatchStatusType.CANCELLED));
    }

    @Override
    @Async
    public void receiveBatch(BatchFile batchFile) {
        Objects.requireNonNull(batchFile.getUserId(), "UserId батча отсутствует");
        Objects.requireNonNull(batchFile.getFileName(), "FileName батча отсутствует");

        logger.info("Батч принят в обработку; fileName = {}, userId = {}", batchFile.getFileName(), batchFile.getUserId());

        Batch batch = new Batch();

        try {
            batch.setUserId(batchFile.getUserId());
            batch.setReceiptDate(LocalDateTime.now());
            batch.setBatchName(batchFile.getFileName());
            batch.setBatchEntryId(batchFile.getBatchEntryId());
            batch.setFilePath(batchFile.getFilePath());

            // сохраняем информацию по батчу непосредственно в БД,
            // затем будем дополнять информацию по батчу, пока респондент пользователя не определен
            batchService.save(batch);

            logger.info("Батч {} сохранен в БД id = {} ", batchFile.getFileName(), batch.getId());

            Respondent respondent = respondentClient.getRespondentByUser(new User(batchFile.getUserId(), batchFile.getNb()));
            if (respondent == null)
                throw new UsciException("Ошибка определения респондента пользователя");

            batchFile.setRespondentId(respondent.getId());

            // batchEntry хранятся в отдельных каталогах; батчи от респондентов загружаем на файловый сервер
            if (batchFile.getBatchEntryId() == null)
                batchFile.setFilePath(saveFileOnDisk(batchFile));

            batch.setFilePath(batchFile.getFilePath());
            batch.setBatchName(batchFile.getFilePath().substring(batchFile.getFilePath().lastIndexOf('/') + 1));

            // присвоение respondentId в системе идет два раза
            // первое здесь и второе в validateBatch методе где сверяется респондент(кредитор) из батча и таблицы адинистрирования
            batch.setRespondentId(batchFile.getNb()? Constants.NBK_AS_RESPONDENT_ID : batchFile.getRespondentId());

            // еще раз обновляем статус батча в БД перед парсингом манифест файла
            // так как могут быть ошибки во время парсинга манифеста и изменения пропадут
            batchService.save(batch);

            // вытягиваем базовую информацию из батча, то есть мы полностью его не парсим и не анализируем
            // нам нужно первоначально узнать базовую информацию по кредитору, отчетной дате и тд
            manifestReader.read(batch);

            // после того как мы распарсили манифести файл или тег <info> и получили необходимую информацию
            // обязательно заливаем изменения по батчу в БД чтобы зафиксировать их
            batchService.save(batch);

            boolean validBatch = validateBatch(batch);

            // сохраняю батч файл в каталоге батчей на физическом сервере
            batch = batchService.uploadBatch(batch);


            if (validBatch) {
                // заливаем утверждение от кредитора (кроме батчей НБРК: справочники и тд)
                if (!batch.getRespondentId().equals(Constants.NBK_AS_RESPONDENT_ID)) {
                    updateConfirm(batch);
                    //checkForApprove(batch);
                }

                // делаем проверку не нужно ли сперва отправить батч на подписание
                if (!waitForSignature(batch)) {
                    if (batch.isMaintenance()) {
                       mailService.notifyRegulatorMaintenance(batch);
                    }
                    putBatchToQueue(batch);
                }
            }
        } catch (UsciException e) {
            failBatchProcess(batch.getId(), e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Ошибка обработки батча; batchId = {}, fileName = {}, userId = {}", batch.getId(), batchFile.getFilePath(), batchFile.getUserId());

            failBatchProcess(batch.getId(), e.getMessage(), e);

            throw new UsciException(e.getMessage());
        }
    }

    private String saveFileOnDisk(BatchFile batchFile) {
        File uploadsDir = new File(this.uploadsDir);
        if (!uploadsDir.exists()) {
            if (!uploadsDir.mkdir()) {
                throw new UsciException("Ошибка создания директорий для загрузки батчей");
            }
        }

        // файлы раскладываю по папочкам, id кредитора
        String respondentsPath = this.uploadsDir  + File.separator + batchFile.getRespondentId() + File.separator;
        File respondentsDir = new File(respondentsPath);
        if (!respondentsDir.exists()) {
            if (!respondentsDir.mkdir()) {
                throw new UsciException("Ошибка создания директорий для загрузки батчей");
            }
        }

        // создаю вложенную директорию текущее время и счетчик
        String rndchars = RandomStringUtils.randomAlphanumeric(16);
        String dirName = LocalDateTime.now().format(BATCH_FILE_DT_FORMATTER) + rndchars;
        respondentsPath = respondentsPath.concat(dirName);
        int counter = 0;
        while ((new File(respondentsPath + (counter == 0 ? "" : counter + "") + File.separator)).exists()) {
            counter++;
        }

        String dirPath = respondentsPath + (counter == 0 ? "" : counter + "") + File.separator;
        File newDirectory = new File(dirPath);
        if (!newDirectory.mkdir()) {
            return null;
        }

        // названия файлов могут содержать не приемлемые символы
        StringBuilder normalFileNameBuilder = new StringBuilder(batchFile.getFileName().length());
        for (int i = 0; i < batchFile.getFileName().length(); i++) {
            char ch = batchFile.getFileName().charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == '.' || ch == '_' || ch == '-') {
                normalFileNameBuilder.append(batchFile.getFileName().charAt(i));
            } else {
                // заменяю не приемлемые символы
                normalFileNameBuilder.append("_");
            }
        }

        if (normalFileNameBuilder.length() == 0) {
            normalFileNameBuilder.append("dummyName");
        }

        normalFileNameBuilder.append(File.separator);
        String normalFileName;
        if (normalFileNameBuilder.length() > 128) {
            normalFileName = normalFileNameBuilder.substring(normalFileNameBuilder.length() - 128,
                    normalFileNameBuilder.length());
        } else {
            normalFileName = normalFileNameBuilder.toString();
        }

        File newFile = new File(dirPath + normalFileName);

        try (FileOutputStream fos = new FileOutputStream(newFile)) {
            fos.write(batchFile.getFileContent());
        } catch (IOException e) {
            throw new UsciException(String.format("Ошибка записи файла на диск %s", batchFile.getFileName()), e);
        }

        return newFile.getAbsolutePath();
    }

    /**
     * Занимается непосредственной обработкой батча; добавляет его в очередь
     */
    private void putBatchToQueue(Batch batch) {
        try {
            batchService.addBatchStatus(new BatchStatus(batch.getId(), BatchStatusType.WAITING));
            if (!batch.getRespondentId().equals(46L) /*&& !batch.getRespondentId().equals(20L) && !batch.getRespondentId().equals(14L) && !batch.getRespondentId().equals(22L)*/) {
                receiverStatus.batchReceived();
                batchQueueHolder.addBatch(batch);
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки батча; batchId = {}", batch.getId());
            failBatchProcess(batch.getId(), e.getMessage(), e);

            throw new UsciException(e.getMessage());
        }
    }

    private boolean validateBatch(Batch batch) {
        logger.info("Идет валидация батча id = {}...", batch.getId());

        try {
            if (StringUtils.isEmpty(batch.getProductCode()))
                throw new ReceiverException("Продукт батча не заполнен");

            // присвоение продукта батчу
            Product product = productClient.findProductByCode(batch.getProductCode());
            if (product == null)
                throw new ReceiverException("Продукт не верный");

            batch.setProduct(product);

            // проверим есть ли права у пользователя загружать батчи по данному продукту
            // продукт справочников на права проверять не будем
            if (!product.getCode().equals("DICT")) {
                Integer count = jdbcTemplate.queryForObject("select count(1)\n " +
                        "  from USCI_ADM.USER_PRODUCT\n" +
                        " where USER_ID = ?\n" +
                        "   and PRODUCT_ID = ?", new Object[] {batch.getUserId(), product.getId()}, Integer.class);
                if (count == 0)
                    throw new ReceiverException("Пользователь не имеет права загружать батчи по продукту");
            }

            if (!Objects.equals(batch.getRespondentId(), Constants.NBK_AS_RESPONDENT_ID)) {
                // проверку респондента и отчетной даты делаем только для респондентов
                List<Respondent> respondents = userClient.getUserRespondentList(batch.getUserId());
                batchValidator.validateUserRespondent(batch, respondents);

                // присваиваю respondentId из обьекта респондент
                if (batch.getRespondent() != null)
                    batch.setRespondentId(batch.getRespondent().getId());
            }
        } catch (ReceiverException e) {
            failBatchProcess(batch.getId(), e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * Создает подтверждение автоматом во время загрузки батча; детали смотри в методе ConfirmService.updateConfirm
     */
    @SuppressWarnings("unchecked")
    private void updateConfirm(Batch batch) {
        logger.info("Сохранение состояния подтверждения batchId = {}", batch.getId());

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://core/confirm/updateConfirm")
                .queryParam("respondentId", batch.getRespondentId())
                .queryParam("reportDate", batch.getReportDate().format(Constants.DATE_FORMATTER_ISO))
                .queryParam("productId", batch.getProduct().getId())
                .queryParam("userId", batch.getUserId());

        Map<String, Object> params = restTemplate.getForObject(builder.toUriString(), Map.class);

        // привидение в string сделано преднамеренно так как rest интерпритирует тип long как integer
        if (params.get("id") instanceof Integer)
            batch.setConfirmId((long)(int)params.get("id"));
        else
            batch.setConfirmId((long)params.get("id"));

        // зафиксируем изменения по id утверждению
        batchService.save(batch);
    }

    //для одобрения по батчам
    private void checkForApprove(Batch batch) {
        logger.info("Проверка отчетной даты батча id = {} на одобрение", batch.getId());

        Long receiptDateMillis = batch.getReceiptDate().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Long reportDateMillis = batch.getReportDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        boolean isMaintenance = productClient.checkForApprove(batch.getProduct().getId(), receiptDateMillis, reportDateMillis);
        if (isMaintenance)
            batch.setMaintenance(true);
        batchService.save(batch);
    }

    /**
     * помечает что обработка батча завершена
     * также фиксирует текст Exception который произошел по батчу
     */
    private void failBatchProcess(Long batchId, String message, Exception e) {
        logger.error("По батчу обнаружены ошибки id = {}, ошибка = {}", batchId, e);

        batchService.addBatchStatus(new BatchStatus()
                .setBatchId(batchId)
                .setStatus(BatchStatusType.ERROR)
                .setText(message)
                .setExceptionTrace(e != null? ExceptionUtils.getStackTrace(e): null)
                .setReceiptDate(LocalDateTime.now()));

        batchService.endBatch(batchId);
    }

    /**
     * проверяет кредитора из батча есть ли он в списке кредиторов которые должны подписывать батчи через ЭЦП
     * */
    private boolean waitForSignature(Batch batch) {
        // батчи НБРК не подписываются ЭЦП
        if (batch.getRespondentId().equals(Constants.NBK_AS_RESPONDENT_ID))
            return false;

        // проверим из настроек если респондент должен подписывать свои батчи, то помечаем батч как ожидающий подпись
        Set<Long> respondentIds = configClient.getDigitalSigningOrgIds();
        if (respondentIds.contains(batch.getRespondentId())) {
            logger.info("Батч помечен на подписание ЭЦП id = {}", batch.getId());

            batchService.addBatchStatus(new BatchStatus(batch.getId(),
                    BatchStatusType.WAITING_FOR_SIGNATURE));

            return true;
        }

        return false;
    }

}
