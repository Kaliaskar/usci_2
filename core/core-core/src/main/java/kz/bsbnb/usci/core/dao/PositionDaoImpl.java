package kz.bsbnb.usci.core.dao;

import kz.bsbnb.usci.model.adm.Position;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * @author Jandos Iskakov
 */

@Repository
public class PositionDaoImpl implements PositionDao {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final SimpleJdbcInsert positionUserInsert;

    public PositionDaoImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate npJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.npJdbcTemplate = npJdbcTemplate;

        this.positionUserInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("USCI_ADM")
                .withTableName("USER_PROD_POS")
                .usingColumns("USER_ID", "PRODUCT_ID", "POS_ID")
                .usingGeneratedKeyColumns("ID");
    }

    @Override
    public List<Position> getUserPosList() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from USCI_ADM.REF_POS");

        List<Position> userPos = new ArrayList<>();
        for (Map<String, Object> row : rows)
            userPos.add(getPosFromJdbcMap(row));

        return userPos;
    }

    @Override
    public List<Position> getUserPosListByProductId(Long userId, Long productId) {
        String query = "select t.* from USCI_ADM.REF_POS t, USCI_ADM.USER_PROD_POS ps \n" +
                "       where ps.user_id = :userId \n" +
                "       and ps.product_id = :productId \n" +
                "       and ps.pos_id = t.id";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("productId", productId);

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);

        if (rows.size() < 1)
            return Collections.emptyList();

        List<Position> userPos = new ArrayList<>();
        for (Map<String, Object> row : rows)
            userPos.add(getPosFromJdbcMap(row));

        return userPos;
    }

    @Override
    public Position getUserPosById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from USCI_ADM.REF_POS where ID = ?", new Object[]{id});
        if (rows.size() != 1)
            throw new UsciException("Ошибка нахождения записи в таблице USCI_ADM.REF_POS");

        return getPosFromJdbcMap(rows.get(0));
    }

    @Override
    public void addUserPosition(Long userId, Long productId, List<Long> positionIds) {
        List<MapSqlParameterSource> params = new ArrayList<>();

        for (Long positionId : positionIds) {
            params.add(new MapSqlParameterSource("USER_ID", userId)
                    .addValue("PRODUCT_ID", productId)
                    .addValue("POS_ID", positionId));
        }

        if (positionIds.size() > 1) {
            int counts[] = positionUserInsert.executeBatch(params.toArray(new SqlParameterSource[0]));
            if (Arrays.stream(counts).anyMatch(value -> value != 1))
                throw new UsciException("Ошибка insert записей в таблицу USCI_ADM.USER_PROD_POS");
        }
        else {
            int count = positionUserInsert.execute(params.get(0));
            if (count != 1)
                throw new UsciException("Ошибка insert записей в таблицу USCI_ADM.USER_PROD_POS");
        }

    }

    @Override
    public void delUserPosition(Long userId, Long productId, List<Long> positionIds) {
        List<MapSqlParameterSource> params = new ArrayList<>();

        for (Long positionId : positionIds) {
            params.add(new MapSqlParameterSource("userId", userId)
                    .addValue("productId", productId)
                    .addValue("positionId", positionId));
        }

        String delete = "delete from USCI_ADM.USER_PROD_POS \n" +
                "  where USER_ID = :userId \n" +
                "    and PRODUCT_ID = :productId \n" +
                "    and POS_ID = :positionId";

        if (positionIds.size() > 1) {
            int counts[] = npJdbcTemplate.batchUpdate(delete, params.toArray(new SqlParameterSource[0]));
            if (Arrays.stream(counts).anyMatch(value -> value != 1))
                throw new UsciException("Ошибка delete записей из таблицы USCI_ADM.USER_PROD_POS");
        } else {
            int count = npJdbcTemplate.update(delete, params.get(0));
            if (count != 1)
                throw new UsciException("Ошибка delete записей из таблицы USCI_ADM.USER_PROD_POS");
        }

    }

    private static Position getPosFromJdbcMap(Map<String, Object> row) {
        Position position = new Position();
        position.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
        position.setNameRu(SqlJdbcConverter.convertObjectToString(row.get("NAME_RU")));
        position.setNameKz(SqlJdbcConverter.convertObjectToString(row.get("NAME_KZ")));
        position.setLevel(SqlJdbcConverter.convertToLong(row.get("LEVEL_")));
        return position;
    }

}
