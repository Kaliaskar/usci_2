package kz.bsbnb.usci.eav.dao;

import kz.bsbnb.usci.model.adm.Position;
import kz.bsbnb.usci.model.batch.Product;
import kz.bsbnb.usci.model.json.ApproveIterationJson;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @author Jandos Iskakov
 * @author Olzhas Kaliaskar
 */

public interface ProductDao {

    List<Product> getProducts();

    Product getProductById(Long id);

    Optional<Product> findProductByCode(String code);

    List<Long> getProductMetaClassIds(Long productId);

    long createProduct(Product product);

    void updateProduct(Product product);

    void addProductMetaClass(Long productId, List<Long> metaIds);

    void deleteProductMetaClass(Long productId, List<Long> metaIds);

    List<Long> getProductIdsByMetaClassId(Long metaClassId);

    List<Position> getPositions(Long productId, boolean available);

    void addProductPosition(Long productId, List<Long> posIds);

    void deleteProductPosition(Long productId, List<Long> posIds);

    List<ApproveIterationJson> getApproveIterations(Long productId);

    void setApproveIterations(Long productId, List<ApproveIterationJson> approveIterationJsonList);

    List<LocalDate> getHolidayDays();

}
