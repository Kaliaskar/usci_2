package kz.bsbnb.usci.wsclient.model.currency;

import java.time.LocalDate;

public class HolidayEntityCustom {
    private LocalDate holidayDate;
    private Long holidayType;

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    public Long getHolidayType() {
        return holidayType;
    }

    public void setHolidayType(Long holidayType) {
        this.holidayType = holidayType;
    }
}
