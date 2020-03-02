package kz.bsbnb.usci.eav.dao;

import kz.bsbnb.usci.eav.model.base.OperType;
import kz.bsbnb.usci.eav.model.core.BaseEntityStatus;
import kz.bsbnb.usci.eav.model.core.EntityStatusType;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Maksat Nussipzhan
 * @author Jandos Iskakov
 */

@Repository
public class BaseEntityStatusDaoImpl implements BaseEntityStatusDao {
    private SimpleJdbcInsert baseEntityStatusInsert;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private static final Logger logger = LoggerFactory.getLogger(BaseEntityStatusDaoImpl.class);

    public BaseEntityStatusDaoImpl(JdbcTemplate baseEntityStatusInsert,
                                   JdbcTemplate jdbcTemplate,
                                   NamedParameterJdbcTemplate npJdbcTemplate) {

        this.baseEntityStatusInsert = new SimpleJdbcInsert(baseEntityStatusInsert)
                .withTableName("EAV_ENTITY_STATUS")
                .withSchemaName("EAV_DATA")
                .usingColumns("BATCH_ID", "META_CLASS_ID", "EXCEPTION_TRACE",
                        "ENTITY_INDEX", "ENTITY_ID", "SYSTEM_DATE", "ENTITY_TEXT",
                        "STATUS_ID", "OPERATION_ID", "ERROR_CODE", "ERROR_MESSAGE")
                .usingGeneratedKeyColumns("ID");
        this.jdbcTemplate = jdbcTemplate;
        this.npJdbcTemplate = npJdbcTemplate;
    }

    @Override
    public BaseEntityStatus insert(BaseEntityStatus baseEntityStatus) {
        String exceptionTrace = baseEntityStatus.getExceptionTrace();
        if (exceptionTrace != null && exceptionTrace.length() > 4000)
            exceptionTrace = exceptionTrace.substring(0, 3000);

        Number id = baseEntityStatusInsert
                .executeAndReturnKey(new MapSqlParameterSource("BATCH_ID", baseEntityStatus.getBatchId())
                        .addValue("ENTITY_ID", baseEntityStatus.getEntityId())
                        .addValue("META_CLASS_ID", baseEntityStatus.getMetaClassId())
                        .addValue("SYSTEM_DATE", baseEntityStatus.getSystemDate())
                        .addValue("ENTITY_INDEX", baseEntityStatus.getIndex())
                        .addValue("STATUS_ID", baseEntityStatus.getStatus().getId())
                        .addValue("OPERATION_ID", baseEntityStatus.getOperation() != null ? baseEntityStatus.getOperation().getId() : null)
                        .addValue("ENTITY_TEXT", baseEntityStatus.getEntityText())
                        .addValue("ERROR_CODE", baseEntityStatus.getErrorCode())
                        .addValue("ERROR_MESSAGE", baseEntityStatus.getErrorMessage())
                        .addValue("EXCEPTION_TRACE", exceptionTrace));
        baseEntityStatus.setId(id.longValue());

        return baseEntityStatus;
    }
    //TODO
    /*
       Данный метод реалтзован для времнного решение проблем с очередью.
       Проблема заключается в том, что когда нагрузка большая executeAndReturnKey застревает
     */
    @Override
    public BaseEntityStatus insertBatchMethod(BaseEntityStatus baseEntityStatus) {
        String exceptionTrace = baseEntityStatus.getExceptionTrace();
        String errorMessage = baseEntityStatus.getErrorMessage();
        if (exceptionTrace != null && exceptionTrace.length() > 4000)
            exceptionTrace = exceptionTrace.substring(0, 3000);
        if (errorMessage != null && errorMessage.length() > 4000)
            errorMessage = errorMessage.substring(0, 3000);

        baseEntityStatusInsert
                .executeBatch(new MapSqlParameterSource("BATCH_ID", baseEntityStatus.getBatchId())
                        .addValue("ENTITY_ID", baseEntityStatus.getEntityId())
                        .addValue("META_CLASS_ID", baseEntityStatus.getMetaClassId())
                        .addValue("SYSTEM_DATE", baseEntityStatus.getSystemDate())
                        .addValue("ENTITY_INDEX", baseEntityStatus.getIndex())
                        .addValue("STATUS_ID", baseEntityStatus.getStatus().getId())
                        .addValue("OPERATION_ID", baseEntityStatus.getOperation() != null ? baseEntityStatus.getOperation().getId() : null)
                        .addValue("ENTITY_TEXT", baseEntityStatus.getEntityText())
                        .addValue("ERROR_CODE", baseEntityStatus.getErrorCode())
                        .addValue("ERROR_MESSAGE", errorMessage)
                        .addValue("EXCEPTION_TRACE", exceptionTrace));

        return baseEntityStatus;
    }

