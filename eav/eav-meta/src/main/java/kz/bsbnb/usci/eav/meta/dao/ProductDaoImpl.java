package kz.bsbnb.usci.eav.meta.dao;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import kz.bsbnb.usci.eav.dao.ProductDao;
import kz.bsbnb.usci.model.adm.Position;
import kz.bsbnb.usci.model.batch.Product;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.json.ApproveIterationJson;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * @author Jandos Iskakov
 * @author Olzhas Kaliaskar
 */

@Repository
public class ProductDaoImpl implements ProductDao {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final SimpleJdbcInsert productMetaInsert;
    private final SimpleJdbcInsert productInsert;
    private static final Gson gson = new Gson();

    public ProductDaoImpl(JdbcTemplate jdbcTemplate,
                          NamedParameterJdbcTemplate npJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.npJdbcTemplate = npJdbcTemplate;

        this.productInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("EAV_CORE")
                .withTableName("EAV_M_PRODUCT")
                .usingGeneratedKeyColumns("ID");

        this.productMetaInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("EAV_CORE")
                .withTableName("EAV_M_PRODUCT_CLASS")
                .usingColumns("PRODUCT_ID", "META_CLASS_ID")
                .usingGeneratedKeyColumns("ID");
    }

    @Override
    public List<Product> getProducts() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from EAV_CORE.EAV_M_PRODUCT");

