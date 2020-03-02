package kz.bsbnb.usci.receiver.batch.service.impl;

import kz.bsbnb.usci.core.client.RespondentClient;
import kz.bsbnb.usci.eav.client.ProductClient;
import kz.bsbnb.usci.eav.dao.BaseEntityStatusDao;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.receiver.batch.service.BatchService;
import kz.bsbnb.usci.receiver.dao.BatchDao;
import kz.bsbnb.usci.receiver.dao.BatchStatusDao;
import kz.bsbnb.usci.receiver.model.Batch;
import kz.bsbnb.usci.receiver.model.BatchStatus;
import kz.bsbnb.usci.receiver.model.BatchStatusType;
import kz.bsbnb.usci.receiver.model.BatchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * @author Maksat Nussipzhan
 * @author Olzhas Kaliaskar
 * @author Daulethan Tulendiev
 */

@Service
public class BatchServiceImpl implements BatchService {
    private static final Logger logger = LoggerFactory.getLogger(BatchServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMAT_BATCH = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final BatchDao batchDao;
    private final BatchStatusDao batchStatusDao;
    private final RespondentClient respondentClient;
    private final BaseEntityStatusDao baseEntityStatusDao;
    private final ProductClient productClient;

    @Value("${batch.save.dir}")
    private String batchSaveDir = "";

    public BatchServiceImpl(BatchDao batchDao,
                            BatchStatusDao batchStatusDao,
                            RespondentClient respondentClient,
                            BaseEntityStatusDao baseEntityStatusDao,
                            ProductClient productClient) {
        this.batchDao = batchDao;
        this.batchStatusDao = batchStatusDao;
        this.respondentClient = respondentClient;
        this.baseEntityStatusDao = baseEntityStatusDao;
        this.productClient = productClient;
    }

    @Override
    public Long save(Batch batch) {
        return batchDao.save(batch);
    }

    @Override
    public Batch getBatch(long batchId) {
        Batch batch = batchDao.load(batchId);

        // batchDao подгружает только id продукта, посему подгрузим сам продукт полностью
        if (batch.getProduct() != null)
            batch.setProduct(productClient.getProductById(batch.getProduct().getId()));

        if (batch.getRespondentId() != null)
            batch.setRespondent(respondentClient.getRespondentById(batch.getRespondentId()));

        // если файл не удалось распарсить и вытянуть параметры небходимые для его сохранения на файловом сервере
        // значит его не сможем достать
        if (batch.getReportDate() == null || batch.getRespondentId() == null || batch.getHash() == null) {
            return batch;
        }

        //замечание!!! каталог где расположены батчи формируем программно
        //то есть batch.getFileName это путь где он расположен изначально когда загружался
        File file = new File(getFullFilePath(batch));

        try {
            byte[] bytes = FileCopyUtils.copyToByteArray(file);
            batch.setContent(bytes);
        } catch (IOException e) {
            throw new UsciException(e);
        }

        return batch;
    }

    @Override
    public Batch uploadBatch(Batch batch) {
        batch.setHash(DigestUtils.md5DigestAsHex(batch.getContent()));

        saveBatchFile(batch);
        batchDao.save(batch);

        return batch;
    }

    private void saveBatchFile(Batch batch) {
        // создаем директорию для файла в формате кредитор->отчетная дата
        File repDateDir = new File(getRespondentDirPath(batch));

        if (!repDateDir.exists()) {
            repDateDir.mkdirs();
        }

        File outputFile = new File(getFullFilePath(batch));

        try {
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }

            // копируем непосредственно сам батч в файл
            FileCopyUtils.copy(batch.getContent(), outputFile);
        } catch (IOException e) {
            logger.error("Ошибка создания файла батча на сервере", e);
            throw new UsciException(e);
        }
    }

    private String getFullFilePath(Batch batch) {
        if (batch.getReportDate() == null || batch.getRespondentId() == null || batch.getHash() == null)
            throw new UsciException("RepDate, respondentId и хэш необходимы для пути к файлу");

        return batchSaveDir + "/" + batch.getReportDate().format(DATE_FORMAT_BATCH) +
                "/" + batch.getRespondentId() + "/" + batch.getHash() + ".zip";
    }

