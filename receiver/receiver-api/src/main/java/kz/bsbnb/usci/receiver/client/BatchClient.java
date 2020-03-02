package kz.bsbnb.usci.receiver.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * @author Jandos Iskakov
 */

@FeignClient(name = "receiver", value = "receiver")
public interface BatchClient {

    @PostMapping(value = "/batch/endBatch")
    void endBatch(@RequestParam(name = "batchId") Long batchId);

    @PostMapping(value = "/batch/incrementActualCounts")
    boolean incrementActualCounts(@RequestBody Map<Long, Long> batchesToUpdate);

}
