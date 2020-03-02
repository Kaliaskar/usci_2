package kz.bsbnb.usci.receiver.service;

import kz.bsbnb.usci.receiver.model.Batch;

public interface MailService {

    void notifyRegulatorMaintenance(Batch batch);

    void notifyBatchProcessCompleted(Batch batch);

}
