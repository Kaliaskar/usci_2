package kz.bsbnb.usci.wsclient.job;

import kz.bsbnb.usci.wsclient.service.KGDService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class KGDServiceJob {
    private static final Logger logger = LoggerFactory.getLogger(KGDServiceJob.class);

    private final KGDService kgdService;

    public KGDServiceJob(KGDService kgdService) {
        this.kgdService = kgdService;
    }

    @Scheduled(cron = "1 * * * * *")
    public void sendRequestToKGD() {
        LocalDate localDate = LocalDate.of(2019,5,2);
        kgdService.ctrRequest(localDate, null, false);
    }
}
