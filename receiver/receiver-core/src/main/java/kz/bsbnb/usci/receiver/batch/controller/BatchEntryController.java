package kz.bsbnb.usci.receiver.batch.controller;

import kz.bsbnb.usci.receiver.batch.converter.BatchEntryConverter;
import kz.bsbnb.usci.receiver.batch.service.BatchEntryService;
import kz.bsbnb.usci.receiver.model.BatchEntry;
import kz.bsbnb.usci.receiver.model.json.BatchEntryJson;
import kz.bsbnb.usci.util.json.ext.ExtJsJson;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * @author Jandos Iskakov
 */

@RestController
@RequestMapping(value = "/batchEntry")
public class BatchEntryController {
    private final BatchEntryConverter converter;
    private final BatchEntryService batchEntryService;

    public BatchEntryController(BatchEntryConverter converter,
                                BatchEntryService batchEntryService) {
        this.converter = converter;
        this.batchEntryService = batchEntryService;
    }

    @PostMapping(value = "saveXml")
    public void saveXml(@RequestParam(name = "xmlData") String xmlData,
                        @RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(name = "userId") Long userId,
                        @RequestParam(name = "entityId", required = false) Long entityId,
                        @RequestParam(name = "metaClassId", required = false) Long metaClassId,
                        @RequestParam(name = "isMaintenance") boolean isMaintenance) {
        BatchEntry batchEntry = new BatchEntry();

        batchEntry.setValue(xmlData);
        batchEntry.setRepDate(date);
        batchEntry.setMetaClassId(metaClassId);
        batchEntry.setEntityId(entityId);
        batchEntry.setUserId(userId);
        batchEntry.setMaintenance(isMaintenance);
        batchEntry.setUpdateDate(LocalDateTime.now());

        batchEntryService.save(batchEntry);
    }

    @GetMapping(value = "getBatchEntriesByUserId")
    public ExtJsJson getBatchEntriesByUserId(@RequestParam Long userId) {
        return new ExtJsJson(batchEntryService.getBatchEntriesByUserId(userId).stream()
                .map(converter::convertToDto)
                .collect(Collectors.toList()));
    }

    @GetMapping(value = "getBatchEntry")
    public BatchEntryJson getBatchEntry(@RequestParam Long batchEntryId) {
        return converter.convertToDto(batchEntryService.load(batchEntryId));
    }

    @PostMapping(value = "deleteBatchEntry")
    public void deleteBatchEntry(@RequestParam Long batchEntryId) {
        batchEntryService.delete(batchEntryId);
    }

    @PostMapping(value = "confirmBatchEntries")
    public void confirmBatchEntries(@RequestParam Long userId, @RequestParam boolean isNb) {
        batchEntryService.confirmBatchEntries(userId, isNb);
    }

}
