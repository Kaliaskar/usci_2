package kz.bsbnb.usci.sync.controller;

import kz.bsbnb.usci.sync.service.SyncService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * @author Jandos Iskakov
 */

@RestController
public class SyncController {
    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @GetMapping(value = "/getQueueSize")
    public int getQueueSize() {
        return syncService.getQueueSize();
    }

    @GetMapping(value = "/getFinishedBatches")
    public Set<Long> getFinishedBatches() {
        return syncService.getFinishedBatches();
    }

    @PostMapping(value = "/batchFinishedInReader")
    public void batchFinishedInReader(@RequestParam(name = "batchId") Long batchId) {
        syncService.batchFinishedInReader(batchId);
    }

}
