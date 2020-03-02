package kz.bsbnb.usci.wsclient.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@FeignClient(name = "wsclient")
public interface NSIClient {

    @PostMapping(value = "/nsi/saveCurrencyRates")
    void saveCurrencyRates(@RequestParam(name = "date") String date) ;

}
