package kz.bsbnb.usci.wsclient.dao;

import kz.bsbnb.usci.wsclient.model.currency.NSIEntity;

import java.time.LocalDate;
import java.util.List;

public interface NSIDao {

    void saveCurrencyRates(List<NSIEntity> currList);

    LocalDate getMaxCourseDate();

    void saveHolidayDates(List<NSIEntity> currList);

}
