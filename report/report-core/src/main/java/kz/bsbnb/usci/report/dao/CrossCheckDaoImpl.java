package kz.bsbnb.usci.report.dao;

import kz.bsbnb.usci.eav.client.ProductClient;
import kz.bsbnb.usci.eav.service.ProductService;
import kz.bsbnb.usci.model.batch.Product;
import kz.bsbnb.usci.report.crosscheck.CrossCheck;
import kz.bsbnb.usci.report.crosscheck.CrossCheckMessage;
import kz.bsbnb.usci.report.crosscheck.CrossCheckMessageDisplayWrapper;
import kz.bsbnb.usci.report.crosscheck.Message;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import oracle.jdbc.OracleTypes;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class CrossCheckDaoImpl implements  CrossCheckDao{

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final ProductClient productClient;

    public CrossCheckDaoImpl (JdbcTemplate jdbcTemplate,
                              NamedParameterJdbcTemplate npJdbcTemplate,
                              ProductClient productClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.npJdbcTemplate = npJdbcTemplate;
        this.productClient = productClient;
    }

    @Override
    public List<CrossCheck> getCrossChecks(List<Long> creditorIds, LocalDate reportDate, Long productId)  {
        List<CrossCheck> crossCheckList = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("REP_DATE", reportDate)
                .addValue("CREDITOR_IDS", creditorIds)
                .addValue("PRODUCT_ID", productId);

        String query = "SELECT cc.ID, " +
                "              cr.NAME as CREDITOR_NAME, "+
                "              cc.DATE_BEGIN, " +
                "              cc.DATE_END, " +
                "              cc.REPORT_DATE, " +
                "              cc.STATUS_ID, " +
                "              cc.STATUS_NAME, " +
                "              cc.USER_NAME, " +
                "              cc.CREDITOR_ID " +
                "         FROM REPORTER.CROSS_CHECK cc," +
                "              EAV_DATA.REF_RESPONDENT cr " +
                "        WHERE cc.CREDITOR_ID = cr.ENTITY_ID" +
                "          and cc.PRODUCT_ID = :PRODUCT_ID"+
                "          and cc.CREDITOR_ID IN (:CREDITOR_IDS) " +
                "          and cr.REPORT_DATE = (Select max(REPORT_DATE) FROM  EAV_DATA.REF_RESPONDENT  cr2 WHERE cr2.ENTITY_ID = cr.ENTITY_ID and cr2.REPORT_DATE <= :REP_DATE)"+
                "          and cc.REPORT_DATE = :REP_DATE ORDER BY DATE_BEGIN DESC, ID ASC";

        crossCheckList = mapCrossCheckList(npJdbcTemplate.queryForList(query,params));
        return crossCheckList;
    }

    @Override
    public void crossCheck(Long userId, Long creditorId, LocalDate reportDate, long productId, String executeClause)  {
        Product product = productClient.getProductById(productId);
        String packageName = product.getCrosscheckPackageName();

        jdbcTemplate.execute(connection -> {
            CallableStatement stmt = connection.prepareCall("begin\n" +
                    "reporter." + packageName + ".cross_check(p_creditor => ?, " +
                    "                              p_date => ?," +
                    "                              p_user_id => ?," +
                    "                              p_executeClause => ?);\n" +
                    "end;");

            stmt.setLong(1, creditorId != null? creditorId: OracleTypes.NULL);
            stmt.setDate(2,  Date.valueOf(reportDate));
            stmt.setLong(3, userId != null? userId: OracleTypes.NULL);
            stmt.setString(4, executeClause);


            return stmt;
        }, (CallableStatementCallback<Boolean>) PreparedStatement::execute);
    }

    @Override
    public void crossCheckAll(Long userId, LocalDate reportDate, long productId, String executeClause)  {
        Product product = productClient.getProductById(productId);
        String packageName = product.getCrosscheckPackageName() + "." + executeClause;
        jdbcTemplate.execute(connection -> {
            CallableStatement stmt = connection.prepareCall("begin\n" +
                    "REPORTER. " + packageName + "(p_date => ?," +
                    "                              p_user_id => ?);\n" +
                    "end;");

            stmt.setDate(1,  Date.valueOf(reportDate));
            stmt.setLong(2, userId != null? userId: OracleTypes.NULL);


            return stmt;
        }, (CallableStatementCallback<Boolean>) PreparedStatement::execute);
    }

    @Override
    public List<CrossCheckMessageDisplayWrapper> getCrossCheckMessages(@RequestParam  Long crossCheckId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("CROSSCHECK_ID", crossCheckId);

        String query =  "SELECT ID, DESCRIPTION, DIFF, INNER_VALUE, IS_ERROR, OUTER_VALUE, CROSS_CHECK_ID, MESSAGE_ID " +
                "FROM reporter.CROSS_CHECK_MESSAGE " +
                "WHERE CROSS_CHECK_ID = :CROSSCHECK_ID ORDER BY ID ASC";

        List<CrossCheckMessage> cList  = mapCrossCheckMessage(npJdbcTemplate.queryForList(query,params));

        ArrayList<CrossCheckMessageDisplayWrapper> result  = new ArrayList<>(cList.size());

        for (CrossCheckMessage crossCheckMessage : cList)
            result.add(new CrossCheckMessageDisplayWrapper(crossCheckMessage));
        return  result;
    }


    @Override
    public LocalDate getFirstNotApprovedDate(Long creditorId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("CREDITOR_ID", creditorId);

        String query = "SELECT MAX(REPORT_DATE) AS MAX_CHANGE_DATE FROM APPROVAL_DATE@usci_batch where creditor_id= :creditorId + and status_id not in (5,6,7)";
        List<LocalDate> rows = npJdbcTemplate.queryForList(query, params, LocalDate.class);

        return rows.get(0);
    }

    @Override
    public LocalDate getLastApprovedDate(Long creditorId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("CREDITOR_ID", creditorId);

        String query = "SELECT MAX(REPORT_DATE) AS MAX_CHANGE_DATE FROM APPROVAL_DATE@usci_batch where creditor_id= :creditorId + and status_id in (5,6,7)";
        List<LocalDate> rows = npJdbcTemplate.queryForList(query, params, LocalDate.class);

        return rows.get(0);
    }

    @Override
    public CrossCheck getCrossCheck(Long crossCheckId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("CROSSCHECK_ID", crossCheckId);
        String query = "SELECT cc.ID, " +
                "              cr.NAME as CREDITOR_NAME, "+
                "              cc.DATE_BEGIN, " +
                "              cc.DATE_END, " +
                "              cc.REPORT_DATE, " +
                "              cc.STATUS_ID, " +
                "              cc.STATUS_NAME, " +
                "              cc.USER_NAME, " +
                "              cc.CREDITOR_ID " +
                "         FROM REPORTER.CROSS_CHECK cc," +
                "              EAV_DATA.REF_RESPONDENT cr " +
                "        WHERE cc.CREDITOR_ID = cr.ENTITY_ID"+
                "          and cc.ID = :CROSSCHECK_ID" +
                "          and cr.REPORT_DATE = (Select max(REPORT_DATE) FROM  EAV_DATA.REF_RESPONDENT  cr2 WHERE cr2.ENTITY_ID = cr.ENTITY_ID)"+
                "              ORDER BY DATE_BEGIN DESC, ID ASC";
        List<Map<String, Object>> rows = (npJdbcTemplate.queryForList(query,params));

        if (rows == null || rows.isEmpty())
            return null;

        Map<String, Object> row = rows.get(0);
        return mapCrossCheck(row);
    }

    @Override
    public  Message getMessage(Long messageId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("MESSAGE_ID", messageId);
        String query = "SELECT ID, CODE, NAME_RU, NAME_KZ, NOTE FROM reporter.MESSAGE WHERE ID = :MESSAGE_ID";

        List<Map<String, Object>> rows = (npJdbcTemplate.queryForList(query,params));
        if (rows == null || rows.isEmpty())
            return null;

        Map<String, Object> row = rows.get(0);
        return mapMessage(row);
    }


    private static CrossCheck mapCrossCheck(Map<String, Object> row) {
        CrossCheck crossCheck = new CrossCheck();

        crossCheck.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
        crossCheck.setDateBegin(SqlJdbcConverter.convertToLocalDateTime(row.get("DATE_BEGIN")));
        crossCheck.setDateEnd(SqlJdbcConverter.convertToLocalDateTime(row.get("DATE_END")));
        crossCheck.setReportDate(SqlJdbcConverter.convertToLocalDate(row.get("REPORT_DATE")));
        crossCheck.setStatus(SqlJdbcConverter.convertToLong(row.get("STATUS_ID")));
        crossCheck.setCreditorId(SqlJdbcConverter.convertToLong(row.get("CREDITOR_ID")));
        crossCheck.setStatusName(SqlJdbcConverter.convertObjectToString(row.get("STATUS_NAME")));
        crossCheck.setUsername(SqlJdbcConverter.convertObjectToString(row.get("USER_NAME")));
        crossCheck.setCreditorName(SqlJdbcConverter.convertObjectToString(row.get("CREDITOR_NAME")));

        return crossCheck;
    }

    private static Message mapMessage(Map<String, Object> row) {
        Message message = new Message();
        message.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
        message.setCode(SqlJdbcConverter.convertObjectToString(row.get("CODE")));
        message.setNameRu(SqlJdbcConverter.convertObjectToString(row.get("NAME_RU")));
        message.setNameKz(SqlJdbcConverter.convertObjectToString(row.get("NAME_KZ")));
        message.setNote(SqlJdbcConverter.convertObjectToString(row.get("NOTE")));
        return message;
    }

    private List<CrossCheck> mapCrossCheckList(List<Map<String, Object>> rows) {
        List<CrossCheck> list = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            CrossCheck crossCheck = new CrossCheck();
            crossCheck.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            crossCheck.setDateBegin(SqlJdbcConverter.convertToLocalDateTime(row.get("DATE_BEGIN")));
            crossCheck.setDateEnd(SqlJdbcConverter.convertToLocalDateTime(row.get("DATE_END")));
            crossCheck.setReportDate(SqlJdbcConverter.convertToLocalDate(row.get("REPORT_DATE")));
            crossCheck.setStatus(SqlJdbcConverter.convertToLong(row.get("STATUS_ID")));
            crossCheck.setCreditorId(SqlJdbcConverter.convertToLong(row.get("CREDITOR_ID")));
            crossCheck.setStatusName(SqlJdbcConverter.convertObjectToString(row.get("STATUS_NAME")));
            crossCheck.setUsername(SqlJdbcConverter.convertObjectToString(row.get("USER_NAME")));
            crossCheck.setCreditorName(SqlJdbcConverter.convertObjectToString(row.get("CREDITOR_NAME")));
            list.add(crossCheck);
        }
        return list;
    }

    private List<CrossCheckMessage> mapCrossCheckMessage(List<Map<String, Object>> rows) {
        List<CrossCheckMessage> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            CrossCheckMessage crossCheckMessage = new CrossCheckMessage();
            crossCheckMessage.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            crossCheckMessage.setInnerValue(SqlJdbcConverter.convertObjectToString(row.get("INNER_VALUE")));
            crossCheckMessage.setOuterValue(SqlJdbcConverter.convertObjectToString(row.get("OUTER_VALUE")));
            crossCheckMessage.setDiff(SqlJdbcConverter.convertObjectToString(row.get("DIFF")));
            crossCheckMessage.setDescription(SqlJdbcConverter.convertObjectToString(row.get("DESCRIPTION")));
            crossCheckMessage.setIsError(SqlJdbcConverter.convertToLong(row.get("IS_ERROR")));
            crossCheckMessage.setCrossCheckId(SqlJdbcConverter.convertToLong(row.get("CROSS_CHECK_ID")));
            crossCheckMessage.setMessageId(SqlJdbcConverter.convertToLong(row.get("MESSAGE_ID")));

            if (crossCheckMessage.getMessageId() != null)
                crossCheckMessage.setMessage(getMessage(crossCheckMessage.getMessageId()));

            list.add(crossCheckMessage);
        }
        return list;
    }
}
