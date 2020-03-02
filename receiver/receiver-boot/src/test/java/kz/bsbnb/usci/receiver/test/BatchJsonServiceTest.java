package kz.bsbnb.usci.receiver.test;

import kz.bsbnb.usci.receiver.batch.service.BatchJsonService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BatchJsonServiceTest {
    @Autowired
    private BatchJsonService batchJsonService;

    @Before
    public void setUp() {
    }

    @Test
    public void test0() throws IOException {
        batchJsonService.getBatchList(Arrays.asList(14L, 38L), 16102, true, null,1, 100);
        //batchJsonService.getBatchListToSign(14L, 16102);
        //batchJsonService.getBatchStatusList(421L, 1, 100);
        //batchJsonService.getMaintenanceBatchList(Arrays.asList(14L, 38L), null);
        //batchJsonService.getPendingBatchList(Arrays.asList(14L, 38L));
    }

}



