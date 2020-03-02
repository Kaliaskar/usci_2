package kz.bsbnb.usci.receiver.reader;

import kz.bsbnb.usci.eav.client.EntityClient;
import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseEntityJson;
import kz.bsbnb.usci.eav.model.base.OperType;
import kz.bsbnb.usci.eav.model.core.BaseEntityStatus;
import kz.bsbnb.usci.eav.model.core.EntityStatusType;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.eav.model.meta.json.EntityExtJsTreeJson;
import kz.bsbnb.usci.eav.repository.MetaClassRepository;
import kz.bsbnb.usci.eav.service.BaseEntityLoadXmlService;
import kz.bsbnb.usci.receiver.batch.service.BatchService;
import kz.bsbnb.usci.receiver.model.exception.ReceiverException;
import kz.bsbnb.usci.receiver.validator.XsdValidator;
import kz.bsbnb.usci.sync.client.SyncClient;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.util.*;

@Component
@StepScope
public class MaintenanceEntityReader<T> extends  AbstractReader<T>{
    private static final Logger logger = LoggerFactory.getLogger(MaintenanceEntityReader.class);
    private  List<BaseEntityJson> baseEntityJsonList = new ArrayList<>();
    private Iterator<BaseEntityJson> iterator;

    @Autowired
    public MaintenanceEntityReader(BatchService batchService,
                                   SyncClient syncClient,
                                   MetaClassRepository metaClassRepository,
                                   XsdValidator xsdValidator,
                                   EntityClient entityClient,
                                   BaseEntityLoadXmlService baseEntityLoadXmlService) {
                this.batchService = batchService;
                this.syncClient = syncClient;
                this.metaClassRepository = metaClassRepository;
                this.xsdValidator = xsdValidator;
                this.entityClient = entityClient;
                this.baseEntityLoadXmlService = baseEntityLoadXmlService;
            }

    @PostConstruct
    @Override
    protected void init() {
        baseEntityJsonList = baseEntityLoadXmlService.loadEntityForApproval(batchId);
        if (baseEntityJsonList.size() == 0L) {
            logger.error(String.format("Нет сущности в таблице EAV_ENTITY_MAINTENANCE с batch_id = %d", batchId));
            batchService.endBatch(batchId);
            iterator = null;
        } else
        iterator = baseEntityJsonList.iterator();

    }

    @Override
    public ByteArrayOutputStream extractBatch() {
        return null;
    }

    @Override
    public T read() throws UnexpectedInputException, ParseException, NonTransientResourceException {
        try {
            if (iterator != null) {
                return readInner();
            } else
                return null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            entityClient.addEntityStatus(new BaseEntityStatus(batchId, EntityStatusType.ERROR)
                    .setErrorMessage(e.getLocalizedMessage()));
            return null;
        }
    }

    private T readInner() {
        waitSync();
        while (iterator.hasNext()) {
            BaseEntityJson baseEntityJson = iterator.next();
            MetaClass metaClass = metaClassRepository.getMetaClass(baseEntityJson.getMetaClassId());
            EntityExtJsTreeJson entityJson = baseEntityLoadXmlService.loadBaseEntity(baseEntityJson.getId());
            BaseEntity baseEntity = baseEntityLoadXmlService.getBaseEntityFromJsonTree(respondentId, baseEntityJson.getReportDate(), entityJson, metaClass, batchId);
            baseEntity.setOperation(baseEntityJson.getOperType());
            totalCount++;
            return (T)  baseEntity;
        }
        //saveTotalCounts();
        return null;
    }


}
