package kz.bsbnb.usci.eav.exception;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseEntityOutput;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.model.exception.UsciException;

/**
 * Исключение когда сущность закрыта
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

public class ClosedBaseEntityException extends UsciException {

    public ClosedBaseEntityException(BaseEntity baseEntity) {
        MetaClass metaClass = baseEntity.getMetaClass();
        String entityText = BaseEntityOutput.getEntityAsString(baseEntity, true);

        if (metaClass.isDictionary()) {
            setErrorMessage(String.format("Справочник %s является закрытым с даты %s", entityText, baseEntity.getReportDate()));
        } else if (getErrorMessages().isEmpty()) {
            setErrorMessage(String.format("Сущность %s является закрытой с даты %s. " +
                    "Изменения после закрытия сущностей не является возможным", entityText, baseEntity.getReportDate()));
        }
    }

}
