package kz.bsbnb.usci.sync.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

/**
 * @author Jandos Iskakov
 */

@FeignClient(name = "sync", value = "sync")
public interface SyncClient {

    @GetMapping(value = "/getQueueSize")
    int getQueueSize();

    @GetMapping(value = "/getFinishedBatches")
    Set<Long> getFinishedBatches();

    @PostMapping(value = "/batchFinishedInReader")
    void batchFinishedInReader(@RequestParam(name = "batchId") Long batchId);

}
