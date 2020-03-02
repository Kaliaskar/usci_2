package kz.bsbnb.usci.receiver.reader;

import kz.bsbnb.usci.eav.client.EntityClient;
import kz.bsbnb.usci.eav.model.base.*;
import kz.bsbnb.usci.eav.model.core.BaseEntityStatus;
import kz.bsbnb.usci.eav.model.core.EntityStatusType;
import kz.bsbnb.usci.eav.model.meta.*;
import kz.bsbnb.usci.eav.repository.MetaClassRepository;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.receiver.batch.service.BatchService;
import kz.bsbnb.usci.receiver.model.BatchStatus;
import kz.bsbnb.usci.receiver.model.BatchStatusType;
import kz.bsbnb.usci.receiver.validator.XsdValidator;
import kz.bsbnb.usci.sync.client.SyncClient;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Kanat Tulbassiev
 */

@Component
@StepScope
public class EavEntityReader<T> extends AbstractReader<T> {
    private static final Logger logger = LoggerFactory.getLogger(EavEntityReader.class);

    private final Deque<BaseContainer> stack = new ArrayDeque<>();
    private final Deque<Boolean> flagsStack = new ArrayDeque<>();

    private BaseContainer currentContainer;
    private Long index = 0L, level = 0L;
    private boolean hasMembers = false;
    private boolean rootEntityExpected = false;
    private String currentRootMeta = null;

    @Autowired
    public EavEntityReader(BatchService batchService,
                           SyncClient syncClient,
                           MetaClassRepository metaClassRepository,
                           XsdValidator xsdValidator,
                           EntityClient entityClient) {
        this.batchService = batchService;
        this.syncClient = syncClient;
        this.metaClassRepository = metaClassRepository;
        this.xsdValidator = xsdValidator;
        this.entityClient = entityClient;
    }

    @PostConstruct
    @Override
    protected void init() {
        super.init();
    }

    @Override
    public ByteArrayOutputStream extractBatch() {
        ByteArrayOutputStream out = null;

        ByteArrayInputStream inputStream = new ByteArrayInputStream(batch.getContent());

        try (ZipInputStream zis = new ZipInputStream(inputStream, Charset.forName("CP866"))) {
            ZipEntry entry;

            int entryCount = 0;
            // перебираем файлы так как в zip два файла manifest и сам файл с данными
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("manifest.xml") || entry.getName().equals("usci_manifest.xml")) {
                    zis.closeEntry();
                    continue;
                }

                int len;

                byte[] buffer = new byte[ZIP_BUFFER_SIZE];
                out = new ByteArrayOutputStream(ZIP_BUFFER_SIZE);
                while ((len = zis.read(buffer, 0, ZIP_BUFFER_SIZE)) > 0) {
                    out.write(buffer, 0, len);
                }

                zis.closeEntry();
                entryCount++;

                break;
            }

