package kz.bsbnb.usci.core.test;

import kz.bsbnb.usci.core.service.ConfirmService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ConfirmServiceTest {
    @Autowired
    private ConfirmService approvalService;

    @Test
    public void test0() {
        //approvalService.approve(16089L, 81L, "HASH","DUMMY SIGNATURE");
        //approvalService.approve(16447L, 81L, "HASH","DUMMY SIGNATURE");
        //approvalService.approve(16916L, 81L, "HASH","DUMMY SIGNATURE");
    }

    @Test
    public void test1() {
        //approvalService.createConfirmDocument(81L, 10196);
    }

    @Test
    public void test3() {
        approvalService.getConfirmJsonList(392, false, Arrays.asList(38L), null, 1, 100);
    }

    @Test
    public void test2() {
        /*ConfirmMessage confirmMessage = new ConfirmMessage();
        confirmMessage.setConfirmId(101L);
        confirmMessage.setSendDate(LocalDateTime.now());
        confirmMessage.setText("algo");
        confirmMessage.setUserId(10196L);
        approvalService.addConfirmMessage(confirmMessage);*/
    }

}



