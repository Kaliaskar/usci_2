package kz.bsbnb.usci.receiver.batch.controller;

import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.receiver.batch.service.BatchJsonService;
import kz.bsbnb.usci.receiver.batch.service.BatchMaintenanceService;
import kz.bsbnb.usci.receiver.batch.service.BatchService;
import kz.bsbnb.usci.receiver.model.BatchFile;
import kz.bsbnb.usci.receiver.model.BatchStatus;
import kz.bsbnb.usci.receiver.model.BatchStatusJsonList;
import kz.bsbnb.usci.receiver.model.BatchStatusType;
import kz.bsbnb.usci.receiver.model.json.BatchJson;
import kz.bsbnb.usci.receiver.model.json.BatchJsonList;
import kz.bsbnb.usci.receiver.model.json.BatchSignJson;
import kz.bsbnb.usci.receiver.model.json.BatchSignJsonList;
import kz.bsbnb.usci.receiver.processor.BatchReceiver;
import kz.bsbnb.usci.receiver.queue.BatchQueueHolder;
import kz.bsbnb.usci.receiver.sign.SignatureChecker;
import kz.bsbnb.usci.util.json.ext.ExtJsList;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jandos Iskakov
 * @author Yernur Bakash
 */

@RestController
@RequestMapping(value = "/batch")
public class BatchController {
    private static final Logger logger = LoggerFactory.getLogger(BatchController.class);

    private final BatchService batchService;
    private final BatchReceiver batchReceiver;
    private final BatchQueueHolder batchQueueHolder;
    private final BatchJsonService batchJsonService;
    private final BatchMaintenanceService batchMaintenanceService;

    public BatchController(BatchService batchService,
                           BatchReceiver batchProcessor,
                           BatchQueueHolder batchQueueHolder,
                           BatchJsonService batchJsonService,
                           BatchMaintenanceService batchMaintenanceService) {
        this.batchService = batchService;
        this.batchReceiver = batchProcessor;
        this.batchQueueHolder = batchQueueHolder;
        this.batchJsonService = batchJsonService;
        this.batchMaintenanceService = batchMaintenanceService;
    }

    @PostMapping(value = "endBatch")
    public void endBatch(@RequestParam(name = "batchId") Long batchId) {
        batchService.endBatch(batchId);
    }

    @PostMapping(value = "incrementActualCounts")
    public boolean incrementActualCounts(@RequestBody Map<Long, Long> batchesToUpdate) {
        return batchService.incrementActualCounts(batchesToUpdate);
    }

    @PostMapping(value = "uploadBatch")
    public void uploadBatch(@RequestParam("file") MultipartFile[] files,
                            @RequestParam(name = "isNb") Boolean isNb,
                            @RequestParam(name = "userId") Long userId) {
        for (MultipartFile uploadedFile : files) {
            BatchFile batchFile = new BatchFile();
            batchFile.setNb(isNb);
            batchFile.setUserId(userId);

            try {
                batchFile.setFileContent(uploadedFile.getBytes());
                batchFile.setFileName(uploadedFile.getOriginalFilename());
            } catch (IOException e) {
                throw new UsciException("Ошибка загрузки батча");
            }

            batchReceiver.receiveBatch(batchFile);
        }
    }

    @GetMapping(value = "getBatchList")
    public ExtJsList getBatchList(@RequestParam(name = "respondentIds") List<Long> respondentIds,
                                  @RequestParam(name = "userId") Long userId,
                                  @RequestParam(name = "isNb") Boolean isNb,
                                  @RequestParam(name = "reportDate")  @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate reportDate,
                                  @RequestParam(name = "page") Integer pageIndex,
                                  @RequestParam(name = "limit") Integer pageSize) {
        return batchJsonService.getBatchList(respondentIds, userId, isNb, reportDate, pageIndex, pageSize);
    }

    @GetMapping(value = "getBatchStatusList")
    public ExtJsList getBatchStatusList(@RequestParam(name = "batchId") Long batchId,
                                        @RequestParam(name = "statusTypes") List<String> statusTypes) {
        return batchJsonService.getBatchStatusList(batchId, statusTypes);
    }

    @GetMapping(value = "getBatchContent")
    public byte[] getBatchContent(@RequestParam(name = "batchId") Long batchId) {
        return batchService.getBatch(batchId).getContent();
    }

    @PostMapping(value = "getBatchExcelContent")
    public byte[] getBatchExcelContent(@RequestBody BatchJsonList batchJsonList) {
        return batchJsonService.getExcelFromBatch(batchJsonList.getBatchList(), batchJsonList.getColumnList());
    }

