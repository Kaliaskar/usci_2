package kz.bsbnb.usci.wsclient.dao;

import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import kz.bsbnb.usci.util.json.ext.ExtJsList;
import kz.bsbnb.usci.wsclient.model.ctrkgd.Request;
import kz.bsbnb.usci.wsclient.model.ctrkgd.RequestStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
public class KGDDaoImpl implements KGDDao {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final SimpleJdbcInsert requestInsert;

    public KGDDaoImpl(NamedParameterJdbcTemplate npJdbcTemplate, JdbcTemplate jdbcTemplate) {
        this.npJdbcTemplate = npJdbcTemplate;
        this.jdbcTemplate = jdbcTemplate;

        this.requestInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("USCI_WS")
                .withTableName("CTR_KGD")
                .usingColumns("REPORT_DATE", "STATUS_ID", "REQUEST_BODY", "RESPONSE_BODY", "ENTITIES_COUNT", "REQUEST_ID")
                .usingGeneratedKeyColumns("ID");
    }

    @Override
    public List<Map<String, Object>> entityRows(LocalDate reportDate) {
        return npJdbcTemplate.queryForList("select t.curr_trans_date, t.reference, t.cont_sum, t.cont_num, t.cont_date, t.cont_reg_num ,\n" +
                        "                       (select r.code_alpha_2 from eav_data.ref_country r, eav_data.ctr_subject c \n" +
                        "                               where r.entity_id = c.ref_country_id\n" +
                        "                               and c.entity_id = t.beneficiary_id\n" +
                        "                               and c.report_date = (Select max(report_date)\n" +
                        "                                    from eav_data.ctr_subject cs2\n" +
                        "                                   where cs2.entity_id = c.entity_id\n" +
                        "                                     and cs2.report_date <= t.report_date)) as beneficiary_country_code ,\n" +
                        "                       (select r.code from eav_data.ref_residency r, eav_data.ctr_subject c \n" +
                        "                               where r.entity_id = c.ref_residency_id\n" +
                        "                               and c.entity_id = t.beneficiary_id\n" +
                        "                               and c.report_date = (Select max(report_date)\n" +
                        "                                    from eav_data.ctr_subject cs2\n" +
                        "                                   where cs2.entity_id = c.entity_id\n" +
                        "                                     and cs2.report_date <= t.report_date)) as beneficiary_residency_code ,\n" +
                        "                       (select c.name from eav_data.ctr_subject c \n" +
                        "                               where c.entity_id = t.beneficiary_id\n" +
                        "                               and c.report_date = (Select max(report_date)\n" +
                        "                                    from eav_data.ctr_subject cs2\n" +
                        "                                   where cs2.entity_id = c.entity_id\n" +
                        "                                     and cs2.report_date <= t.report_date)) as beneficiary_name ,  \n" +
                        //"                       (select c.bin_iin_id from eav_data.ctr_subject c \n" +
                        "                       (select c.bin_iin from eav_data.ctr_subject c \n" +
                        "                               where c.entity_id = t.beneficiary_id\n" +
                        "                               and c.report_date = (Select max(report_date)\n" +
                        "                                    from eav_data.ctr_subject cs2\n" +
                        "                                   where cs2.entity_id = c.entity_id\n" +
                        "                                     and cs2.report_date <= t.report_date)) as beneficiary_biniin ,  --надо поменять bin_iin_id на bin_iin\n" +
                        "                       (select r.code from eav_data.ref_econ_sector r, eav_data.ctr_subject c \n" +
                        "                               where r.entity_id = c.ref_econ_sector_id\n" +
                        "                               and c.entity_id = t.beneficiary_id\n" +
                        "                               and c.report_date = (Select max(report_date)\n" +
                        "                                    from eav_data.ctr_subject cs2\n" +
                        "                                   where cs2.entity_id = c.entity_id\n" +
                        "                                     and cs2.report_date <= t.report_date)) as beneficiary_econ_sector_code ,  \n" +
                        "                       (select r.code_alpha_2 from eav_data.ref_country r, eav_data.ctr_subject c \n" +
                        "                               where r.entity_id = c.ref_country_id\n" +
                        "                               and c.entity_id = t.sender_id\n" +
                        "                               and c.report_date = (Select max(report_date)\n" +
                        "                                    from eav_data.ctr_subject cs2\n" +
                        "                                   where cs2.entity_id = c.entity_id\n" +
                        "                                     and cs2.report_date <= t.report_date)) as sender_country_code ,\n" +
                        "                       (select r.code from eav_data.ref_residency r, eav_data.ctr_subject c \n" +
                        "                               where r.entity_id = c.ref_residency_id\n" +
                        "                               and c.entity_id = t.sender_id\n" +
                        "                               and c.report_date = (Select max(report_date)\n" +
                        "                                    from eav_data.ctr_subject cs2\n" +
                        "                                   where cs2.entity_id = c.entity_id\n" +
                        "                                     and cs2.report_date <= t.report_date)) as sender_residency_code ,\n" +
                        "                       (select c.name from eav_data.ctr_subject c \n" +
                        "                               where c.entity_id = t.sender_id\n" +
                        "                               and c.report_date = (Select max(report_date)\n" +
                        "                                    from eav_data.ctr_subject cs2\n" +
                        "                                   where cs2.entity_id = c.entity_id\n" +
                        "                                     and cs2.report_date <= t.report_date)) as sender_name ,  \n" +
                        //"                       (select c.bin_iin_id from eav_data.ctr_subject c \n" +
                        "                       (select c.bin_iin from eav_data.ctr_subject c \n" +
                        "                               where c.entity_id = t.sender_id\n" +
                        "                               and c.report_date = (Select max(report_date)\n" +
                        "                                    from eav_data.ctr_subject cs2\n" +
                        "                                   where cs2.entity_id = c.entity_id\n" +
                        "                                     and cs2.report_date <= t.report_date)) as sender_biniin ,  --надо поменять bin_iin_id на bin_iin\n" +
                        "                       (select r.code from eav_data.ref_econ_sector r, eav_data.ctr_subject c \n" +
                        "                               where r.entity_id = c.ref_econ_sector_id\n" +
                        "                               and c.entity_id = t.sender_id\n" +
                        "                               and c.report_date = (Select max(report_date)\n" +
                        "                                    from eav_data.ctr_subject cs2\n" +
                        "                                   where cs2.entity_id = c.entity_id\n" +
                        "                                     and cs2.report_date <= t.report_date)) as sender_econ_sector_code ,\n" +
                        "                       (select r.code from eav_data.ref_curr_trans_ppc r\n" +
                        "                               where r.entity_id = t.ref_curr_trans_ppc_id\n" +
                        "                               and r.report_date = (Select max(report_date)\n" +
                        "                                    from eav_data.ref_curr_trans_ppc cs2\n" +
                        "                                   where cs2.entity_id = r.entity_id\n" +
                        "                                     and cs2.report_date <= t.report_date)) as curr_trans_ppc_code ,\n" +
                        "                       (select r.code from eav_data.ref_currency r\n" +
                        "                               where r.entity_id = t.ref_currency_id\n" +
                        "                               and r.report_date = (Select max(report_date)\n" +
                        "                                    from eav_data.ref_currency cs2\n" +
                        "                                   where cs2.entity_id = r.entity_id\n" +
                        "                                     and cs2.report_date <= t.report_date)) as currency_code ,\n" +
                        "                       (select r.short_name from eav_data.ref_currency r\n" +
                        "                               where r.entity_id = t.ref_currency_id\n" +
                        "                               and r.report_date = (Select max(report_date)\n" +
                        "                                    from eav_data.ref_currency cs2\n" +
                        "                                   where cs2.entity_id = r.entity_id\n" +
                        "                                     and cs2.report_date <= t.report_date)) as currency_name  ----надо поменять name_ru на short_name\n" +
                        "                           from eav_data.ctr_transaction t , reporter.v_currency_usd_tg cur\n" +
                        "                           where t.cont_date is not null \n" +
                        "                           and t.ref_corp_money_trans_id = 1 \n" +
                        "                           and t.ref_curr_trans_code_id in (1,2,3,15,8,14,11,6,5,4,17,22,18,13,12,10,25,9,20,7,19,16,24,27,30,21,28,31,23,26,29,34,33,32,42,43,45,46,48,49,50,51,52,57,58)\n" +
                        "                           and t.entity_id = cur.entity_id\n" +
                        "                           and (t.cont_sum * cur.course_curr /cur.corellation/ cur.course_usd) > 50\n" +
                        "                           and t.report_date = (Select max(report_date)\n" +
                        "                                    from eav_data.ctr_transaction t2\n" +
                        "                                   where t2.entity_id = t.entity_id\n" +
                        "                                     and t2.report_date <= t.report_date)\n" +
                        "                           and not exists (select  *\n" +
                        "                                              from eav_data.ctr_transaction ctr3\n" +
                        "                                             where ctr3.entity_id = t.entity_id\n" +
                        "                                               and ctr3.creditor_id = t.creditor_id\n" +
                        "                                               and ctr3.operation_id = 3)\n" +
                        "                           and t.report_date = :REPORT_DATE" +
                        "                           and t.system_date < to_date('14.10.2019','dd.mm.yyyy')",
                new MapSqlParameterSource("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(reportDate)));
    }

    @Override
    public void insertRequest(Request request) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(request.getReportDate()))
                .addValue("STATUS_ID",  request.getRequestStatus().getId())
                .addValue("REQUEST_BODY", request.getRequestBody())
                .addValue("RESPONSE_BODY", request.getResponseBody())
                .addValue("ENTITIES_COUNT", request.getEntitiesCount())
                .addValue("REQUEST_ID", request.getRequestId());

        int count = requestInsert.execute(params);
        if (count != 1)
            throw new UsciException("Ошибка insert записи в таблицу USCI_WS.CTR_KGD");

    }

