package kz.bsbnb.usci.eav.repository;

import kz.bsbnb.usci.eav.model.meta.MetaClass;

import java.util.List;

/**
 * @author Kanat Tulbassiev
 * @author Artur Tkachenko
 * @author Baurzhan Makhambetov
 */

public interface MetaClassRepository {
    MetaClass getMetaClass(String className);

    MetaClass getMetaClass(long id);

    List<MetaClass> getMetaClasses();

    void saveMetaClass(MetaClass metaClass);

    void resetCache();

    boolean delMetaClass(Long classId);

}
