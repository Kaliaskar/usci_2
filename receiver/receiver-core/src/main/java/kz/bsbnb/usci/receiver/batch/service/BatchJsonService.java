package kz.bsbnb.usci.receiver.batch.service;

import kz.bsbnb.usci.receiver.model.BatchStatusJson;
import kz.bsbnb.usci.receiver.model.json.BatchJson;
import kz.bsbnb.usci.util.json.ext.ExtJsList;
import org.apache.tomcat.jni.Local;

import java.time.LocalDate;
import java.util.List;

/**
 * @author Yernur Bakash
 * @author Jandos Iskakov
 */

public interface BatchJsonService {

    ExtJsList getBatchList(List<Long> respondentIds, long userId, boolean isNb, LocalDate reportDate, int pageIndex, int pageSize);

    ExtJsList getBatchStatusList(long batchId, List<String> statusTypes);

    List<BatchJson> getPendingBatchList(List<Long> respondentIds);

    List<BatchJson> getBatchListToSign(long respondentId, long userId);

    List<BatchJson> getMaintenanceBatchList(List<Long> respondentIds, LocalDate reportDate, long userId);

    List<BatchJson> getBatchListToApprove(Long productId, Long respondentId);

    byte[] getExcelFromBatch(List<BatchJson> batchList, List<String> columnNames);

    byte[] getExcelFromProtocol(List<BatchStatusJson> protocolList, List<String> columnNames);

    byte[] getExcelFromQueueBatch(List<BatchJson> batchList, List<String> columnNames);

}
