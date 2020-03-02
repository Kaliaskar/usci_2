package kz.bsbnb.usci.core.test;

import kz.bsbnb.usci.eav.service.ProductService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ProductServiceTest {
    @Autowired
    private ProductService productService;

    @Test
    public void test0() {
        System.out.println(productService.getProducts());
        System.out.println(productService.getProductById(1L));
        System.out.println(productService.findProductByCode("EEE"));
    }

}



