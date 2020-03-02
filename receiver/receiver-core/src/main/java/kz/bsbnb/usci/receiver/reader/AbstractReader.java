package kz.bsbnb.usci.receiver.reader;

import kz.bsbnb.usci.eav.client.EntityClient;
import kz.bsbnb.usci.eav.repository.MetaClassRepository;
import kz.bsbnb.usci.eav.service.BaseEntityLoadXmlService;
import kz.bsbnb.usci.model.Constants;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.receiver.batch.service.BatchService;
import kz.bsbnb.usci.receiver.model.Batch;
import kz.bsbnb.usci.receiver.model.BatchStatus;
import kz.bsbnb.usci.receiver.model.BatchStatusType;
import kz.bsbnb.usci.receiver.model.BatchType;
import kz.bsbnb.usci.receiver.validator.XsdValidator;
import kz.bsbnb.usci.sync.client.SyncClient;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Kanat Tulbassiev
 */

public abstract class AbstractReader<T> implements ItemReader<T> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractReader.class);

    private static final long STEP_WAIT_TIMEOUT = 1000;
    static final int ZIP_BUFFER_SIZE = 4096;

    @Value("#{jobParameters['batchId']}")
    protected Long batchId;
    @Value("#{jobParameters['userId']}")
    protected Long userId;
    @Value("#{jobParameters['reportId']}")
    protected Long reportId;
    @Value("#{jobParameters['actualCount']}")
    protected Long actualCount;
    @Value("#{jobParameters['respondentId']}")
    protected Long respondentId;
    @Value("#{jobParameters['maintenance']}")
    protected Long maintenance;

    long totalCount = 0;
    XMLEventReader xmlEventReader;
    BatchService batchService;
    protected Batch batch;
    SyncClient syncClient;
    XsdValidator xsdValidator;
    EntityClient entityClient;
    MetaClassRepository metaClassRepository;
    BaseEntityLoadXmlService baseEntityLoadXmlService;

    @PostConstruct
    protected void init() {
        batch = batchService.getBatch(batchId);

        // извлекаем XML файл из zip файла
        ByteArrayOutputStream out = extractBatch();

        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty("javax.xml.stream.isCoalescing", true);

        logger.info("Проверяем XML файл на правильность схемы XSD (batchId = {})", batchId);

        validateXsdSchema(new ByteArrayInputStream(out.toByteArray()));

        try {
            xmlEventReader = inputFactory.createXMLEventReader(new ByteArrayInputStream(out.toByteArray()));
        } catch (XMLStreamException e) {
            batchService.addBatchStatus(new BatchStatus(batchId, BatchStatusType.ERROR)
                    .setText("Ошибка при проверке XML: " + e.getMessage())
                    .setExceptionTrace(ExceptionUtils.getStackTrace(e)));

            throw new UsciException(e.getMessage());
        }
    }

    /**
     * Делает валидацию батча согласно XSD продукта
     */
    private void validateXsdSchema(InputStream xmlInputStream) {
        InputStream xsdStream = null;

        try {
            if (batch.getBatchType() == BatchType.USCI) {
                // новый формат ессп берет xsd согласно продуктам
                byte[] xsdBytes = batch.getProduct().getXsd();
                if (xsdBytes == null)
                    throw new UsciException(String.format("Ошибка получения XSD файла для XML валидаций batchId = %s", batchId));

                xsdStream = new ByteArrayInputStream(xsdBytes);
            } else if (batch.getBatchType() == BatchType.CREDIT_REGISTRY) {
                // старый формат ессп кредитного регистра
                xsdStream = getClass().getClassLoader().getResourceAsStream("xsd/credit-registry.xsd");
            } else if (batch.getBatchType() == BatchType.USCI_OLD) {
                // старый формат ессп xsd c manifest файлом
                xsdStream = getClass().getClassLoader().getResourceAsStream("xsd/usci.xsd");
            }

            if (xsdStream == null)
                throw new UsciException(String.format("Ошибка получения XSD файла для XML валидаций batchId = %s", batchId));

            xsdValidator.validateSchema(xsdStream, xmlInputStream);
        } catch (Exception e) {
            batchService.addBatchStatus(new BatchStatus(batchId, BatchStatusType.ERROR)
                    .setText("XML не прошёл проверку XSD: " + e.getMessage())
                    .setExceptionTrace(ExceptionUtils.getStackTrace(e)));

            throw new UsciException(e);
        } finally {
            // не забываем обязательно закрывать поток XSD файла
            if (xsdStream != null) {
                try {
                    xsdStream.close();
                } catch (IOException e) {
                    logger.error("Ошибка закрытия потока XSD файла", e);
                }
            }
        }
    }

    @Override
    public abstract T read() throws UnexpectedInputException, ParseException, NonTransientResourceException;

    protected abstract ByteArrayOutputStream extractBatch();

    void waitSync() {
        while (syncClient.getQueueSize() > Constants.MAX_SYNC_QUEUE_SIZE) {
            logger.info("Очередь SYNC заполнена, ожидаем чтобы продолжить парсинг XML файла:"  + syncClient.getQueueSize());

            try {
                Thread.sleep(STEP_WAIT_TIMEOUT);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    void saveTotalCounts() {
        logger.info("Обновление статуса батча id = {}, totalCount = {}", batchId, totalCount);

        batchService.setBatchTotalCount(batchId, totalCount);
    }

}
