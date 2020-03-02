package kz.bsbnb.usci.eav.model.meta;

import java.io.Serializable;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 */

public interface MetaType extends Serializable {

    String toString(String prefix);

    boolean isSet();

    boolean isComplex();

    boolean isDictionary();

}

