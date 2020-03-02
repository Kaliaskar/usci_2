package kz.bsbnb.usci.receiver.reader;

import kz.bsbnb.usci.model.Constants;
import kz.bsbnb.usci.model.respondent.Respondent;
import kz.bsbnb.usci.receiver.model.Batch;
import kz.bsbnb.usci.receiver.model.BatchType;
import kz.bsbnb.usci.receiver.model.deprecated.InfoData;
import kz.bsbnb.usci.receiver.model.exception.ReceiverException;
import kz.bsbnb.usci.receiver.processor.BatchReceiverImpl;
import kz.bsbnb.usci.receiver.validator.XsdValidator;
import kz.bsbnb.usci.util.client.ConfigClient;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Aibek Bukabayev
 * @author Maksat Nussipzhan
 * @author Artur Tkachenko
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 */

@Component
public class ManifestReaderImpl implements ManifestReader {
    private final Logger logger = LoggerFactory.getLogger(BatchReceiverImpl.class);

    private final ConfigClient configClient;
    private final XsdValidator xsdValidator;

    public ManifestReaderImpl(ConfigClient configClient, XsdValidator xsdValidator) {
        this.configClient = configClient;
        this.xsdValidator = xsdValidator;
    }

    @Override
    public void read(Batch batch) {
        ZipFile zipFile = null;

        try {
            byte[] zipBytes;
            try (InputStream in = new FileInputStream(batch.getFilePath())) {
                zipBytes = StreamUtils.copyToByteArray(in);
            }

            batch.setContent(zipBytes);

            // необходимо определить какой вид батча нам высылают
            // если в архиве есть манифест файло то это батч ЕССП, иначе батч кредитного регистра
            // manifest файл имеет старый и новый формат посему часть кода помечано как @deprecated
            zipFile = new ZipFile(batch.getFilePath());
            ZipEntry manifestEntry2019 = zipFile.getEntry("manifest.xml");
            ZipEntry manifestEntry = zipFile.getEntry("usci_manifest.xml");

            if (manifestEntry2019 == null && manifestEntry == null)
                //readXmlInfoTag(batch);
                throw new ReceiverException("ZIP-файл не содержит manifest файл");
            else if (manifestEntry2019 != null)
                readManifestXmlBefore2019(batch, zipFile, manifestEntry2019);
            else
                readManifestXml(batch, zipFile, manifestEntry);

        } catch (IOException e) {
            throw new ReceiverException("Ошибка I/O: " + e.getMessage());
        } finally {
            try {
                if (zipFile != null)
                    zipFile.close();
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * делает сверку кредитора из батча и кредитора которые привязаны к пользователю
     * если все ок то возвращает кредитора
     */
    @Override
    public Optional<Respondent> getRespondent(Batch batch, List<Respondent> respondents) {
        if (batch.getAddParams() == null || batch.getAddParams().size() == 0)
            return Optional.empty();

        String docType = batch.getAddParams().get("DOC_TYPE");
        String docValue = batch.getAddParams().get("DOC_VALUE");

        String code = batch.getAddParams().get("CODE");
        String bin = batch.getAddParams().get("BIN");
        String bik = batch.getAddParams().get("BIK");
        String rnn = batch.getAddParams().get("RNN");

        if (docType == null) docType = "";
        if (docValue == null) docValue = "";

        for (Respondent respondent : respondents) {
            if ((respondent.getBik() != null && docType.equals(Constants.DOC_TYPE_BIK) && respondent.getBik().equals(docValue)) ||
                (respondent.getBin() != null && docType.equals(Constants.DOC_TYPE_BIN) && respondent.getBin().equals(docValue)) ||
                (respondent.getRnn() != null && docType.equals(Constants.DOC_TYPE_RNN) && respondent.getRnn().equals(docValue)) ||
                (!StringUtils.isEmpty(code) && !StringUtils.isEmpty(respondent.getCode()) && code.equals(respondent.getCode())) ||
                (!StringUtils.isEmpty(bin) && !StringUtils.isEmpty(respondent.getBin()) && bin.equals(respondent.getBin())) ||
                (!StringUtils.isEmpty(bik) && !StringUtils.isEmpty(respondent.getBik()) && bik.equals(respondent.getBik())) ||
                (!StringUtils.isEmpty(rnn) && !StringUtils.isEmpty(respondent.getRnn()) && rnn.equals(respondent.getRnn())))
            return Optional.of(respondent);
        }

        return Optional.empty();
    }

    /**
     * парсит тег "info" батча от кредитного регистра чтобы получить оттуда информацию:
     * документы кредитора, количество пакетов в батче, отчетная дата и тд
     */
    private void readXmlInfoTag(Batch batch) {
        try {
            batch.setBatchType(BatchType.CREDIT_REGISTRY);
            batch.setProductCode("CREDIT");

            byte[] extractedBytes = null;

            try (ZipArchiveInputStream zis = new ZipArchiveInputStream(new ByteArrayInputStream(batch.getContent()))) {
                while (zis.getNextZipEntry() != null) {
                    byte[] buffer = new byte[Constants.ZIP_BUFFER_SIZE];

                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        int size;
                        while ((size = zis.read(buffer, 0, buffer.length)) != -1)
                            baos.write(buffer, 0, size);
                        extractedBytes = baos.toByteArray();
                    }
                }
            }

            if (extractedBytes == null)
                throw new ReceiverException("ZIP-файл не содержит каких-либо файлов");

            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLEventReader eventReader = inputFactory.createXMLEventReader(new ByteArrayInputStream(extractedBytes));

            // считываем только тег <info> чтобы получить информацию про отчетную дату, кредитора и тд
            InfoParser infoParser = new InfoParser();
            infoParser.parse(eventReader);

            InfoData infoData = infoParser.getInfoData();

            batch.setReportDate(infoData.getReportDate());
            batch.setTotalEntityCount(infoData.getActualCreditCount());
            batch.setMaintenance(infoData.isMaintenance());

            // добавляем в батч доп параметры которые получили из тега <info>
            String code = infoData.getCode();
            if (code != null && code.length() > 0) {
                batch.addParam("CODE", code.trim());
            } else {
                String docType = infoData.getDocType();
                String docValue = infoData.getDocValue();

                if (!StringUtils.isEmpty(docType) && !StringUtils.isEmpty(docValue)) {
                    batch.addParam("DOC_TYPE", docType.trim());
                    batch.addParam("DOC_VALUE", docValue.trim());
                }
            }

        } catch (IOException e) {
            throw new ReceiverException("Ошибка I/O: " + e.getMessage());
        } catch (XMLStreamException e) {
            throw new ReceiverException("Не корректный XML файл");
        }
    }

    /**
     * парсит файл манифест батча чтобы получить оттуда информацию:
     * доп параметры по кредитору, кол-во кредитов и тд
     * формат manifest файла до 01.01.2019
     */
    @Deprecated
    private void readManifestXmlBefore2019(Batch batch, ZipFile zipFile, ZipEntry manifestEntry) {
        try {
            batch.setBatchType(BatchType.USCI_OLD);
            batch.setProductCode("CREDIT");

            InputStream inManifest = zipFile.getInputStream(manifestEntry);

            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLEventReader eventReader = inputFactory.createXMLEventReader(inManifest);

            // парсим манифест чтобы считать данные батча
            kz.bsbnb.usci.receiver.reader.deprecated.ManifestParser manifestParser = new kz.bsbnb.usci.receiver.reader.deprecated.ManifestParser();
            manifestParser.parse(eventReader);
            kz.bsbnb.usci.receiver.model.deprecated.ManifestData manifestData = manifestParser.getManifestData();

            batch.setMaintenance(manifestData.isMaintenance());

            if (batch.getUserId() == null)
                batch.setUserId(manifestData.getUserId());

            batch.setTotalEntityCount((long) manifestData.getSize());
            batch.setReportDate(manifestData.getReportDate());

            batch.setAddParams(manifestData.getAdditionalParams());
        } catch (IOException e) {
            throw new ReceiverException("Ошибка I/O: " + e.getMessage());
        } catch (XMLStreamException e) {
            throw new ReceiverException("Не корректный XML файл");
        }
    }

    /**
     * парсит файл манифест батча чтобы получить оттуда информацию: продукт, кредитор
     * формат manifest файла с 01.01.2019
     */
    private void readManifestXml(Batch batch, ZipFile zipFile, ZipEntry manifestEntry) {
        try {
            batch.setBatchType(BatchType.USCI);

            InputStream inManifest = zipFile.getInputStream(manifestEntry);

            // проверяем манифест файл на xsd схему
            validateManifestXsd(inManifest);

            inManifest = zipFile.getInputStream(manifestEntry);

            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLEventReader eventReader = inputFactory.createXMLEventReader(inManifest);

            // парсим манифест чтобы считать данные батча
            ManifestParser manifestParser = new ManifestParser();
            manifestParser.parse(eventReader);
            kz.bsbnb.usci.receiver.model.ManifestData manifestData = manifestParser.getManifestData();

            batch.setTotalEntityCount(batch.getActualEntityCount());
            batch.setReportDate(manifestData.getReportDate());
            batch.setProductCode(manifestData.getProduct());

            // метод getCreditor определяет респондента через Batch.addParams
            // заполняем данное поле; желательно сделать в будущем более строгую проверку как
            // валидация вида документа и тд
            HashMap<String, String> params = new HashMap<>();
            params.put("CODE", manifestData.getRespondent());

            batch.setAddParams(params);
        } catch (IOException e) {
            throw new ReceiverException("Ошибка I/O: " + e.getMessage());
        } catch (XMLStreamException e) {
            throw new ReceiverException("Не корректный XML файл");
        }
    }

    /**
     * Выполняет валидацию XSD манифест файла
     * @param manifestStream
     * @throws IOException
     */
    private void validateManifestXsd(InputStream manifestStream) throws IOException {
        try {
            // проверяем манифест файл на xsd схему
            byte[] manifestXsd = configClient.getManifestXsd();

            // не забываем обязательно закрывать поток XSD файла
            try (InputStream xsdStream = new ByteArrayInputStream(manifestXsd)) {
                xsdValidator.validateSchema(xsdStream, manifestStream);
            }
        } catch (ReceiverException e) {
            throw new ReceiverException("Manifest не прошёл проверку XSD: " + e.getMessage());
        }
    }



}