    @PostMapping(value = "getProtocolExcelContent")
    public byte[] getProtocolExcelContent(@RequestBody BatchStatusJsonList batchStatusJsonList) {
        return batchJsonService.getExcelFromProtocol(batchStatusJsonList.getProtocolList(), batchStatusJsonList.getColumnList());
    }

    @GetMapping(value = "getPendingBatchList")
    public List<BatchJson> getPendingBatchList(@RequestParam(name = "respondentIds") List<Long> respondentIds) {
        return batchJsonService.getPendingBatchList(respondentIds);
    }

    @GetMapping(value = "getMaintenanceBatchList")
    public List<BatchJson> getMaintenanceBatchList(@RequestParam(name = "respondentIds") List<Long> respondentIds,
                                                   @RequestParam(name = "reportDate") @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate reportDate,
                                                   @RequestParam(name = "userId") Long userId) {
        return batchJsonService.getMaintenanceBatchList(respondentIds, reportDate, userId);
    }

    @PostMapping(value = "approveAndSendMaintenance")
    public void approveAndSendMaintenance(@RequestParam(name = "batchIds") List<Long> batchIds) {
        batchService.approveMaintenanceBatchList(batchIds);
        for (Long batchId : batchIds) {
           batchReceiver.processBatch(batchId);
        }
    }

    @PostMapping(value = "declineAndSendMaintenance")
    public void declineAndSendMaintenance(@RequestParam(name = "batchIds") List<Long> batchIds) {
        batchService.declineMaintenanceBatchList(batchIds);
        for (Long batchId : batchIds) {
            batchReceiver.declineMaintenanceBatch(batchId);
        }
    }

    @PostMapping(value = "reloadQueueConfig")
    public void reloadQueueConfig() {
        batchQueueHolder.reloadConfig();
    }

    @GetMapping(value = "getQueuePreviewBatches")
    public ExtJsList getQueuePreviewBatches(@RequestParam(name = "respondentsWithPriority") Set<Long> respondentsWithPriority,
                                            @RequestParam(name = "queueAlgo") String queueAlgo) {
        return new ExtJsList(batchQueueHolder.getOrderedBatches(respondentsWithPriority, queueAlgo));
    }

    @PostMapping(value = "getQueueBatchExcelContent")
    public byte[] getQueueBatchExcelContent(@RequestBody BatchJsonList batchJsonList) {
        return batchJsonService.getExcelFromQueueBatch(batchJsonList.getBatchList(), batchJsonList.getColumnList());
    }

    @GetMapping(value = "getBatchListToSign")
    public List<BatchJson> getBatchListToSign(@RequestParam(name = "respondentId") Long respondentId,
                                              @RequestParam(name = "userId") Long userId) {
        return batchJsonService.getBatchListToSign(respondentId, userId);
    }

    @PostMapping(value = "saveSignedBatchList")
    public void saveSignedBatchList(@RequestParam(name = "respondentBin") String respondentBin,
                                    @RequestBody BatchSignJsonList batchSignJsonList) {
        String ocspServiceUrl = "http://91.195.226.34:62255";
        SignatureChecker checker = new SignatureChecker(respondentBin, ocspServiceUrl);
        for (BatchSignJson batch : batchSignJsonList.getBatchList()) {
            try {
                checker.checkAndUpdate(batch);
                batchService.signBatch(batch.getId(), batch.getSignature(), batch.getInformation(), batch.getSigningTime());
                batchReceiver.processBatch(batch.getId());
            } catch (UsciException e) {
                logger.error("По батчу обнаружены ошибки id = {}, ошибка = {}", batch.getId(), e);

                batchService.addBatchStatus(new BatchStatus()
                        .setBatchId(batch.getId())
                        .setStatus(BatchStatusType.ERROR)
                        .setText(e.getMessage())
                        .setExceptionTrace(e != null? ExceptionUtils.getStackTrace(e): null)
                        .setReceiptDate(LocalDateTime.now()));

                batchService.endBatch(batch.getId());
            }
        }
    }

    @PostMapping(value = "cancelBatch")
    public void cancelBatch(@RequestParam(name = "batchIds") List<Long> batchIds) {
        for (Long batchId : batchIds) {
            batchService.cancelBatch(batchId);
        }
    }

    @PostMapping(value = "removeBatchFromQueue")
    public void removeBatchFromQueue(@RequestParam(name = "batchId") Long batchId) {
        batchQueueHolder.removeBatch(batchId);
    }

    @GetMapping(value = "getBatchListToApprove")
    public List<BatchJson> getBatchListToApprove(@RequestParam(name = "productId") Long productId,
                                                 @RequestParam(name = "respondentId") Long respondentId) {
        return batchJsonService.getBatchListToApprove(productId, respondentId);
    }

}