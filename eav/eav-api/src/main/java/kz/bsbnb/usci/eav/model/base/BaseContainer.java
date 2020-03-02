package kz.bsbnb.usci.eav.model.base;

import java.util.Collection;

/**
 * @author Kanat Tulbassiev
 */

public interface BaseContainer extends BaseType {

    Collection<BaseValue> getValues();

    int getValueCount();

}
