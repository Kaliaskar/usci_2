package kz.bsbnb.usci.receiver.batch.converter;

import kz.bsbnb.usci.model.util.DtoConverter;
import kz.bsbnb.usci.receiver.model.BatchEntry;
import kz.bsbnb.usci.receiver.model.json.BatchEntryJson;
import org.springframework.stereotype.Component;

/**
 * @author Jandos Iskakov
 */

@Component
public class BatchEntryConverter implements DtoConverter<BatchEntry, BatchEntryJson> {

    @Override
    public BatchEntryJson convertToDto(BatchEntry batchEntry) {
        BatchEntryJson batchEntryJson = new BatchEntryJson();
        batchEntryJson.setId(batchEntry.getId());
        batchEntryJson.setEntityId(batchEntry.getEntityId());
        batchEntryJson.setRepDate(batchEntry.getRepDate());
        batchEntryJson.setValue(batchEntry.getValue());
        batchEntryJson.setUpdateDate(batchEntry.getUpdateDate());
        batchEntryJson.setMaintenance(batchEntry.isMaintenance());
        batchEntryJson.setUserId(batchEntry.getUserId());
        return batchEntryJson;
    }

    @Override
    public void updateFromDto(BatchEntry batchEntry, BatchEntryJson dto) {
        throw new UnsupportedOperationException();
    }

}
