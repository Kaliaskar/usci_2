package kz.bsbnb.usci.wsclient.service;

import java.time.LocalDate;

public interface NSIService {

    void saveCurrencyRates(LocalDate beginDate, LocalDate endDate);

    LocalDate getMaxCourseDate();

    void saveHolidayDates(LocalDate beginDate, LocalDate endDate);
}