            if (entryCount != 1) {
                batchService.addBatchStatus(new BatchStatus()
                        .setBatchId(batchId)
                        .setStatus(BatchStatusType.ERROR)
                        .setText("Не полный zip файл: нет манифест файла или нет файла с данными")
                        .setExceptionTrace("Не полный zip файл: нет манифест файла или нет файла с данными")
                        .setReceiptDate(LocalDateTime.now()));

                throw new UsciException("Не полный zip файл: нет манифест файла или нет файла с данными");
            }
        } catch (IOException e) {
            logger.error("Ошибка парсинга xml в EavEntityReader batchId = {}", batch.getId());

            batchService.addBatchStatus(new BatchStatus()
                    .setBatchId(batchId)
                    .setStatus(BatchStatusType.ERROR)
                    .setText(e.getMessage())
                    .setExceptionTrace(ExceptionUtils.getStackTrace(e))
                    .setReceiptDate(LocalDateTime.now()));

            throw new UsciException(e);
        }

        return out;
    }

    private HashMap<String, MetaClass> metaCache = new HashMap<>();

    private MetaClass getMeta(String metaName) {
        if(!metaCache.containsKey(metaName)) {
            MetaClass meta = metaClassRepository.getMetaClass(metaName);
            metaCache.put(metaName, meta);
        }

        return metaCache.get(metaName);
    }

    private void startElement(StartElement startElement, String localName) {
        if (localName.equals("batch")) {
            //
        } else if (localName.equals("entities")) {
            rootEntityExpected = true;
        } else if (rootEntityExpected) {
            currentRootMeta = localName;
            rootEntityExpected = false;

            MetaClass metaClass = getMeta(localName);
            BaseEntity baseEntity = new BaseEntity(metaClass, respondentId, batch.getReportDate(), batchId, batch.getReceiptDate());

            if (hasOperation(startElement, OperType.DELETE))
                baseEntity.setOperation(OperType.DELETE);

            if (hasOperation(startElement, OperType.OPEN))
                baseEntity.setOperation(OperType.OPEN);

            if (hasOperation(startElement, OperType.CLOSE))
                baseEntity.setOperation(OperType.CLOSE);

            if (hasOperation(startElement, OperType.INSERT))
                baseEntity.setOperation(OperType.INSERT);

            currentContainer = baseEntity;
        } else {
            MetaType metaType = (currentContainer instanceof BaseEntity)? ((BaseEntity) currentContainer).getAttributeType(localName): currentContainer.getMetaType();

            if (metaType.isSet()) {
                hasMembers = false;
                stack.push(currentContainer);
                flagsStack.push(hasMembers);

                currentContainer = new BaseSet(((MetaSet) metaType).getMetaType());
                level++;
            } else if (metaType.isComplex()) {
                stack.push(currentContainer);
                currentContainer = new BaseEntity((MetaClass) metaType, respondentId, batch.getReportDate(), batchId);

                flagsStack.push(hasMembers);
                level++;

                hasMembers = false;
            } else {
                Object obj = null;
                MetaValue metaValue = (MetaValue) metaType;

                try {
                    XMLEvent event = (XMLEvent) xmlEventReader.next();
                    obj = MetaDataType.getCastObject(metaValue.getMetaDataType(), event.asCharacters().getData().trim());
                } catch (NumberFormatException n) {
                    logger.error(n.getMessage());
                    throw new UsciException("Ошибка преобразования класса");
                } catch (DateTimeParseException d) {
                    logger.error(d.getMessage());
                    throw new UsciException("Ошибка преобразования класса: Ошибка парсинга даты");
                } catch (ClassCastException ex) {
                    level--;
                }

                if (obj != null)
                    hasMembers = true;

                BaseValue baseValue = new BaseValue(obj);

                if (hasOperation(startElement, OperType.NEW)) {
                    BaseValue newBaseValue = new BaseValue(MetaDataType.getCastObject(metaValue.getMetaDataType(),
                            startElement.getAttributeByName(new QName("data")).getValue()));
                    baseValue.setNewBaseValue(newBaseValue);
                    if (!currentContainer.isSet())
                        newBaseValue.setMetaAttribute((((BaseEntity) currentContainer)).getMetaAttribute(localName));
                }

                if (currentContainer.isSet())
                    ((BaseSet) currentContainer).put(baseValue);
                else
                    ((BaseEntity) currentContainer).put(localName, baseValue);

                level++;
            }
        }
    }

    @Override
    public T read() throws UnexpectedInputException, ParseException, NonTransientResourceException {
        try {
            return readInner();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            entityClient.addEntityStatus(new BaseEntityStatus(batchId, EntityStatusType.ERROR)
                    .setErrorMessage(e.getLocalizedMessage()));

            return null;
        }
    }

    private T readInner() {
        waitSync();

        while (xmlEventReader.hasNext()) {
            XMLEvent event = (XMLEvent) xmlEventReader.next();

            if (event.isStartDocument()) {
                logger.debug("start document");
            } else if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                String localName = startElement.getName().getLocalPart();

                startElement(startElement, localName);
            } else if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                String localName = endElement.getName().getLocalPart();

                if (endElement(localName)) {
                    if (currentContainer == null) {
                        break;
                    } else {
                        totalCount++;

                        if (currentContainer instanceof BaseEntity)
                            ((BaseEntity) currentContainer).setBatchIndex(index);
                            ((BaseEntity) currentContainer).setMaintenance(maintenance == 1);

                        return (T) currentContainer;
                    }
                }
            } else if (event.isEndDocument()) {
                logger.debug("end document");
            } else {
                logger.debug(event.toString());
            }
        }

        saveTotalCounts();

        return null;
    }

    private boolean endElement(String localName) {
        if (localName.equals("batch")) {
            logger.debug("batch");
        } else if (localName.equals("entities")) {
            logger.debug("entities");
            currentContainer = null;
            return true;
        } else if (localName.equals(currentRootMeta)) {
            rootEntityExpected = true;
            index++;
            return true;
        } else {
            MetaType metaType;
            MetaAttribute metaAttribute;
            BaseContainer tempContainer = level == stack.size()? stack.peek(): currentContainer;

            if (tempContainer instanceof BaseEntity)
                metaType = ((BaseEntity) tempContainer).getAttributeType(localName);
            else
                metaType = tempContainer.getMetaType();

            if (metaType.isComplex() || metaType.isSet()) {
                Object obj = currentContainer;
                currentContainer = stack.pop();

                if (currentContainer instanceof BaseEntity)
                    metaAttribute = ((BaseEntity) currentContainer).getMetaAttribute(localName);
                else
                    metaAttribute = null;

                if (currentContainer.isSet()) {
                    if (hasMembers) {
                        ((BaseSet) currentContainer).put(new BaseValue(obj));
                        flagsStack.pop();
                        hasMembers = true;
                    } else {
                        hasMembers = flagsStack.pop();
                    }
                } else {
                    // временный костыль для филиалов без документа
                    if (localName.equals("ref_creditor_branch") &&
                            ((((BaseEntity) obj).getBaseValue("docs") == null) ||
                                    ((BaseEntity) obj).getBaseValue("docs").getValue() == null)) obj = null;



                    if (hasMembers) {
                        // added information about parent
                        if (metaAttribute != null && metaAttribute.isParentIsKey())
                            if (metaAttribute.getMetaType().isSet()) {
                                for (BaseValue value : ((BaseSet)obj).getValues())
                                    ((BaseEntity)value.getValue()).setParentInfo((BaseEntity) currentContainer, metaAttribute);
                            } else {
                                ((BaseEntity)obj).setParentInfo((BaseEntity) currentContainer, metaAttribute);
                            }



                        ((BaseEntity) currentContainer).put(localName, new BaseValue(obj));
                        flagsStack.pop();
                        hasMembers = true;
                    } else {
                        ((BaseEntity)currentContainer).put(localName, new BaseValue());
                        flagsStack.pop();
                        // изменил на hasMembers = true так как, если закрытый тэг стоит в конце, то родитель
                        hasMembers = true;
                    }
                }
            }

            level--;
        }

        return false;
    }

    private static final String OPERATION_STR = "operation";

    private boolean hasOperation(StartElement startElement, OperType operType) {
        return startElement.getAttributeByName(new QName(OPERATION_STR)) != null &&
                startElement.getAttributeByName(new QName(OPERATION_STR)).getValue()
                        .equalsIgnoreCase(operType.toString());
    }

}
