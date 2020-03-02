package kz.bsbnb.usci.core.dao;

import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.respondent.*;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import oracle.jdbc.OracleTypes;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Nurlan Seitkozhayev
 * @author Yernur Bakash
 * @author Olzhas Kaliaskar
 * @author Jandos Iskakov
 */

@Repository
public class ConfirmDaoImpl implements ConfirmDao {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate npJdbcTemplate;

    private SimpleJdbcInsert confirmInsert;
    private SimpleJdbcInsert confirmMessageInsert;
    private SimpleJdbcInsert confirmMessageFileInsert;

    public ConfirmDaoImpl(JdbcTemplate jdbcTemplate,
                          NamedParameterJdbcTemplate npJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.npJdbcTemplate = npJdbcTemplate;

        this.confirmInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("USCI_RSPDENT")
                .withTableName("CONFIRM")
                .usingGeneratedKeyColumns("ID");

        this.confirmMessageInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("USCI_RSPDENT")
                .withTableName("CONFIRM_MESSAGE")
                .usingGeneratedKeyColumns("ID");

        this.confirmMessageFileInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("USCI_RSPDENT")
                .withTableName("CONFIRM_MSG_FILE")
                .usingGeneratedKeyColumns("ID");
    }

    @Override
    public long insertConfirm(Confirm confirm) {
        Number id = confirmInsert.executeAndReturnKey(new MapSqlParameterSource()
                .addValue("USER_ID", confirm.getUserId())
                .addValue("CHANGE_DATE", SqlJdbcConverter.convertToSqlTimestamp(confirm.getChangeDate()))
                .addValue("CREDITOR_ID", confirm.getRespondentId())
                .addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(confirm.getReportDate()))
                .addValue("STATUS_ID", confirm.getStatus().getId())
                .addValue("BEG_DATE", SqlJdbcConverter.convertToSqlTimestamp(confirm.getFirstBatchLoadTime()))
                .addValue("END_DATE", SqlJdbcConverter.convertToSqlTimestamp(confirm.getLastBatchLoadTime()))
                .addValue("PRODUCT_ID", SqlJdbcConverter.convertToLong(confirm.getProductId()))
                .addValue("IS_RECONFIRM", confirm.isReconfirm()? 1: 0));

        confirm.setId(id.longValue());

