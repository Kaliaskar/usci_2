package kz.bsbnb.usci.eav.dao;

import kz.bsbnb.usci.eav.model.meta.MetaClass;

import java.util.List;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

public interface MetaClassDao {

    long save(MetaClass meta);

    MetaClass load(String className);

    MetaClass load(Long id);

    List<MetaClass> loadAll();

    void remove(MetaClass metaClass);

    List<Long> loadClassesWithReference(Long id);

}