    @Override
    public void updateRequest(Request request) {
        int count = npJdbcTemplate.update("update USCI_WS.CTR_KGD\n" +
                        "   set STATUS_ID = :STATUS_ID, ENTITIES_COUNT = :ENTITIES_COUNT, REQUEST_BODY = :REQUEST_BODY, " +
                        "       RESPONSE_BODY = :RESPONSE_BODY, REQUEST_ID = :REQUEST_ID\n" +
                        " where ID = :ID",
                new MapSqlParameterSource("ID", request.getId())
                        .addValue("STATUS_ID", request.getRequestStatus().getId())
                        .addValue("REQUEST_BODY", request.getRequestBody())
                        .addValue("RESPONSE_BODY", request.getResponseBody())
                        .addValue("ENTITIES_COUNT", request.getEntitiesCount())
                        .addValue("REQUEST_ID", request.getRequestId()));

        if (count != 1)
            throw new UsciException("Ошибка update записи в таблице USCI_WS.CTR_KGD");

    }

    @Override
    public List<Request> getCtrRequestList(LocalDate reportDate) {
        List<Request> requestList = new ArrayList<>();

        String query = "select * from USCI_WS.CTR_KGD\n";
        if (reportDate != null)
            query += " where REPORT_DATE = :REPORT_DATE\n";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(reportDate));

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);
        if (rows.isEmpty())
            return Collections.emptyList();

        for (Map<String, Object> row : rows) {
            Request request = getRequestFromJdbcMap(row);
            requestList.add(request);
        }

        return requestList;
    }

    private Request getRequestFromJdbcMap(Map<String, Object> row) {
        Request request = new Request();
        request.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
        request.setReportDate(SqlJdbcConverter.convertToLocalDate(row.get("REPORT_DATE")));
        request.setRequestStatus(RequestStatus.getRequestStatus(SqlJdbcConverter.convertToLong(row.get("STATUS_ID"))));
        request.setRequestBody(String.valueOf(row.get("REQUEST_BODY")));
        request.setResponseBody(String.valueOf(row.get("RESPONSE_BODY")));
        request.setEntitiesCount(SqlJdbcConverter.convertToLong(row.get("ENTITIES_COUNT")));
        return request;
    }
}
