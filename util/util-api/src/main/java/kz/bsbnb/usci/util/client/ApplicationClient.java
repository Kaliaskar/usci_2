package kz.bsbnb.usci.util.client;

import kz.bsbnb.usci.model.util.Application;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "utils")
public interface ApplicationClient {

    @GetMapping(value = "/config/getApplicationList")
    List<Application> getApplicationList();

}