    @Override
    public void update(BaseEntityStatus baseEntityStatus) {
        int count = npJdbcTemplate.update("update EAV_DATA.EAV_ENTITY_STATUS\n" +
                        "   set ENTITY_ID = :ENTITY_ID, STATUS_ID = :STATUS_ID, OPERATION_ID = :OPERATION_ID, SYSTEM_DATE = :SYSTEM_DATE, ERROR_MESSAGE = :ERROR_MESSAGE \n" +
                        " where BATCH_ID = :BATCH_ID and ENTITY_ID = :APPROVED_ENTITY_ID and STATUS_ID = 9",
                new MapSqlParameterSource("BATCH_ID", baseEntityStatus.getBatchId())
                        .addValue("ENTITY_ID", baseEntityStatus.getEntityId())
                        .addValue("APPROVED_ENTITY_ID", baseEntityStatus.getApprovedEntityId())
                        .addValue("STATUS_ID", baseEntityStatus.getStatus().getId())
                        .addValue("OPERATION_ID", baseEntityStatus.getOperation() != null ? baseEntityStatus.getOperation().getId() : null)
                        .addValue("SYSTEM_DATE", baseEntityStatus.getSystemDate())
                        .addValue("ERROR_MESSAGE",baseEntityStatus.getErrorMessage()));

        if (count != 1)
            throw new UsciException("Ошибка update записи в таблице EAV_DATA.EAV_ENTITY_STATUS");
    }

    @Override
    public Object[] getStatusList(Long batchId, List<EntityStatusType> statuses) {
        String selectClause = "select *\n" +
                "  from EAV_DATA.EAV_ENTITY_STATUS t\n";

        String whereClause =  "where BATCH_ID = :batchId\n" +
        "   and STATUS_ID in (:statusIds)\n";

        String fetchQuery = selectClause + whereClause + "order by SYSTEM_DATE desc\n";


        String countQuery = "select count(1) from EAV_DATA.EAV_ENTITY_STATUS\n" + whereClause;

        MapSqlParameterSource params = new MapSqlParameterSource("batchId", batchId)
                .addValue("statusIds", statuses.stream()
                        .map(EntityStatusType::getId)
                        .collect(Collectors.toList()));

        int count = npJdbcTemplate.queryForObject(countQuery, params, Integer.class);

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(fetchQuery, params);

        List<BaseEntityStatus> list = new ArrayList<>();

        for (Map<String, Object> row : rows)
            list.add(getBaseEntityStatusFromJdbcMap(row));

        return new Object[] {list, count};
    }

    @Override
    public int getErrorEntityCount(long batchId) {
        return jdbcTemplate.queryForObject("select count(1) from EAV_DATA.EAV_ENTITY_STATUS where BATCH_ID = ? and STATUS_ID = ?",
                new Object[] {batchId, EntityStatusType.ERROR.getId()}, Integer.class);
    }

    @Override
    public int getSuccessEntityCount(long batchId) {
        return jdbcTemplate.queryForObject("select count(1) from EAV_DATA.EAV_ENTITY_STATUS where BATCH_ID = ? and STATUS_ID = ?",
                new Object[] {batchId, EntityStatusType.COMPLETED.getId()}, Integer.class);
    }

    private BaseEntityStatus getBaseEntityStatusFromJdbcMap(Map<String, Object> row) {
        BaseEntityStatus entityStatus = new BaseEntityStatus();
        entityStatus.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
        entityStatus.setEntityId(SqlJdbcConverter.convertToLong(row.get("ENTITY_ID")));
        entityStatus.setMetaClassId(SqlJdbcConverter.convertToLong(row.get("META_CLASS_ID")));
        entityStatus.setBatchId(SqlJdbcConverter.convertToLong(row.get("BATCH_ID")));
        entityStatus.setIndex(SqlJdbcConverter.convertToLong(row.get("INDEX")));
        entityStatus.setOperation(row.get("OPERATION_ID") != null? OperType.getOperType(SqlJdbcConverter.convertToShort(row.get("OPERATION_ID"))): null);
        entityStatus.setErrorCode(SqlJdbcConverter.convertObjectToString(row.get("ERROR_CODE")));
        entityStatus.setErrorMessage(SqlJdbcConverter.convertObjectToString(row.get("ERROR_MESSAGE")));
        entityStatus.setStatus(EntityStatusType.getEntityStatus(SqlJdbcConverter.convertToInt(row.get("STATUS_ID"))));
        entityStatus.setEntityText(SqlJdbcConverter.convertObjectToString(row.get("ENTITY_TEXT")));
        return entityStatus;
    }


}
