package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseEntityRegistry;
import kz.bsbnb.usci.eav.model.core.BaseEntityDate;
import kz.bsbnb.usci.eav.model.core.EavHub;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.model.exception.UsciException;

import java.util.*;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

public class BaseEntityHolder {
    private final Map<MetaClass, List<BaseEntity>> processedBaseEntities = new HashMap<>();
    private final Map<MetaClass, List<BaseEntity>> newBaseEntities = new HashMap<>();

    private final List<BaseEntity> newBaseEntityEntries = new ArrayList<>();
    private final List<BaseEntity> updatedBaseEntityEntries = new ArrayList<>();
    private final List<BaseEntity> movedBaseEntityDates = new ArrayList<>();
    private final List<BaseEntity> deletedBaseEntityEntries = new ArrayList<>();

    private final List<EavHub> updatedBaseEntityHubs = new ArrayList<>();
    private final Map<BaseEntity, List<String>> addBaseEntityHubs =  new HashMap<>();
    private final List<EavHub> deletedBaseEntityHubs = new ArrayList<>();

    private final List<BaseEntityRegistry> newBaseEntityRegistries = Collections.synchronizedList(new ArrayList<>());

    private final List<BaseEntityDate> newBaseEntityDates = new ArrayList<>();
    private final List<BaseEntityDate> updatedBaseEntityDates = new ArrayList<>();

    private final Map<String, List<BaseEntity>> baseEntityPairs = new HashMap<>();

    //todo potom nado ubrat', dobavken dlya generacii kluchei v hub migrirovannyh dannyh
    private final List<BaseEntity> migrationErrorsHub = new ArrayList<>();

    private void putBaseEntity(Map<MetaClass, List<BaseEntity>> objects, BaseEntity baseEntity) {
        MetaClass metaClass = baseEntity.getMetaClass();

        if (objects.containsKey(metaClass))
            objects.get(metaClass).add(baseEntity);
        else {
            List<BaseEntity> objList = new ArrayList<>();
            objList.add(baseEntity);

            objects.put(metaClass, objList);
        }
    }

    public void putAsNewBaseEntity(BaseEntity baseEntity) {
        Objects.requireNonNull(baseEntity, "Сущность для вставки не может быть NULL");

        if (baseEntity.getId() != null)
            throw new UsciException("Сущность для вставки не должна иметь идентификатор");

        putBaseEntity(newBaseEntities, baseEntity);
    }

    public void putAsUpdatedBaseEntityEntry(BaseEntity baseEntity) {
        Objects.requireNonNull(baseEntity, "Сущность для обновления не может быть NULL");
        Objects.requireNonNull(baseEntity.getId(), "Сущность для обновления должна иметь идентификатор");

        updatedBaseEntityEntries.add(baseEntity);
    }

    public void putAsNewBaseEntityEntry(BaseEntity baseEntity) {
        Objects.requireNonNull(baseEntity, "Сущность для вставки записи не может быть NULL");
        Objects.requireNonNull(baseEntity.getId(), "Сущность для вставки записи должна иметь идентификатор");

        newBaseEntityEntries.add(baseEntity);
    }

    public void putAsUpdateBaseEntityReportDate(BaseEntity baseEntity) {
        Objects.requireNonNull(baseEntity, "Сущность для обновления не может быть NULL");
        Objects.requireNonNull(baseEntity.getId(), "Сущность для обновления должна иметь идентификатор");

        movedBaseEntityDates.add(baseEntity);
    }

    public void putAsDeletedBaseEntityEntry(BaseEntity baseEntity) {
        Objects.requireNonNull(baseEntity, "Сущность для удаления не может быть NULL");
        Objects.requireNonNull(baseEntity.getId(), "Сущность для удаления должна иметь идентификатор");

        deletedBaseEntityEntries.add(baseEntity);
    }

    public void putAsNewBaseEntityRegistry(BaseEntityRegistry baseEntityRegistry) {
        newBaseEntityRegistries.add(baseEntityRegistry);
    }

    public void putAsDeletedBasEntityHub(EavHub eavHub) {
        Objects.requireNonNull(eavHub, "Хаб для удаления не может быть NULL");

        deletedBaseEntityHubs.add(eavHub);
    }

