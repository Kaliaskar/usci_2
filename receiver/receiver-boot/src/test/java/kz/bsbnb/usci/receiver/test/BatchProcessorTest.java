package kz.bsbnb.usci.receiver.test;

import kz.bsbnb.usci.receiver.model.BatchFile;
import kz.bsbnb.usci.receiver.processor.BatchReceiver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StreamUtils;

import java.io.FileInputStream;
import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BatchProcessorTest {

    @Autowired
    private BatchReceiver batchReceiver;

    @Before
    public void setUp() {

    }

    @Test
    public void test0() throws IOException {
        String fileName = "C:\\bsb\\usci\\batches\\33333333\\5-cred_reg_file_31.03.2018.xml.zip";

        BatchFile batchFile = new BatchFile(10196L, false, fileName);
        batchFile.setFileName("5-cred_reg_file_31.03.2018.xml.zip");
        batchFile.setFileContent(StreamUtils.copyToByteArray(new FileInputStream(fileName)));

        batchReceiver.receiveBatch(batchFile);
    }

}