        return id.longValue();
    }

    @Override
    public Optional<Confirm> getConfirm(long respondentId, LocalDate reportDate, long productId) {
        try {
            List<Map<String, Object>> rows = npJdbcTemplate.queryForList("select * " +
                            "  from USCI_RSPDENT.CONFIRM\n" +
                            " where CREDITOR_ID = :CREDITOR_ID\n" +
                            "   and REPORT_DATE = :REPORT_DATE" +
                            "   and PRODUCT_ID = :PRODUCT_ID",
                    new MapSqlParameterSource("CREDITOR_ID", respondentId)
                            .addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(reportDate))
                            .addValue("PRODUCT_ID", productId));

            if (rows.isEmpty())
                return Optional.empty();

            if (rows.size() > 1)
                throw new UsciException("Ошибка нахождения записи в таблице USCI_RSPDENT.CONFIRM");

            return Optional.of(getConfirmFromJdbcMap(rows).get(0));
        } catch(EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Confirm getConfirm(long confirmId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from USCI_RSPDENT.CONFIRM where ID = ?",
                new Object[] {confirmId});
        if (rows.size() != 1)
            throw new UsciException("Ошибка нахождения записи в таблице USCI_RSPDENT.CONFIRM");

        return getConfirmFromJdbcMap(rows).get(0);
    }

    @Override
    @Transactional
    public void addConfirmMessage(ConfirmMessage message) {
        Number messageId = confirmMessageInsert.executeAndReturnKey(new MapSqlParameterSource()
                .addValue("CONFIRM_ID", message.getConfirmId())
                .addValue("USER_ID", message.getUserId())
                .addValue("SEND_DATE", SqlJdbcConverter.convertToSqlTimestamp(message.getSendDate()))
                .addValue("TEXT", message.getText()));

        message.setId(messageId.longValue());

        for (ConfirmMessageFile attachment : message.getFiles()) {
            Number attId = confirmMessageFileInsert.executeAndReturnKey(new MapSqlParameterSource()
                    .addValue("CONFIRM_MSG_ID", message.getId())
                    .addValue("FILE_NAME",attachment.getFileName())
                    .addValue("CONTENT", new SqlLobValue(new ByteArrayInputStream(attachment.getContent()),
                            attachment.getContent().length, new DefaultLobHandler()), OracleTypes.BLOB));

            attachment.setId(attId.longValue());
        }
    }

    @Override
    public void updateConfirm(Confirm confirm) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID", confirm.getId())
                .addValue("USER_ID", confirm.getUserId())
                .addValue("STATUS_ID", confirm.getStatus().getId())
                .addValue("IS_RECONFIRM", confirm.isReconfirm()? 1: 0)
                .addValue("END_DATE", SqlJdbcConverter.convertToSqlTimestamp(confirm.getLastBatchLoadTime()));

        int count = npJdbcTemplate.update("update USCI_RSPDENT.CONFIRM\n" +
                "  set STATUS_ID = :STATUS_ID,\n" +
                "      END_DATE = :END_DATE,\n" +
                "      IS_RECONFIRM = :IS_RECONFIRM,\n" +
                "      USER_ID = :USER_ID\n" +
                "where ID = :ID", params);

        if (count != 1)
            throw new UsciException("Ошибка update записи в таблице USCI_RSPDENT.CONFIRM");
    }

    @Override
    public List<ConfirmMessageJson> getMessagesByConfirmId(long confirmId) {
        List<ConfirmMessageJson> list = new ArrayList<>();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select cf.*, u.FIRST_NAME || ' ' || u.LAST_NAME || ' ' || u.MIDDLE_NAME as USER_NAME\n" +
                "  from USCI_RSPDENT.CONFIRM_MESSAGE cf, USCI_ADM.USERS u\n" +
                " where cf.CONFIRM_ID = ?\n" +
                "   and cf.USER_ID = u.USER_ID", new Object[]{confirmId});

        for (Map<String, Object> row : rows) {
            ConfirmMessageJson confirmMessage = new ConfirmMessageJson();
            confirmMessage.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            confirmMessage.setSendDate(SqlJdbcConverter.convertToLocalDateTime(row.get("SEND_DATE")));
            confirmMessage.setText(SqlJdbcConverter.convertObjectToString(row.get("TEXT")));
            confirmMessage.setUserId(SqlJdbcConverter.convertToLong(row.get("USER_ID")));
            confirmMessage.setUserName(SqlJdbcConverter.convertObjectToString(row.get("USER_NAME")));

            // прикрепляемых данных очень мало вцелом посему можем файлы извлекать по отдельности для каждого сообщения
            confirmMessage.setFiles(getFilesByMessage(confirmMessage.getId()));

            list.add(confirmMessage);
        }

        return list;
    }

    @Override
    public List<ConfirmMsgFileJson> getFilesByMessage(long confirmId) {
        ArrayList<ConfirmMsgFileJson> attachments = new ArrayList<>();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select *\n" +
                "from USCI_RSPDENT.CONFIRM_MSG_FILE\n" +
                "where CONFIRM_MSG_ID = ?\n", new Object[]{confirmId});

        for (Map<String, Object> row : rows) {
            ConfirmMsgFileJson file = new ConfirmMsgFileJson();
            file.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            file.setContent((byte[]) row.get("CONTENT"));
            file.setFileName(SqlJdbcConverter.convertObjectToString(row.get("FILE_NAME")));
            file.setMessageId(SqlJdbcConverter.convertToLong(row.get("CONFIRM_MSG_ID")));
            attachments.add(file);
        }

        return attachments;
    }

    @Override
    public byte[] getMessageFileContent(long fileId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select CONTENT\n" +
                "from USCI_RSPDENT.CONFIRM_MSG_FILE\n" +
                "where ID = ?", new Object[] {fileId});
        if (rows.size() != 1)
            throw new UsciException("Ошибка получение записи ищ таблицы USCI_RSPDENT.CONFIRM_MSG_FILE");

        return (byte[])rows.get(0).get("CONTENT");
    }

    private List<Confirm> getConfirmFromJdbcMap(List<Map<String, Object>> rows) {
        List<Confirm> list = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            Confirm confirm = new Confirm();
            confirm.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            confirm.setFirstBatchLoadTime(SqlJdbcConverter.convertToLocalDateTime(row.get("BEG_DATE")));
            confirm.setLastBatchLoadTime(SqlJdbcConverter.convertToLocalDateTime(row.get("END_DATE")));
            confirm.setProductId(SqlJdbcConverter.convertToLong(row.get("PRODUCT_ID")));
            confirm.setRespondentId(SqlJdbcConverter.convertToLong(row.get("CREDITOR_ID")));
            confirm.setUserId(SqlJdbcConverter.convertToLong(row.get("USER_ID")));
            confirm.setChangeDate(SqlJdbcConverter.convertToLocalDateTime(row.get("CHANGE_DATE")));
            confirm.setReconfirm(SqlJdbcConverter.convertToBoolean(row.get("IS_RECONFIRM")));
            confirm.setStatus(row.get("STATUS_ID") != null?
                    ConfirmStatus.getApprovalStatus(SqlJdbcConverter.convertToLong(row.get("STATUS_ID"))): null);
            confirm.setReportDate(SqlJdbcConverter.convertToLocalDate(row.get("REPORT_DATE")));
            list.add(confirm);
        }

        return list;
    }

}
