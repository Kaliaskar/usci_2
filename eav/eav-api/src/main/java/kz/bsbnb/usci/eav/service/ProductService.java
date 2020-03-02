package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.eav.model.meta.json.MetaClassJson;
import kz.bsbnb.usci.eav.model.meta.json.ProductJson;
import kz.bsbnb.usci.model.adm.Position;
import kz.bsbnb.usci.model.batch.Product;
import kz.bsbnb.usci.model.json.ApproveIterationJson;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * @author Jandos Iskakov
 * @author Olzhas Kaliaskar
 * */

public interface ProductService {

    List<Product> getProducts();

    void generateXsd();

    Product getProductById(Long id);

    Optional<Product> findProductByCode(String code);

    long createProduct(ProductJson product);

    void updateProduct(ProductJson json);

    void addProductMetaClass(long productId, List<Long> metaIds);

    void deleteProductMetaClass(long productId, List<Long> metaIds);

    List<MetaClassJson> getMetaClasses(long productId, boolean available);

    boolean isOpenDictionary(Long id);

    List<Long> getProductIdsByMetaClassId(Long metaClassId);

    List<Position> getPositions(Long productId, boolean available);

    void addProductPosition(Long productId, List<Long> posIds);

    void deleteProductPosition(Long productId, List<Long> posIds);

    List<ApproveIterationJson> getApproveIterations(Long productId);

    void setApproveIterations(Long productId, List<ApproveIterationJson> approveIterationJsonList);

    boolean checkForApprove(Long productId, Long receiptDateMillis, Long reportDateMillis) ;

}