    public void putAsProcessedBaseEntity(BaseEntity baseEntity) {
        putBaseEntity(processedBaseEntities, baseEntity);
    }

    public void putAsUpdatedBaseEntityHub(EavHub eavHub) {
        Objects.requireNonNull(eavHub, "Сущность для обновления хаба не может быть NULL");
        Objects.requireNonNull(eavHub.getEntityId(), "Сущность для обновления хаба должна иметь идентификатор");

        updatedBaseEntityHubs.add(eavHub);
    }

    public void putAsAddBaseEntityHub(BaseEntity baseEntity, List<String> oldKeys) {
        Objects.requireNonNull(baseEntity, "Сущность для удаления не может быть NULL");
        Objects.requireNonNull(baseEntity.getId(), "Сущность для удаления должна иметь идентификатор");

        addBaseEntityHubs.put(baseEntity, oldKeys);
    }

    public void putAsNewBaseEntityDate(BaseEntityDate baseEntityDate) {
        newBaseEntityDates.add(baseEntityDate);
    }

    public void putAsUpdatedBaseEntityDate(BaseEntityDate baseEntityDate) {
        updatedBaseEntityDates.add(baseEntityDate);
    }

    public BaseEntity getProcessedBaseEntity(BaseEntity baseEntity) {
        MetaClass metaClass = baseEntity.getMetaClass();

        if (!metaClass.isSearchable())
            return null;

        List<BaseEntity> entityList = processedBaseEntities.get(metaClass);
        if (entityList == null)
            return null;

        for (BaseEntity tempBaseEntity : entityList)
            if (baseEntity.equalsByKey(tempBaseEntity))
                return tempBaseEntity;

        return null;
    }

    public Map<MetaClass, List<BaseEntity>> getNewBaseEntities() {
        return newBaseEntities;
    }

    public List<BaseEntity> getNewBaseEntityEntries() {
        return newBaseEntityEntries;
    }

    public List<BaseEntityRegistry> getNewBaseEntityRegistries() {
        return newBaseEntityRegistries;
    }

    public List<BaseEntity> getUpdatedBaseEntityEntries() {
        return updatedBaseEntityEntries;
    }

    public List<BaseEntity> getMovedBaseEntityDates() {
        return movedBaseEntityDates;
    }

    public List<BaseEntity> getDeletedBaseEntityEntries() {
        return deletedBaseEntityEntries;
    }

    public List<EavHub> getUpdatedBaseEntityHubs() {
        return updatedBaseEntityHubs;
    }

    public Map<BaseEntity, List<String>> getAddBaseEntityHubs() {
        return addBaseEntityHubs;
    }

    public List<EavHub> getDeletedBaseEntityHubs() {
        return deletedBaseEntityHubs;
    }


    public void putBaseEntityPairs (Map<String, List<BaseEntity>> objects, BaseEntity baseEntity, BaseEntity baseEntitySaving) {

        if (objects.containsKey(baseEntity.getUuid().toString()))
            objects.get(baseEntity.getUuid().toString()).add(baseEntitySaving);
        else {
            List<BaseEntity> objList = new ArrayList<>();
            objList.add(baseEntitySaving);
            objects.put(baseEntity.getUuid().toString(), objList);
        }
    }

    public void saveBaseEntitySavingAppliedPair(BaseEntity baseEntitySaving, BaseEntity baseEntityApplied) {
        putBaseEntityPairs(baseEntityPairs, baseEntityApplied, baseEntitySaving);
       // baseEntityPairs.put(baseEntityApplied.getUuid().toString(), baseEntitySaving);
    }

    public Map<String, List<BaseEntity>> getBaseEntityPairs() {
        return baseEntityPairs;
    }

    public List<BaseEntityDate> getNewBaseEntityDates() {
        return newBaseEntityDates;
    }

    public List<BaseEntityDate> getUpdatedBaseEntityDates() {
        return updatedBaseEntityDates;
    }

    public void putAsMigrationErrorsHub(BaseEntity baseEntity) {

        migrationErrorsHub.add(baseEntity);
    }

    public List<BaseEntity> getMigrationErrorsHub() {
        return migrationErrorsHub;
    }

}
