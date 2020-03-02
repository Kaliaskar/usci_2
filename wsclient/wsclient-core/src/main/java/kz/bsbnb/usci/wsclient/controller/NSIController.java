package kz.bsbnb.usci.wsclient.controller;

import kz.bsbnb.usci.util.SqlJdbcConverter;
import kz.bsbnb.usci.wsclient.service.NSIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping(value = "/nsi")
public class NSIController {
    private static final Logger logger = LoggerFactory.getLogger(NSIController.class);

    private final NSIService nsiService;

    public NSIController(NSIService nsiService) {
        this.nsiService = nsiService;
    }

    @PostMapping(value = "saveCurrencyRates")
    public void saveCurrencyRates(@RequestParam String date) {
        LocalDate convertedDate = SqlJdbcConverter.convertToLocalDate(date);
        LocalDate courseDate = nsiService.getMaxCourseDate();
        if (courseDate.compareTo(convertedDate) < 0) {
            nsiService.saveCurrencyRates(courseDate.plusDays(1), convertedDate);
        }

    }
}
