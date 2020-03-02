package kz.bsbnb.usci.report.service;

import kz.bsbnb.usci.report.crosscheck.CrossCheck;
import kz.bsbnb.usci.report.crosscheck.CrossCheckMessageDisplayWrapper;
import kz.bsbnb.usci.report.crosscheck.Message;

import java.time.LocalDate;
import java.util.List;

public interface CrossCheckService {

    List<CrossCheck> getCrossChecks(List<Long> creditorIds, LocalDate reportDate, Long productId);

    void crossCheck(Long userId, Long creditorId, LocalDate reportDate, long productId, String executeClause);

    void crossCheckAll(Long userId, LocalDate reportDate, long productId, String executeClause);

    List<CrossCheckMessageDisplayWrapper> getCrossCheckMessages(Long crossCheckId);

    LocalDate getFirstNotApprovedDate(Long creditorId);

    LocalDate getLastApprovedDate(Long creditorId);

    CrossCheck getCrossCheck(Long crossCheckId);

    Message getMessage(Long messageId);

    byte[] exportToExcel(CrossCheck crossCheck);
}
