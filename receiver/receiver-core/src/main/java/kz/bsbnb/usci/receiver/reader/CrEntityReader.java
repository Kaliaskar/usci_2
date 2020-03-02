package kz.bsbnb.usci.receiver.reader;

import kz.bsbnb.usci.eav.client.EntityClient;
import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.core.BaseEntityStatus;
import kz.bsbnb.usci.eav.model.core.EntityStatusType;
import kz.bsbnb.usci.eav.repository.MetaClassRepository;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.receiver.batch.service.BatchService;
import kz.bsbnb.usci.receiver.model.BatchStatus;
import kz.bsbnb.usci.receiver.model.BatchStatusType;
import kz.bsbnb.usci.receiver.parser.impl.MainParser;
import kz.bsbnb.usci.receiver.validator.XsdValidator;
import kz.bsbnb.usci.sync.client.SyncClient;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLInputFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author Kanat Tulbassiev
 */

@Component
@StepScope
public class CrEntityReader<T> extends AbstractReader<T> {
    private static final Logger logger = LoggerFactory.getLogger(EavEntityReader.class);

    private final MainParser crParser;

    @Autowired
    public CrEntityReader(BatchService batchService,
                          SyncClient syncClient,
                          XsdValidator xsdValidator,
                          MetaClassRepository metaClassRepository,
                          EntityClient entityClient,
                          MainParser crParser) {
        this.batchService = batchService;
        this.syncClient = syncClient;
        this.metaClassRepository = metaClassRepository;
        this.xsdValidator = xsdValidator;
        this.entityClient = entityClient;
        this.crParser = crParser;
    }

    @Override
    protected void init() {
        super.init();

        try {
            crParser.parse(xmlEventReader, batch, 0L, respondentId);
        } catch (SAXException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    protected ByteArrayOutputStream extractBatch() {
        ZipArchiveInputStream zais = new ZipArchiveInputStream(new ByteArrayInputStream(batch.getContent()));
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty("javax.xml.stream.isCoalescing", true);

        ByteArrayOutputStream out;

        try {
            zais.getNextZipEntry();

            int len;
            out = new ByteArrayOutputStream(ZIP_BUFFER_SIZE);

            byte[] buffer = new byte[ZIP_BUFFER_SIZE];
            while ((len = zais.read(buffer, 0, ZIP_BUFFER_SIZE)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch (IOException e) {
            logger.error("Ошибка извлечения xml файла из zip батча(id = {})", batchId);

            batchService.addBatchStatus(new BatchStatus(batchId, BatchStatusType.ERROR)
                    .setText(e.getMessage()));

            throw new UsciException(e);
        }

        return out;
    }

    @Override
    public T read() throws UnexpectedInputException, ParseException, NonTransientResourceException {
        T entity = (T) crParser.getCurrentBaseEntity();

        long index = crParser.getIndex();

        waitSync();

        if (crParser.hasMore()) {
            try {
                crParser.parse(xmlEventReader, batch, index, respondentId);
            } catch (SAXException e) {
                entityClient.addEntityStatus(new BaseEntityStatus(batchId, EntityStatusType.ERROR)
                        .setEntityText("Ошибка парсинга xml")
                        .setIndex(index));

                logger.error(e.getMessage(), e);

                return null;
            }

            ((BaseEntity) entity).setBatchId(batchId);
            ((BaseEntity) entity).setBatchIndex(index);

            return entity;
        }

        totalCount = (long) crParser.getPackageCount();

        saveTotalCounts();

        return null;
    }

}
