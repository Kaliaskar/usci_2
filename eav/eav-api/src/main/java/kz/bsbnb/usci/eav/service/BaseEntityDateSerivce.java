package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.eav.model.base.BaseEntity;

import java.time.LocalDate;

public interface BaseEntityDateSerivce {

    LocalDate getMaxReportDate(BaseEntity baseEntity, LocalDate reportDate);

    LocalDate getMinReportDate(BaseEntity baseEntity, LocalDate reportDate);

    LocalDate getMinReportDate(BaseEntity baseEntity);

    LocalDate getMaxReportDate(BaseEntity baseEntity);


}
