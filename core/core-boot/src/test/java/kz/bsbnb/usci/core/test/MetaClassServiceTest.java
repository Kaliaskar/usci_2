package kz.bsbnb.usci.core.test;

import kz.bsbnb.usci.eav.meta.xsd.XsdGenerator;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.eav.repository.MetaClassRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MetaClassServiceTest {
    @Autowired
    private MetaClassRepository metaClassRepository;

    @Autowired
    private XsdGenerator xsdGenerator;

    @Test
    public void test0() {
        List<MetaClass> metaClasses = metaClassRepository.getMetaClasses();

        try (FileOutputStream fileOutputStream = new FileOutputStream(new File("test.xsd"))) {
            xsdGenerator.generate(fileOutputStream, metaClasses);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}



