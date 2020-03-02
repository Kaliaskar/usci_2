package kz.bsbnb.usci.core.dao;

import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.respondent.ConfirmStage;
import kz.bsbnb.usci.model.respondent.ConfirmStatus;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import oracle.jdbc.OracleTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

/**
 * @author Jandos Iskakov
 */

@Repository
public class ConfirmStageDaoImpl implements ConfirmStageDao {
    private final JdbcTemplate jdbcTemplate;
    private SimpleJdbcInsert approvalStageInsert;

    public ConfirmStageDaoImpl(JdbcTemplate jdbcTemplate,
                               NamedParameterJdbcTemplate npJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.approvalStageInsert = new SimpleJdbcInsert(npJdbcTemplate.getJdbcTemplate())
                .withSchemaName("USCI_RSPDENT")
                .withTableName("CONFIRM_STAGE")
                .usingGeneratedKeyColumns("ID");
    }

    @Override
    public void insertConfirmStage(ConfirmStage confirmStage) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("CONFIRM_ID", confirmStage.getConfirmId())
                .addValue("STATUS_ID", confirmStage.getStatus().getId())
                .addValue("SIGNATURE", confirmStage.getSignature())
                .addValue("STAGE_DATE", SqlJdbcConverter.convertToSqlTimestamp(confirmStage.getStageDate()))
                .addValue("USER_ID", confirmStage.getUserId())
                .addValue("USER_POS_ID", confirmStage.getUserPosId())
                .addValue("DOC_HASH", confirmStage.getDocHash())
                .addValue("SIGN_INFO", confirmStage.getSignInfo());

        if (confirmStage.getDocument() != null)
            params.addValue("DOCUMENT", new SqlLobValue(new ByteArrayInputStream(confirmStage.getDocument()),
                    confirmStage.getDocument().length, new DefaultLobHandler()), OracleTypes.BLOB);

        Number id = approvalStageInsert.executeAndReturnKey(params);

        confirmStage.setId(id.longValue());
    }

    @Override
    public ConfirmStage getLastConfirmStage(Long confirmId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * \n" +
                "from (select *\n" +
                "        from USCI_RSPDENT.CONFIRM_STAGE\n" +
                "       where CONFIRM_ID = ?\n" +
                "       order by STAGE_DATE desc)\n" +
                " where ROWNUM <= 1", new Object[]{confirmId});

        if (rows.isEmpty())
            return null;

        if (rows.size() != 1)
            throw new UsciException("Ошибка получения записи из таблицы USCI_RSPDENT.CONFIRM_STAGE");

        return getConfirmStageFromJdbcMap(rows.get(0));
    }

    @Override
    public ConfirmStage getConfirmStage(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from USCI_RSPDENT.CONFIRM_STAGE where ID = ?",
                new Object[]{id});

        if (rows.size() != 1)
            throw new UsciException("Ошибка получения записи из таблицы USCI_RSPDENT.CONFIRM_STAGE");

        return getConfirmStageFromJdbcMap(rows.get(0));
    }

    private ConfirmStage getConfirmStageFromJdbcMap(Map<String, Object> row) {
        ConfirmStage confirmStage = new ConfirmStage();
        confirmStage.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
        confirmStage.setStatus(ConfirmStatus.getApprovalStatus(SqlJdbcConverter.convertToLong(row.get("STATUS_ID"))));
        confirmStage.setSignature(SqlJdbcConverter.convertObjectToString(row.get("SIGNATURE")));
        confirmStage.setStageDate(SqlJdbcConverter.convertToLocalDateTime(row.get("STAGE_DATE")));
        confirmStage.setUserId(SqlJdbcConverter.convertToLong(row.get("USER_ID")));
        confirmStage.setUserPosId(SqlJdbcConverter.convertToLong(row.get("USER_POS_ID")));
        confirmStage.setConfirmId(SqlJdbcConverter.convertToLong(row.get("CONFIRM_ID")));
        if (row.get("DOCUMENT") != null)
            confirmStage.setDocument((byte[])row.get("DOCUMENT"));

        return confirmStage;
    }

}
