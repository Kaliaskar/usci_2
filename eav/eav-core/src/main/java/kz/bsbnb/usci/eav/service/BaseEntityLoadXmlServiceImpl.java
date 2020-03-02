package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.eav.dao.BaseEntityLoadXmlDaoImpl;
import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseEntityJson;
import kz.bsbnb.usci.eav.model.base.BaseSet;
import kz.bsbnb.usci.eav.model.base.BaseValue;
import kz.bsbnb.usci.eav.model.meta.*;
import kz.bsbnb.usci.eav.model.meta.json.EntityExtJsTreeJson;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class BaseEntityLoadXmlServiceImpl implements BaseEntityLoadXmlService {

    private final BaseEntityLoadXmlDaoImpl baseEntityLoadXmlDao;

    BaseEntityLoadXmlServiceImpl(BaseEntityLoadXmlDaoImpl baseEntityLoadXmlDao ) {
        this.baseEntityLoadXmlDao = baseEntityLoadXmlDao;
    }

    @Override
    public EntityExtJsTreeJson loadBaseEntity(Long id) {
        Objects.requireNonNull(id, String.format("Отсутствует ID у сущности "));

        return baseEntityLoadXmlDao.loadBaseEntity(id);
    }

    @Override
    public void updateBaseEntity(BaseEntityJson baseEntityJson, boolean approve) {
        baseEntityLoadXmlDao.updateBaseEntity(baseEntityJson, approve);
    }

    @Override
    public void updateBaseEntityState(BaseEntityJson baseEntityJson) {
        baseEntityLoadXmlDao.updateBaseEntityState(baseEntityJson);
    }

    @Override
    public void deleteBaseEntity(Long id) {
        baseEntityLoadXmlDao.deleteBaseEntity(id);
    }

    @Override
    public List<BaseEntityJson> loadEntityForApproval( long batchId) {
        return baseEntityLoadXmlDao.loadEntityForApproval(batchId);
    }

    @Override
    public BaseEntity getBaseEntityFromJsonTree(Long respondentId, LocalDate reportDate, EntityExtJsTreeJson node, MetaClass metaClass, Long batchId) {
        BaseEntity baseEntity = new BaseEntity(metaClass, respondentId, reportDate, batchId);

        baseEntity.setEavXmlId(node.getValue() != null ? new Double(node.getValue().toString()).longValue() : null );
        baseEntity.setApproved(true);

        for (EntityExtJsTreeJson childNode: node.getChildren()) {
            MetaAttribute metaAttribute = metaClass.getMetaAttribute(childNode.getName());
            MetaType metaType = metaAttribute.getMetaType();

            if (metaType.isComplex()) {
                if (metaType.isSet()) {
                    MetaClass childSetMeta = (MetaClass) ((MetaSet) metaType).getMetaType();

                    BaseSet childBaseSet = new BaseSet(childSetMeta);
                    if (childNode.getChildren() != null && childNode.getChildren().size() > 0) {
                        baseEntity.put(metaAttribute.getName(), new BaseValue(childBaseSet));

                        for (EntityExtJsTreeJson childSetNode : childNode.getChildren()) {
                            BaseEntity childBaseSetEntity = getBaseEntityFromJsonTree(respondentId, reportDate, childSetNode, childSetMeta, batchId);
                            childBaseSet.put(new BaseValue(childBaseSetEntity));
                        }
                    } else {
                        baseEntity.put(metaAttribute.getName(), new BaseValue());
                    }
                }
                else {
                    if (childNode.getChildren() != null && childNode.getChildren().size() > 0) {
                        BaseEntity childBaseEntity = getBaseEntityFromJsonTree(respondentId, reportDate, childNode, (MetaClass) metaType, batchId);
                        baseEntity.put(metaAttribute.getName(), new BaseValue(childBaseEntity));
                    } else
                        baseEntity.put(metaAttribute.getName(), new BaseValue());
                }
            }
            else {
                if (metaType.isSet()) {
                    MetaValue childSetMetaValue = (MetaValue) ((MetaSet)metaType).getMetaType();
                    BaseSet childBaseSet = new BaseSet(new MetaSet(metaType));

                    for (EntityExtJsTreeJson childSetNode : childNode.getChildren()) {
                        Object value = MetaDataType.getCastObject(MetaDataType.valueOf(childSetNode.getMetaType()), String.valueOf(childSetNode.getValue()));
                        childBaseSet.put(new BaseValue(value));
                    }

                    baseEntity.put(metaAttribute.getName(), new BaseValue(childBaseSet));

                } else {
                    if (childNode.getNewValue() != null) {
                        Object value = MetaDataType.getCastObject(((MetaValue) metaType).getMetaDataType(), String.valueOf(childNode.getValue()));
                        Object newValue = MetaDataType.getCastObject(((MetaValue) metaType).getMetaDataType(), String.valueOf(childNode.getNewValue()));
                        BaseValue baseValue = new BaseValue(value);
                        BaseValue newBaseValue = new BaseValue(newValue);
                        newBaseValue.setMetaAttribute(metaAttribute);
                        baseValue.setNewBaseValue(newBaseValue);
                        baseEntity.put(metaAttribute.getName(), baseValue);
                    } else {
                        if (childNode.getValue() != null) {
                            Object value = MetaDataType.getCastObject(((MetaValue) metaType).getMetaDataType(), String.valueOf(childNode.getValue()));
                            baseEntity.put(metaAttribute.getName(), new BaseValue(value));
                        } else
                            baseEntity.put(metaAttribute.getName(), new BaseValue());
                    }
                }
            }
        }

        return baseEntity;
    }

}

