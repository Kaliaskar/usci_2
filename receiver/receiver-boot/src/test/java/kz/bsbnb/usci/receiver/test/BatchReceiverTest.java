package kz.bsbnb.usci.receiver.test;

import kz.bsbnb.usci.receiver.model.BatchFile;
import kz.bsbnb.usci.receiver.processor.BatchReceiver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BatchReceiverTest {
    @Autowired
    private BatchReceiver batchReceiver;

    @Before
    public void setUp() {
    }

    @Test
    public void test0() throws IOException {
        String fileName = "C:\\bsb\\usci\\batches\\XML_DATA_BY_CID_2_RD_20170101_26066\\data.zip";

        BatchFile batchFile = new BatchFile();
        batchFile.setUserId(392L);
        batchFile.setNb(false);
        batchFile.setFileName("data.zip");
        batchFile.setFileContent(Files.readAllBytes(new File(fileName).toPath()));

        batchReceiver.receiveBatch(batchFile);
    }

}



