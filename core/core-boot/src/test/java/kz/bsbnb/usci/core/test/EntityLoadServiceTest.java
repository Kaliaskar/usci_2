package kz.bsbnb.usci.core.test;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseSet;
import kz.bsbnb.usci.eav.model.base.BaseValue;
import kz.bsbnb.usci.eav.model.meta.*;
import kz.bsbnb.usci.eav.model.meta.json.EntityExtJsTreeJson;
import kz.bsbnb.usci.eav.repository.MetaClassRepository;
import kz.bsbnb.usci.eav.service.BaseEntityLoadXmlService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.LocalDate;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EntityLoadServiceTest {
//    @Autowired
//    private EntityExtJsService entityExtJsService;

    @Autowired
    private MetaClassRepository metaClassRepository;
    @Autowired
    private BaseEntityLoadXmlService baseEntityLoadXmlService;

    @Test
    public void test0() {
        MetaClass metaClass = metaClassRepository.getMetaClass("ref_country");

        //entityExtJsService.exportDictionaryToMsExcel(metaClass, LocalDate.of(2019, 1, 1));
    }

    @Test
    public void test1() throws FileNotFoundException {
        MetaClass metaClass = metaClassRepository.getMetaClass("credit");

        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader("C:/Work/tmp/entityJSON1.json"));

        EntityExtJsTreeJson node = gson.fromJson(reader,EntityExtJsTreeJson.class);

        BaseEntity baseEntity = baseEntityLoadXmlService.getBaseEntityFromJsonTree(8L, LocalDate.of(2019,9,1), node ,metaClass, 55255L);

        System.out.println(baseEntity.toString());


    }

}