        return getProductFromJdbcMap(rows);
    }

    @Override
    public Product getProductById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from EAV_CORE.EAV_M_PRODUCT where ID = ?", id);

        if (rows.size() != 1)
            throw new UsciException("Ошибка нахождения записи в таблице EAV_CORE.EAV_M_PRODUCT");

        return getProductFromJdbcMap(rows).get(0);
    }

    @Override
    public Optional<Product> findProductByCode(String code) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select *\n" +
                "from EAV_CORE.EAV_M_PRODUCT\n" +
                "where CODE = ?\n", code);

        if (rows.size() == 0)
            return Optional.empty();

        if (rows.size() > 1)
            throw new UsciException("Ошибка нахождения записи в таблице EAV_CORE.EAV_M_PRODUCT");

        Product product = getProductFromJdbcMap(rows).get(0);

        return Optional.of(product);
    }

    @Override
    public List<Long> getProductMetaClassIds(Long productId) {
        return jdbcTemplate.queryForList("select META_CLASS_ID\n" +
                "from EAV_CORE.EAV_M_PRODUCT_CLASS\n" +
                "where PRODUCT_ID = ?", new Object[]{productId}, Long.class);
    }

    @Override
    public long createProduct(Product product) {
        Number id = this.productInsert.executeAndReturnKey(new MapSqlParameterSource()
                .addValue("CODE", product.getCode())
                .addValue("NAME", product.getName())
                .addValue("XSD", product.getXsd() != null? new String(product.getXsd()): null)
                .addValue("CONFIRM_WITH_APRV", SqlJdbcConverter.convertBooleanToByte(product.isConfirmWithApproval()))
                .addValue("CONFIRM_WITH_SIGN", SqlJdbcConverter.convertBooleanToByte(product.isConfirmWithSignature()))
                .addValue("CONFIRM_POS_IDS", product.getConfirmPositionIds())
                .addValue("CROSSCHECK_PACKAGENAME", product.getCrosscheckPackageName()));

        product.setId(id.longValue());

        return id.longValue();
    }

    @Override
    public void updateProduct(Product product) {
        int count = npJdbcTemplate.update("update EAV_CORE.EAV_M_PRODUCT\n" +
                "set NAME = :NAME,\n" +
                "    XSD = :XSD,\n" +
                "    CROSSCHECK_PACKAGENAME = :CROSSCHECK_PACKAGENAME,\n" +
                "    CONFIRM_WITH_APRV = :CONFIRM_WITH_APRV,\n" +
                "    CONFIRM_WITH_SIGN = :CONFIRM_WITH_SIGN,\n" +
                "    CONFIRM_POS_IDS = :CONFIRM_POS_IDS\n" +
                " where ID = :ID", new MapSqlParameterSource("ID", product.getId())
        .addValue("NAME", product.getName())
        .addValue("XSD", product.getXsd() != null? new String(product.getXsd()): null)
        .addValue("CROSSCHECK_PACKAGENAME", product.getCrosscheckPackageName())
        .addValue("CONFIRM_WITH_APRV", SqlJdbcConverter.convertBooleanToByte(product.isConfirmWithApproval()))
        .addValue("CONFIRM_WITH_SIGN", SqlJdbcConverter.convertBooleanToByte(product.isConfirmWithSignature()))
        .addValue("CONFIRM_POS_IDS", product.getConfirmPositionIds().isEmpty()? null: gson.toJson(product.getConfirmPositionIds())));

        if (count != 1)
            throw new UsciException("Ошибка update записи в таблице EAV_CORE.EAV_M_PRODUCT");
    }

    @Override
    public void addProductMetaClass(Long productId, List<Long> metaIds) {
        List<MapSqlParameterSource> params = new ArrayList<>();

        for (Long metaId : metaIds)
            params.add(new MapSqlParameterSource("META_CLASS_ID", metaId)
                    .addValue("PRODUCT_ID", productId));

        if (metaIds.size() > 1) {
            int counts[] = productMetaInsert.executeBatch(params.toArray(new SqlParameterSource[0]));
            if (Arrays.stream(counts).anyMatch(value -> value != 1))
                throw new UsciException("Ошибка insert записей в таблицу EAV_COR.EAV_M_PRODUCT_CLASS");
        } else {
            int count = productMetaInsert.execute(params.get(0));
            if (count != 1)
                throw new UsciException("Ошибка insert записей в таблицу EAV_COR.EAV_M_PRODUCT_CLASS");
        }
    }

    @Override
    public void deleteProductMetaClass(Long productId, List<Long> metaIds) {
        List<MapSqlParameterSource> params = new ArrayList<>();

        for (Long metaId : metaIds) {
            params.add(new MapSqlParameterSource("META_ID", metaId)
                    .addValue("PRODUCT_ID", productId));
        }

        String delete = "delete from EAV_CORE.EAV_M_PRODUCT_CLASS\n" +
                "  where META_CLASS_ID = :META_ID\n" +
                "    and PRODUCT_ID = :PRODUCT_ID";

        if (metaIds.size() > 1) {
            int counts[] = npJdbcTemplate.batchUpdate(delete, params.toArray(new SqlParameterSource[0]));
            if (Arrays.stream(counts).anyMatch(value -> value != 1))
                throw new UsciException("Ошибка delete записей из таблицы EAV_CORE.EAV_M_PRODUCT_CLASS");
        } else {
            int count = npJdbcTemplate.update(delete, params.get(0));
            if (count != 1)
                throw new UsciException("Ошибка delete записей из таблицы EAV_CORE.EAV_M_PRODUCT_CLASS");
        }
    }

    @Override
    public List<Long> getProductIdsByMetaClassId(Long metaClassId) {
        List<Long> products = jdbcTemplate.queryForList("select PRODUCT_ID\n" +
                "from EAV_CORE.EAV_M_PRODUCT_CLASS\n" +
                "where META_CLASS_ID = ?", new Object[] {metaClassId}, Long.class);

        if (products.size() != 1)
            throw new UsciException("Ошибка определения принадлежности класса к продукту");

        return products;
    }

    @Override
    public List<Position> getPositions(Long productId, boolean available) {
        List<Position> positionList = new ArrayList<>();

        ArrayList<Long> posIds = new ArrayList<>();
        String confirmPositionIds = jdbcTemplate.queryForObject("select CONFIRM_POS_IDS from EAV_CORE.EAV_M_PRODUCT where ID = ?", new Object[]{productId}, String.class);

        String query = "select ID, NAME_RU from USCI_ADM.REF_POS\n";
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (confirmPositionIds != null) {
            Type type = new TypeToken<ArrayList<Long>>(){}.getType();
            posIds = gson.fromJson(confirmPositionIds, type);

            if (!available)
                query += "where ID in (:POSITION_IDS) \n";
            else
                query += "where ID not in (:POSITION_IDS) \n";

            params.addValue("POSITION_IDS", posIds);
        } else {
            if (!available)
                return Collections.emptyList();
        }
        query += "order by ID desc";

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);

        for (Map<String, Object> row : rows) {
            Position position = new Position();
            position.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            position.setNameRu(SqlJdbcConverter.convertObjectToString(row.get("NAME_RU")));

            positionList.add(position);
        }

        return positionList;
    }

    @Override
    public void addProductPosition(Long productId, List<Long> posIds) {
        String confirmPositionIds = jdbcTemplate.queryForObject("select CONFIRM_POS_IDS from EAV_CORE.EAV_M_PRODUCT where ID = ?", new Object[]{productId}, String.class);
        Set<Long> positionIds = new HashSet<>();
        if (confirmPositionIds != null) {
            Type type = new TypeToken<Set<Long>>(){}.getType();
            positionIds = gson.fromJson(confirmPositionIds, type);
            positionIds.addAll(posIds);
        } else {
            positionIds.addAll(posIds);
        }

        int count = npJdbcTemplate.update("update EAV_CORE.EAV_M_PRODUCT\n" +
                        "   set CONFIRM_POS_IDS = :CONFIRM_POS_IDS\n" +
                        " where ID = :ID",
                new MapSqlParameterSource("ID", productId)
                        .addValue("CONFIRM_POS_IDS", gson.toJson(positionIds)));

        if (count != 1)
            throw new UsciException("Ошибка update записи в таблице EAV_CORE.EAV_M_PRODUCT");

    }

    @Override
    public void deleteProductPosition(Long productId, List<Long> posIds) {
        String confirmPositionIds = jdbcTemplate.queryForObject("select CONFIRM_POS_IDS from EAV_CORE.EAV_M_PRODUCT where ID = ?", new Object[]{productId}, String.class);
        Set<Long> positionIds = new HashSet<>();
        Type type = new TypeToken<Set<Long>>(){}.getType();
        positionIds = gson.fromJson(confirmPositionIds, type);
        positionIds.removeAll(posIds);

        int count = npJdbcTemplate.update("update EAV_CORE.EAV_M_PRODUCT\n" +
                        "   set CONFIRM_POS_IDS = :CONFIRM_POS_IDS\n" +
                        " where ID = :ID",
                new MapSqlParameterSource("ID", productId)
                        .addValue("CONFIRM_POS_IDS", positionIds.isEmpty()? null: gson.toJson(positionIds)));

        if (count != 1)
            throw new UsciException("Ошибка update записи в таблице EAV_CORE.EAV_M_PRODUCT");

    }

    @Override
    public List<ApproveIterationJson> getApproveIterations(Long productId) {
        String approveIterations = jdbcTemplate.queryForObject("select APPROVE_ITERATIONS from EAV_CORE.EAV_M_PRODUCT where ID = ?", new Object[]{productId}, String.class);

        if (approveIterations != null) {
            Type type = new TypeToken<ArrayList<ApproveIterationJson>>(){}.getType();
            List<ApproveIterationJson> approveIterationJsonList = gson.fromJson(approveIterations, type);
            approveIterationJsonList.sort(Comparator.comparing(o -> o.getIterationNumber()));
            return approveIterationJsonList;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void setApproveIterations(Long productId, List<ApproveIterationJson> approveIterationJsonList) {
        int count = npJdbcTemplate.update("update EAV_CORE.EAV_M_PRODUCT\n" +
                        "   set APPROVE_ITERATIONS = :APPROVE_ITERATIONS\n" +
                        " where ID = :ID",
                new MapSqlParameterSource("ID", productId)
                        .addValue("APPROVE_ITERATIONS", approveIterationJsonList.isEmpty()? null: gson.toJson(approveIterationJsonList)));

        if (count != 1)
            throw new UsciException("Ошибка update записи в таблице EAV_CORE.EAV_M_PRODUCT");
    }

    @Override
    public List<LocalDate> getHolidayDays() {
        String query = "select HOLIDAY_DATE from USCI_WS.NSI_HOLIDAY order by HOLIDAY_DATE asc";
        List<LocalDate> rows = jdbcTemplate.queryForList(query, LocalDate.class);
        return rows;
    }

    public static List<Product> getProductFromJdbcMap(List<Map<String, Object>> rows) {
        List<Product> list = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            Product product = new Product(SqlJdbcConverter.convertToLong(row.get("ID")));
            product.setCode(SqlJdbcConverter.convertObjectToString(row.get("CODE")));
            product.setName(SqlJdbcConverter.convertObjectToString(row.get("NAME")));
            product.setCrosscheckPackageName(SqlJdbcConverter.convertObjectToString(row.get("CROSSCHECK_PACKAGENAME")));
            product.setConfirmWithApproval(SqlJdbcConverter.convertToBoolean(row.get("CONFIRM_WITH_APRV")));
            product.setConfirmWithSignature(SqlJdbcConverter.convertToBoolean(row.get("CONFIRM_WITH_SIGN")));

            String confirmPositionIds = SqlJdbcConverter.convertObjectToString(row.get("CONFIRM_POS_IDS"));
            if (confirmPositionIds != null) {
                Type type = new TypeToken<Set<Long>>(){}.getType();
                product.setConfirmPositionIds(gson.fromJson(confirmPositionIds, type));
            }

            String xsd = SqlJdbcConverter.convertObjectToString(row.get("XSD"));

            if (xsd != null) {
                product.setXsd(xsd.getBytes());
            }

            list.add(product);
        }

        return list;
    }

}
