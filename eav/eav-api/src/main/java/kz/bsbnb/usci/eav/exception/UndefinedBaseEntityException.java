package kz.bsbnb.usci.eav.exception;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseEntityOutput;
import kz.bsbnb.usci.model.exception.UsciException;

/**
 * Исключение когда ссылочная сущность не найдена в БД
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

public class UndefinedBaseEntityException extends UsciException {

    public UndefinedBaseEntityException(BaseEntity baseEntity) {
        String entityText = BaseEntityOutput.getEntityAsString(baseEntity, true);
        setErrorMessage(String.format("Сущность %s не найдена в системе", entityText));
    }

}
