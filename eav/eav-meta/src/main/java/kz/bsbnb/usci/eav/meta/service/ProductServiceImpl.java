package kz.bsbnb.usci.eav.meta.service;

import kz.bsbnb.usci.eav.dao.ProductDao;
import kz.bsbnb.usci.eav.meta.xsd.XsdGenerator;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.eav.model.meta.json.MetaClassJson;
import kz.bsbnb.usci.eav.model.meta.json.ProductJson;
import kz.bsbnb.usci.eav.repository.MetaClassRepository;
import kz.bsbnb.usci.eav.service.ProductService;
import kz.bsbnb.usci.model.adm.Position;
import kz.bsbnb.usci.model.batch.Product;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.json.ApproveIterationJson;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Jandos Iskakov
 * */

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductDao productDao;
    private final XsdGenerator xsdGenerator;
    private final MetaClassRepository metaClassRepository;
    private final JdbcTemplate jdbcTemplate;

    public ProductServiceImpl(ProductDao productDao,
                              XsdGenerator xsdGenerator,
                              MetaClassRepository metaClassRepository,
                              JdbcTemplate jdbcTemplate) {
        this.productDao = productDao;
        this.xsdGenerator = xsdGenerator;
        this.metaClassRepository = metaClassRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Product> getProducts() {
        return productDao.getProducts();
    }

    @Override
    public void generateXsd() {
        List<Product> products = getProducts();

        for (Product product : products) {
            List<MetaClass> metaClasses = new ArrayList<>();

            if (product.getCode().equals("DICT")) {
                // продукт справочников
                metaClasses = metaClassRepository.getMetaClasses().stream()
                        .filter(MetaClass::isDictionary)
                        .collect(Collectors.toList());
            } else {
                // специифичный продукт; выбираем только его мета классы
                List<Long> classesIds = productDao.getProductMetaClassIds(product.getId());
                for (Long classId : classesIds)
                    metaClasses.add(metaClassRepository.getMetaClass(classId));
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                xsdGenerator.generate(baos, metaClasses);

                product.setXsd(baos.toByteArray());
            } catch (IOException e) {
                throw new UsciException(e);
            }

            productDao.updateProduct(product);
        }
    }

    @Override
    public Product getProductById(Long id) {
        return productDao.getProductById(id);
    }

    @Override
    public Optional<Product> findProductByCode(String code) {
        return productDao.findProductByCode(code);
    }

    @Override
    public long createProduct(ProductJson json) {
        Product product = new Product();
        product.setCode(json.getCode());
        product.setName(json.getName());
        product.setCrosscheckPackageName(json.getCrosscheckPackageName());

        return productDao.createProduct(product);
    }

    @Override
    public void updateProduct(ProductJson json) {
        Product product = productDao.getProductById(json.getId());
        product.setCode(json.getCode());
        product.setName(json.getName());
        product.setCrosscheckPackageName(json.getCrosscheckPackageName());

        productDao.updateProduct(product);
    }

    @Override
    public List<MetaClassJson> getMetaClasses(long productId, boolean available) {
        List<MetaClassJson> metaClasses = new ArrayList<>();
        String query = "select ID, NAME, TITLE from EAV_CORE.EAV_M_CLASSES\n";

        if (!available)
            query += "where ID in (select META_CLASS_ID\n" +
                    "from EAV_CORE.EAV_M_PRODUCT_CLASS\n" +
                    "where PRODUCT_ID = ?" +
                    "  and IS_SYNC = 1)";
        else
            query += "where ID not in (select META_CLASS_ID\n" +
                    " from EAV_CORE.EAV_M_PRODUCT_CLASS\n" +
                    " where PRODUCT_ID = ?" +
                    "   and IS_SYNC = 1)";

        query += "order by ID desc";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, productId);

        for (Map<String, Object> row : rows) {
            MetaClassJson json = new MetaClassJson();
            json.setId(SqlJdbcConverter.convertToLong(row.get("id")));
            json.setName((String) row.get("name"));
            json.setTitle(row.get("title") == null? json.getName(): (String) row.get("title"));
            metaClasses.add(json);
        }

        return metaClasses;
    }

    @Override
    public void addProductMetaClass(long productId, List<Long> metaIds) {
         productDao.addProductMetaClass(productId, metaIds);
    }

    @Override
    public void deleteProductMetaClass(long productId, List<Long> metaIds) {
        productDao.deleteProductMetaClass(productId, metaIds);
    }

    @Override
    public boolean isOpenDictionary(Long id) {
        MapSqlParameterSource param = new MapSqlParameterSource();
        param.addValue("CODE", "OPEN_DICT");
        param.addValue("META_CLASS_ID", id);
        Long count = jdbcTemplate.queryForObject("Select count(1) from EAV_CORE.select * from EAV_CORE.EAV_M_PRODUCT_CLASS cl, \n" +
                "              EAV_CORE.EAV_M_PRODUCT p \n" +
                "        where p.ID = cl.PRODUCT_ID\n" +
                "          and p.CODE = :CODE\n" +
                "          and cl.META_CLASS_ID = :META_CLASS_ID;", Long.class, param);

        return count > 0;

    }

    @Override
    public List<Long> getProductIdsByMetaClassId(Long metaClassId) {
        return productDao.getProductIdsByMetaClassId(metaClassId);
    }

    @Override
    public List<Position> getPositions(Long productId, boolean available) {
        return productDao.getPositions(productId, available);
    }

    @Override
    public void addProductPosition(Long productId, List<Long> posIds) {
        productDao.addProductPosition(productId,posIds);
    }

    @Override
    public void deleteProductPosition(Long productId, List<Long> posIds) {
        productDao.deleteProductPosition(productId,posIds);
    }

    @Override
    public List<ApproveIterationJson> getApproveIterations(Long productId) {
        return productDao.getApproveIterations(productId);
    }

    @Override
    public void setApproveIterations(Long productId, List<ApproveIterationJson> approveIterationJsonList) {
        productDao.setApproveIterations(productId, approveIterationJsonList);
    }

    @Override
    public boolean checkForApprove(Long productId, Long receiptDateMillis, Long reportDateMillis) {
        List<LocalDate> holidayDays = productDao.getHolidayDays();
        List<ApproveIterationJson> approveIterationJsonList = productDao.getApproveIterations(productId);
        if (approveIterationJsonList.size() == 0 || approveIterationJsonList == null) {
            return false;
        } else {
            LocalDateTime reportDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(reportDateMillis), ZoneId.systemDefault());
            for (ApproveIterationJson approveIterationJson : approveIterationJsonList){
                reportDate = reportDate.plusMonths(approveIterationJson.getMonth())
                        .plusDays(approveIterationJson.getDay())
                        .plusHours(approveIterationJson.getHour())
                        .plusMinutes(approveIterationJson.getMin());
                if (approveIterationJson.getHour().equals(0L) && approveIterationJson.getMin().equals(0L)) {
                    reportDate = reportDate.plusDays(1);
                }
                if (approveIterationJson.isDayTransfer())
                    while (holidayDays.contains(reportDate.toLocalDate()))
                        reportDate = reportDate.plusDays(1);
            }
            reportDateMillis = reportDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            return receiptDateMillis.compareTo(reportDateMillis) > 0;
        }
    }


}
