package kz.bsbnb.usci.receiver.batch.service.impl;

import kz.bsbnb.usci.core.client.RespondentClient;
import kz.bsbnb.usci.eav.client.ProductClient;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.eav.repository.MetaClassRepository;
import kz.bsbnb.usci.model.batch.Product;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.respondent.Respondent;
import kz.bsbnb.usci.receiver.batch.service.BatchEntryService;
import kz.bsbnb.usci.receiver.dao.BatchEntryDao;
import kz.bsbnb.usci.receiver.model.BatchEntry;
import kz.bsbnb.usci.receiver.model.BatchFile;
import kz.bsbnb.usci.receiver.processor.BatchReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Artur Tkachenko
 */

@Service
public class BatchEntryServiceImpl implements BatchEntryService {
    private static final Logger logger = LoggerFactory.getLogger(BatchEntryServiceImpl.class);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final BatchEntryDao batchEntryDao;
    private final RespondentClient respondentClient;
    private final BatchReceiver batchReceiver;
    private final MetaClassRepository metaClassRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ProductClient productClient;

    @Value("${batch.entryDir}")
    private String batchEntryDir;

    public BatchEntryServiceImpl(BatchEntryDao batchEntryDao,
                                 RespondentClient respondentClient,
                                 BatchReceiver batchReceiver,
                                 MetaClassRepository metaClassRepository,
                                 JdbcTemplate jdbcTemplate,
                                 ProductClient productClient) {
        this.batchEntryDao = batchEntryDao;
        this.respondentClient = respondentClient;
        this.batchReceiver = batchReceiver;
        this.metaClassRepository = metaClassRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.productClient = productClient;
    }

    @Override
    public Long save(BatchEntry batch) {
        return batchEntryDao.save(batch);
    }

    @Override
    public BatchEntry load(Long batchId) {
        return batchEntryDao.load(batchId);
    }

    @Override
    public List<BatchEntry> getBatchEntriesByUserId(Long userId) {
        return batchEntryDao.getBatchEntriesByUserId(userId);
    }

    @Override
    public void delete(Long batchEntryId) {
        batchEntryDao.remove(batchEntryId);
    }

    @Override
    public void confirmBatchEntries(Long userId, boolean isNb) {
        List<BatchEntry> batchEntries = getBatchEntriesByUserId(userId);

        if (batchEntries.isEmpty())
            throw new UsciException("Нет батчей для утверждения");

        List<Long> batchEntryIds = new ArrayList<>();

        Respondent respondent = respondentClient.getRespondentByUserId(userId, isNb);

        for (BatchEntry batchEntry : batchEntries) {
            String formattedReportDate = batchEntry.getRepDate().format(DATE_FORMAT);

            // определяю продукт; то есть необходимо наш батч отнести к продукту
            String productCode = null;
            MetaClass metaClass = metaClassRepository.getMetaClass(batchEntry.getMetaClassId());
            /*if (metaClass.isDictionary()) {
                productCode = "DICT";
            } else {*/
                List<Long> products = productClient.getProductIdsByMetaClassId(metaClass.getId());
                Product product = productClient.getProductById(products.get(0));
                productCode = product.getCode();
            //}

            // формируем manifest файл для каждого batchEntry
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                         "<batch>\n" +
                         "<entities xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n";

            xml += batchEntry.getValue() + "\n";

            xml += "\n</entities>\n" +
                    "</batch>";

            String manifest = "<manifest>\n" +
                    "\t<product>" + productCode + "</product>\n" +
                    "\t<report_date>" + formattedReportDate + "</report_date>\n";

            manifest += "\t<respondent>"+ respondent.getCode() + "</respondent>\n";
            manifest += "</manifest>";

            File tempBatchEntryFile = null;

            try {
                tempBatchEntryFile = File.createTempFile("tmp", ".zip", new File(batchEntryDir));
            } catch (IOException e) {
                throw new UsciException(e);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            try (ZipOutputStream zipFile = new ZipOutputStream(bos)) {
                ZipEntry zipEntryData = new ZipEntry("data.xml");
                zipFile.putNextEntry(zipEntryData);
                zipFile.write(xml.getBytes());

                //
                ZipEntry zipEntryManifest = new ZipEntry("usci_manifest.xml");
                zipFile.putNextEntry(zipEntryManifest);
                zipFile.write(manifest.getBytes());
            } catch (IOException e) {
                throw new UsciException(e);
            }

            byte[] zipBytes = bos.toByteArray();

            try (FileOutputStream fileOutputStream = new FileOutputStream(tempBatchEntryFile)) {
                fileOutputStream.write(zipBytes);
            } catch (IOException e) {
                throw new UsciException(e);
            }

            logger.info("BatchEntry(id={}, userId={}, isNb={}) отправлен на обработку", batchEntry.getId(), userId, isNb);

            BatchFile batchFile = new BatchFile(userId, isNb, tempBatchEntryFile.getPath());
            batchFile.setFileContent(zipBytes);
            batchFile.setFileName(tempBatchEntryFile.getName());
            batchFile.setBatchEntryId(batchEntry.getId());

            batchReceiver.receiveBatch(batchFile);

            batchEntryIds.add(batchEntry.getId());
        }

        batchEntryDao.setProcessed(batchEntryIds);
    }

}
