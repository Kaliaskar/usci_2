package kz.bsbnb.usci.eav.model.core;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.meta.MetaAttribute;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.model.persistence.Persistable;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jandos Iskakov
 */

public class BaseEntityUtils {

    public static String getReceivedAsString(MetaClass metaClass, Set<String> received) {
        StringBuilder text = new StringBuilder();

        metaClass.getAttributes().stream()
                .sorted(Comparator.comparing(Persistable::getId))
                .forEachOrdered(metaAttribute -> text.append(received.contains(metaAttribute.getName()) ? "1" : "0"));

        return text.toString();
    }

    public static Set<String> getReceivedAsSet(MetaClass metaClass, String received) {
        if (StringUtils.isEmpty(received))
            return Collections.emptySet();

        return getReceivedAsSet(received, metaClass.getAttributes().stream()
                .sorted(Comparator.comparing(Persistable::getId))
                .collect(Collectors.toList()));
    }

    public static Set<String> getReceivedAsSet(String received, List<MetaAttribute> attributes) {
        if (StringUtils.isEmpty(received))
            return Collections.emptySet();

        Set<String> set = new HashSet<>();

        for (int i = 0; i < attributes.size() && i < received.length(); i++)
            if (received.charAt(i) == '1')
                set.add(attributes.get(i).getName());

        return set;
    }

    public static Set<String> getReceivedSet(BaseEntity baseEntity) {
        MetaClass metaClass = baseEntity.getMetaClass();

        Set<String> set = new HashSet<>();
        for (MetaAttribute metaAttribute : metaClass.getAttributes()) {
            if (baseEntity.getBaseValue(metaAttribute.getName()) != null)
                set.add(metaAttribute.getName());
        }

        return set;
    }

}
