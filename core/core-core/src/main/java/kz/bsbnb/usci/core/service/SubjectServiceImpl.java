package kz.bsbnb.usci.core.service;

import kz.bsbnb.usci.core.dao.SubjectDao;
import kz.bsbnb.usci.model.respondent.SubjectType;
import kz.bsbnb.usci.model.util.Text;
import kz.bsbnb.usci.util.client.TextClient;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Yernur Bakash
 */

@Service
public class SubjectServiceImpl implements SubjectService {
    private final TextClient textClient;
    private final SubjectDao subjectDao;

    public SubjectServiceImpl(TextClient textClient, SubjectDao subjectDao) {
        this.textClient = textClient;
        this.subjectDao = subjectDao;
    }

    @Override
    public String getSubjectTypeProductPeriod(Long subjectTypeId, Long productId) {
        Long periodId = subjectDao.getSubjectTypeProductPeriod(subjectTypeId, productId);

        List<Text> periodTypes = textClient.getTextListByType(Collections.singletonList("PERIOD_TYPE"));
        Map<Long, Text> periodTypesMap = periodTypes.stream()
                .collect(Collectors.toMap(Text::getId, o -> o));

        return periodTypesMap.get(periodId).getNameRu();
    }

    @Override
    public List<SubjectType> getSubjectTypeList() {
        return subjectDao.getSubjectTypeList();
    }

    @Override
    public void updateSubjectTypeProductPeriod(Long subjectTypeId, Long productId, Long periodId) {
        subjectDao.updateSubjectTypeProductPeriod(subjectTypeId, productId, periodId);
    }
}
