package kz.bsbnb.usci.eav.client;

import kz.bsbnb.usci.eav.model.core.BaseEntityStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "core")
public interface EntityClient {

    @PutMapping(value = "/eav/entity/addEntityStatus")
    Long addEntityStatus(@RequestBody BaseEntityStatus entityStatus);

}
