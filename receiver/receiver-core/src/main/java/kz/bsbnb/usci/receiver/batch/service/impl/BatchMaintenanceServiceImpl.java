package kz.bsbnb.usci.receiver.batch.service.impl;

import kz.bsbnb.usci.receiver.batch.service.BatchMaintenanceService;
import kz.bsbnb.usci.receiver.batch.service.BatchService;
import kz.bsbnb.usci.receiver.model.Batch;
import kz.bsbnb.usci.receiver.model.BatchType;
import kz.bsbnb.usci.receiver.processor.BatchReceiver;
import org.springframework.stereotype.Service;

@Service
public class BatchMaintenanceServiceImpl implements BatchMaintenanceService {
      private  final BatchService batchService;
      private  final BatchReceiver batchReceiver;


    BatchMaintenanceServiceImpl(BatchService batchService,
                                BatchReceiver batchReceiver ) {
      this.batchService = batchService;
      this.batchReceiver = batchReceiver;
    }

    @Override
    public void confirmBatchMaintenance(Long batchId) {
       batchReceiver.processBatch(batchId);
    }
}
