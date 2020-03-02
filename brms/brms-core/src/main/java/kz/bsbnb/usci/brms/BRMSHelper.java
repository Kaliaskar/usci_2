package kz.bsbnb.usci.brms;

import kz.bsbnb.usci.eav.repository.MetaClassRepository;
import kz.bsbnb.usci.eav.service.BaseEntityLoadService;
import kz.bsbnb.usci.eav.service.BaseEntityProcessor;

/**
 * @author Artur Tkachenko
 * @author Baurzhan Makhanbetov
 */

public class BRMSHelper {

    private BRMSHelper() {
    }

    public static BaseEntityProcessor rulesProcessor;
    public static BaseEntityLoadService rulesLoad;
    public static MetaClassRepository rulesMeta;

    public static void setRulesProcessor(BaseEntityProcessor processor) {
        rulesProcessor = processor;
    }

    public static void setRulesLoad(BaseEntityLoadService load) {
        rulesLoad = load;
    }

    public static void setRulesMeta(MetaClassRepository meta) {
        rulesMeta = meta;
    }

}
