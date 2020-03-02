package kz.bsbnb.usci.brms;

import kz.bsbnb.usci.eav.repository.MetaClassRepository;
import kz.bsbnb.usci.eav.service.BaseEntityLoadService;
import kz.bsbnb.usci.eav.service.BaseEntityProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class StaticContextInitializer {

    @Autowired
    private BaseEntityProcessor rulesProcessor;
    @Autowired
    private BaseEntityLoadService rulesLoad;
    @Autowired
    private MetaClassRepository rulesMeta;

    @PostConstruct
    public void init() {
        BRMSHelper.setRulesProcessor(rulesProcessor);
        BRMSHelper.setRulesLoad(rulesLoad);
        BRMSHelper.setRulesMeta(rulesMeta);
    }

}
