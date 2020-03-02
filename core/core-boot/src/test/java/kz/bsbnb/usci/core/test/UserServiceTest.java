package kz.bsbnb.usci.core.test;

import kz.bsbnb.usci.core.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserServiceTest {
    @Autowired
    private UserService userService;

    @Test
    public void test0() {
        userService.addUserProduct(11L, Arrays.asList(1L, 2L));
        //userService.deleteUserProduct(11L, Arrays.asList(1L));

        System.out.println(userService.getUserProductList(11L));
    }

}



