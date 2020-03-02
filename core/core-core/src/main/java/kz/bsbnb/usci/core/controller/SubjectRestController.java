package kz.bsbnb.usci.core.controller;

import kz.bsbnb.usci.core.service.SubjectService;
import kz.bsbnb.usci.model.respondent.SubjectType;
import kz.bsbnb.usci.util.json.ext.ExtJsJson;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/subject")
public class SubjectRestController {
    private final SubjectService subjectService;

    public SubjectRestController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping(value = "getSubjectTypeProductPeriod")
    public String getSubjectTypeProductPeriod(@RequestParam(name = "subjectTypeId") Long subjectTypeId,
                                              @RequestParam(name = "productId") Long productId) {
        return subjectService.getSubjectTypeProductPeriod(subjectTypeId, productId);
    }

    @GetMapping(value = "getSubjectTypeList")
    public List<SubjectType> getSubjectTypeList() {
        return subjectService.getSubjectTypeList();
    }

    @PostMapping(value = "updateSubjectType")
    public ExtJsJson updateSubjectTypeProductPeriod(@RequestParam(name = "subjectTypeId") Long subjectTypeId,
                                                    @RequestParam(name = "productId") Long productId,
                                                    @RequestParam(name = "periodId") Long periodId) {
        subjectService.updateSubjectTypeProductPeriod(subjectTypeId, productId, periodId);
        return new ExtJsJson(true);
    }
}