    private String getRespondentDirPath(Batch batch) {
        if (batch.getReportDate() == null || batch.getRespondentId() == null)
            throw new UsciException("RepDate, respondentId и хэш необходимы для пути к файлу");

        return batchSaveDir + "/" + batch.getReportDate().format(DATE_FORMAT_BATCH) + "/" + batch.getRespondentId();
    }

    @Override
    @Transactional
    public void addBatchStatus(BatchStatus batchStatus) {
        // обновляю статус батча
        batchDao.updateBatchStatus(batchStatus.getBatchId(), batchStatus.getStatus());

        batchStatusDao.insert(batchStatus);
    }

    /**
     * вызывается из SYNC чтобы завершить загрузку батча,
     * также вызывается из receiver если есть ошибки во время парсинга
     * удаляет файл из каталога батчей после полной загрузки батча
     */
    @Override
    public void endBatch(long batchId) {
        List<BatchStatus> batchStatusList = batchStatusDao.getList(batchId);

        boolean hasError = false;
        for (BatchStatus batchStatus : batchStatusList) {
            if (BatchStatusType.ERROR.equals(batchStatus.getStatus())) {
                hasError = true;
                break;
            }
        }

        Batch batch = getBatch(batchId);

        if ((!hasError && !batch.isMaintenance() )|| (batch.isMaintenance() && batch.isMaintenanceApproved())) {
            addBatchStatus(new BatchStatus(batchId, BatchStatusType.COMPLETED));
        }

        if (!hasError && batch.isMaintenance() && !batch.isMaintenanceApproved()) {
            addBatchStatus(new BatchStatus(batchId, BatchStatusType.MAINTENANCE_REQUEST));
            batch.setBatchType(BatchType.MAINTENANCE);
            save(batch);
        } else {

            try {

                if (batch.getFilePath() != null) {
                    File file = new File(batch.getFilePath());
                    if (file.exists())
                        if (!file.delete())
                            logger.error("Не удалось удалить файл после завершения " + batch.getFilePath());
                }
            } catch (Exception e) {
                throw new UsciException("Не удалось удалить файл после завершения " + batch.getFilePath(), e);
            }
        }
    }

    @Override
    public List<Batch> getPendingBatchList() {
        return batchDao.getPendingBatchList();
    }

    @Override
    public void approveMaintenanceBatchList(List<Long> batchIds) {
        batchDao.approveMaintenanceBatchList(batchIds);
    }

    @Override
    public void declineMaintenanceBatchList(List<Long> batchIds) {
        batchDao.declineMaintenanceBatchList(batchIds);
    }

    @Override
    public List<BatchStatus> getBatchStatusList(List<Long> batchIds) {
        return batchStatusDao.getList(batchIds);
    }

    @Override
    public void cancelBatch(long batchId) {
        addBatchStatus(new BatchStatus(batchId, BatchStatusType.CANCELLED));
    }

    @Override
    public void signBatch(long batchId, String sign, String signInfo, LocalDateTime signTime) {
        Batch batch = batchDao.load(batchId);
        batch.setSignature(sign);
        batch.setSignInfo(signInfo);
        batch.setSignTime(signTime);
        batchDao.save(batch);
    }

    @Override
    public boolean clearActualCount(long batchId) {
        try {
            batchDao.clearActualCount(batchId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public boolean incrementActualCounts(Map<Long, Long> batchesToUpdate) {
        try {
            for (Long batchId : batchesToUpdate.keySet()) {
                batchDao.incrementActualCount(batchId, batchesToUpdate.get(batchId));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public void setBatchTotalCount(long batchId, long totalCount) {
        batchDao.setBatchTotalCount(batchId, totalCount);
    }

    @Override
    public int getErrorEntitiesCount(Long batchId) {
        return baseEntityStatusDao.getErrorEntityCount(batchId);
    }

    @Override
    public int getSuccessEntitiesCount(Long batchId) {
        return baseEntityStatusDao.getSuccessEntityCount(batchId);
    }

}
