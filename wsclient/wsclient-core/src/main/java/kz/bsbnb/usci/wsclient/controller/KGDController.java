package kz.bsbnb.usci.wsclient.controller;

import kz.bsbnb.usci.wsclient.model.ctrkgd.Request;
import kz.bsbnb.usci.wsclient.service.KGDService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(value = "/kgd")
public class KGDController {
    private final KGDService kgdService;

    public KGDController(KGDService kgdService) {
        this.kgdService = kgdService;
    }

    @GetMapping(value = "getCtrRequestList")
    public List<Request> getCtrRequestList(@RequestParam @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate reportDate){
        return kgdService.getCtrRequestList(reportDate);
    }

    @PostMapping(value = "resendCtrRequest")
    public void resendCtrRequest (@RequestParam(name = "requestId") Long requestId,
                                  @RequestParam(name = "isUpdate") boolean isUpdate,
                                  @RequestParam @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate reportDate){
        kgdService.ctrRequest(reportDate, requestId, isUpdate);
    }
}
