package kz.bsbnb.usci.receiver.test;

import kz.bsbnb.usci.receiver.batch.service.BatchEntryService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BatchEntryTest {
    @Autowired
    private BatchEntryService batchEntryService;

    @Before
    public void setUp() {
    }

    @Test
    public void test0() {
        batchEntryService.confirmBatchEntries(10196L, true);
    }

}



