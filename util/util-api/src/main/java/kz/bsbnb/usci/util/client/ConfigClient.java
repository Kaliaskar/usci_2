package kz.bsbnb.usci.util.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Set;

/**
 * @author Jandos Iskakov
 */

@FeignClient(name = "utils")
public interface ConfigClient {

    @GetMapping(value = "/config/getDigitalSigningOrgIds")
    Set<Long> getDigitalSigningOrgIds();

    @GetMapping(value = "/config/getQueueAlgorithm")
    String getQueueAlgorithm();

    @GetMapping(value = "/config/getPriorityRespondentIds")
    Set<Long> getPriorityRespondentIds();

    @GetMapping(value = "/config/getManifestXsd")
    byte[] getManifestXsd();

    @GetMapping(value = "/config/getConfirmText")
    String getConfirmText();

}
