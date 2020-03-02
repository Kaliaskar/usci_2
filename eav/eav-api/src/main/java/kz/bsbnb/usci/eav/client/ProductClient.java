package kz.bsbnb.usci.eav.client;

import kz.bsbnb.usci.model.batch.Product;
import kz.bsbnb.usci.model.json.ApproveIterationJson;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author Jandos Iskakov
 */

@FeignClient(name = "core")
public interface ProductClient {

    @GetMapping(value = "/product/getProducts")
    List<Product> getProducts();

    @GetMapping(value = "/product/findProductByCode")
    Product findProductByCode(@RequestParam(name = "code") String code);

    @GetMapping(value = "/product/getProductById")
    Product getProductById(@RequestParam(name = "id") Long id);

    @GetMapping(value = "/product/getProductIdsByMetaClassId")
    List<Long> getProductIdsByMetaClassId(@RequestParam(name = "metaClassId") Long metaClassId);

    @GetMapping(value = "/product/checkForApprove")
    boolean checkForApprove(@RequestParam(name = "productId") Long productId,
                            @RequestParam(name = "receiptDateMillis") Long receiptDateMillis,
                            @RequestParam(name = "reportDateMillis") Long reportDateMillis);

    @GetMapping(value = "/product/getApproveIterations")
    List<ApproveIterationJson> getApproveIterations(@RequestParam(name = "productId") Long productId);

}
