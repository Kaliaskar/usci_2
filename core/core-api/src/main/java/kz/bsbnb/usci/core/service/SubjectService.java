package kz.bsbnb.usci.core.service;

import kz.bsbnb.usci.model.respondent.SubjectType;

import java.util.List;

/**
 * @author Yernur Bakash
 */

public interface SubjectService {

    String getSubjectTypeProductPeriod(Long subjectTypeId, Long productId);

    List<SubjectType> getSubjectTypeList();

    void updateSubjectTypeProductPeriod(Long subjectTypeId, Long productId, Long periodId);
}
