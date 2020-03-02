package kz.bsbnb.usci.core.dao;

import kz.bsbnb.usci.model.respondent.SubjectType;

import java.util.List;

public interface SubjectDao {

    Long getSubjectTypeProductPeriod(Long subjectTypeId, Long productId);

    List<SubjectType> getSubjectTypeList();

    void updateSubjectTypeProductPeriod(Long subjectTypeId, Long productId, Long periodId);

}
