package kz.bsbnb.usci.eav.dao;

import com.google.gson.Gson;
import kz.bsbnb.usci.eav.model.base.BaseEntityJson;
import kz.bsbnb.usci.eav.model.base.OperType;
import kz.bsbnb.usci.eav.model.meta.json.EntityExtJsTreeJson;
import kz.bsbnb.usci.eav.service.MetaClassService;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class BaseEntityLoadXmlDaoImpl implements  BaseEntityLoadXmlDao{

    private  final MetaClassService metaClassService;
    private final NamedParameterJdbcTemplate npJdbcTemplate;

    BaseEntityLoadXmlDaoImpl (MetaClassService metaClassService,
                              JdbcTemplate jdbcTemplate) {
        this.metaClassService = metaClassService;
        this.npJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public List<BaseEntityJson> loadEntityForApproval(long batchId) {
        String query = "select /*+parallel(16)*/ t.ID, t.creditor_id, t.report_date, t.entity_id, \n" +
                "                         t.entity_text, t.entity_key, t.operation_id, \n" +
                "                         t.meta_class_id, t.pre_approved, t.pre_declined from EAV_XML.EAV_ENTITY_MAINTENANCE t where t.BATCH_ID = :BATCH_ID \n " +
                "           and t.APPROVED = 0 and t.DECLINED = 0 \n" +
                "           and not exists(Select * from eav_data.eav_entity_status s \n" +
                "                           where s.batch_id = t.batch_id \n" +
                "                           and s.entity_id = t.entity_id \n" +
                "                           and s.status_id in (8,15))";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("BATCH_ID", batchId);

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);

        return getBaseEntityJsonFromJdbcMap(rows);
    }

    @Override
    public EntityExtJsTreeJson loadBaseEntity(Long id) {
        Gson gson = new Gson();
        String query = "Select /*+parallel(16)*/ ENTITY_JSON from EAV_XML.EAV_ENTITY_MAINTENANCE where ID = :ID ";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID", id);

        String entity = npJdbcTemplate.queryForObject(query, params, String.class);

        return gson.fromJson(entity,EntityExtJsTreeJson.class);
    }

    @Override
    public void updateBaseEntityState(BaseEntityJson baseEntityJson) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID", baseEntityJson.getId())
                .addValue("PRE_APPROVED", baseEntityJson.isPreApproved()?1:0)
                .addValue("PRE_DECLINED", baseEntityJson.isPreDeclined()?1:0);

        int count = npJdbcTemplate.update("update /*+parallel(16)*/ EAV_XML.EAV_ENTITY_MAINTENANCE\n" +
                "  set PRE_APPROVED = :PRE_APPROVED,\n" +
                "      PRE_DECLINED = :PRE_DECLINED\n" +
                "where ID = :ID", params);

        if (count == 0)
            throw new UsciException(String.format("Ошибка update записи %s в таблице EAV_XML.EAV_ENTITY_MAINTENANCE", baseEntityJson.getEntityId().toString()));

    }

    @Override
    public void updateBaseEntity(BaseEntityJson baseEntityJson, boolean approve) {
        if (approve) {
            int count = npJdbcTemplate.update("update /*+parallel(16)*/ EAV_XML.EAV_ENTITY_MAINTENANCE\n" +
                            "   set APPROVED = 1 \n" +
                            " where ID = :ID",
                    new MapSqlParameterSource("ID", baseEntityJson.getId()));

            if (count != 1)
                throw new UsciException("Ошибка update записи в таблице EAV_XML.EAV_ENTITY_MAINTENANCE");
        } else {
            int count = npJdbcTemplate.update("update /*+parallel(16)*/ EAV_XML.EAV_ENTITY_MAINTENANCE\n" +
                            "   set DECLINED = 1 \n" +
                            " where ID = :ID",
                    new MapSqlParameterSource("ID", baseEntityJson.getId()));

            if (count != 1)
                throw new UsciException("Ошибка update записи в таблице EAV_XML.EAV_ENTITY_MAINTENANCE");
        }
    }

    @Override
    public void deleteBaseEntity(Long id) {
        int count = npJdbcTemplate.update("delete /*+parallel(16)*/ from EAV_XML.EAV_ENTITY_MAINTENANCE\n" +
                        " where ID = :ID",
                new MapSqlParameterSource("ID", id));

        if (count != 1)
            throw new UsciException("Ошибка delete записи в таблице EAV_XML.EAV_ENTITY_MAINTENANCE");
    }

    public List<BaseEntityJson> getBaseEntityJsonFromJdbcMap(List<Map<String, Object>> rows) {
        List<BaseEntityJson> list = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            BaseEntityJson baseEntityJson = new BaseEntityJson();
            baseEntityJson.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            baseEntityJson.setRespondentId(SqlJdbcConverter.convertToLong(row.get("CREDITOR_ID")));
            baseEntityJson.setReportDate(SqlJdbcConverter.convertToLocalDate(row.get("REPORT_DATE")));
            baseEntityJson.setEntityId(SqlJdbcConverter.convertToLong(row.get("ENTITY_ID")));
            baseEntityJson.setPreApproved(SqlJdbcConverter.convertToBoolean(row.get("PRE_APPROVED")));
            baseEntityJson.setPreDeclined(SqlJdbcConverter.convertToBoolean(row.get("PRE_DECLINED")));
            baseEntityJson.setEntityKey(SqlJdbcConverter.convertObjectToString((row.get("ENTITY_KEY"))));
            baseEntityJson.setEntityText(SqlJdbcConverter.convertObjectToString((row.get("ENTITY_TEXT"))));
            baseEntityJson.setOperType(row.get("OPERATION_ID") != null ? OperType.getOperType(SqlJdbcConverter.convertToShort(row.get("OPERATION_ID")) ) : null);
            baseEntityJson.setMetaClassId(SqlJdbcConverter.convertToLong((row.get("META_CLASS_ID"))));
            list.add(baseEntityJson);
        }
        return  list;
    }
}
