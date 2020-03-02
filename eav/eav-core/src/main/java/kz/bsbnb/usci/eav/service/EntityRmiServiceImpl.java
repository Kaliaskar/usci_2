package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.brms.exception.BrmsException;
import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseEntityOutput;
import kz.bsbnb.usci.eav.model.core.BaseEntityStatus;
import kz.bsbnb.usci.eav.model.core.EntityStatusType;
import kz.bsbnb.usci.model.exception.UsciException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author Maksat Nussipzhan
 * @author Jandos Iskakov
 */

@Service
public class EntityRmiServiceImpl implements EntityRmiService {
    private static final Logger logger = LoggerFactory.getLogger(EntityRmiServiceImpl.class);

    private BaseEntityProcessor baseEntityProcessor;
    private  BaseEntityStatusService baseEntityStatusService;

    public EntityRmiServiceImpl(BaseEntityProcessor baseEntityProcessor, BaseEntityStatusService baseEntityStatusService) {
        this.baseEntityProcessor = baseEntityProcessor;
        this.baseEntityStatusService = baseEntityStatusService;
    }

    public boolean process(BaseEntity mockEntity) {
        long startTime = System.currentTimeMillis();

        try {
            //logger.info("Начинаем обработку сущности {}", mockEntity);

            BaseEntity baseEntity = baseEntityProcessor.processBaseEntity(mockEntity);

            if (!baseEntity.isMaintenance()) {
                BaseEntityStatus baseEntityStatus = new BaseEntityStatus()
                        .setEntityId(baseEntity.getId())
                        .setMetaClassId(mockEntity.getMetaClass().getId())
                        .setBatchId(mockEntity.getBatchId())
                        .setIndex(mockEntity.getBatchIndex())
                        .setEntityText(BaseEntityOutput.getEntityAsString(baseEntity, true))
                        .setStatus(EntityStatusType.COMPLETED)
                        .setOperation(mockEntity.getOperation())
                        .setSystemDate(LocalDateTime.now());

                baseEntityStatusService.insert(baseEntityStatus);

                logger.info("Завершена обработка сущности, время {} {}", (System.currentTimeMillis() - startTime), baseEntity);
            } else {
                BaseEntityStatus baseEntityStatus = new BaseEntityStatus()
                        .setEntityId(baseEntity.getId())
                        .setMetaClassId(mockEntity.getMetaClass().getId())
                        .setBatchId(mockEntity.getBatchId())
                        .setIndex(mockEntity.getBatchIndex())
                        .setEntityText(BaseEntityOutput.getEntityAsString(baseEntity, true))
                        .setStatus(EntityStatusType.MAINTENANCE)
                        .setOperation(mockEntity.getOperation())
                        .setSystemDate(LocalDateTime.now());

                baseEntityStatusService.insert(baseEntityStatus);

                logger.info("Завершена обработка , сущность ушла на одобрение, время {} {}", (System.currentTimeMillis() - startTime), baseEntity);
            }

            return true;
        } catch (BrmsException be) {

            String entityText = BaseEntityOutput.getEntityAsString(mockEntity, true);
            // ошибка бизнес правил: заливаем в базу все ошибки по сущности
            for (String errorText : be.getErrorMessages()) {
                String stackTrace = null;
                if (errorText.contains("----")) {
                    stackTrace = errorText.substring(errorText.indexOf("-----"),errorText.length());
                    errorText = errorText.substring(0, errorText.indexOf("-----"));
                }
                BaseEntityStatus baseEntityStatus = new BaseEntityStatus()
                        .setEntityId(mockEntity.getId())
                        .setMetaClassId(mockEntity.getMetaClass().getId())
                        .setBatchId(mockEntity.getBatchId())
                        .setIndex(mockEntity.getBatchIndex())
                        .setEntityText(entityText)
                        .setStatus(EntityStatusType.ERROR)
                        .setOperation(mockEntity.getOperation())
                        .setErrorMessage(errorText)
                        .setErrorCode("Ошибка бизнес правил")
                        .setExceptionTrace(stackTrace)
                        .setSystemDate(LocalDateTime.now());

                baseEntityStatusService.insert(baseEntityStatus);
            }

            logger.error("Ошибка проверки сущности на бизнес правила {}", mockEntity);

            return false;
        } catch (UsciException ue) {

            String entityText = BaseEntityOutput.getEntityAsString(mockEntity, true);

            BaseEntityStatus baseEntityStatus = new BaseEntityStatus()
                    .setEntityId(mockEntity.getId())
                    .setMetaClassId(mockEntity.getMetaClass().getId())
                    .setBatchId(mockEntity.getBatchId())
                    .setIndex(mockEntity.getBatchIndex())
                    .setEntityText(entityText)
                    .setStatus(EntityStatusType.ERROR)
                    .setOperation(mockEntity.getOperation())
                    .setErrorMessage(ue.getMessage())
                    .setErrorCode(ue.getErrorCode())
                    .setExceptionTrace(ExceptionUtils.getStackTrace(ue))
                    .setSystemDate(LocalDateTime.now());

            baseEntityStatusService.insert(baseEntityStatus);

            logger.error("Ошибка обработки сущности {} ", mockEntity);

            return false;
        } catch (Exception e) {
            String errorText = null;
            if (e instanceof DuplicateKeyException) {
                errorText = "Ошибка дублирования сущности";
            } else if (e instanceof UncategorizedSQLException) {
                errorText = "Ошибка БД";
            }
            // прочие ошибки (по БД, программные, разработчика) ловим здесь
            String entityText = BaseEntityOutput.getEntityAsString(mockEntity, true);

            BaseEntityStatus baseEntityStatus = new BaseEntityStatus()
                    .setEntityId(mockEntity.getId())
                    .setMetaClassId(mockEntity.getMetaClass().getId())
                    .setBatchId(mockEntity.getBatchId())
                    .setIndex(mockEntity.getBatchIndex())
                    .setEntityText(entityText)
                    .setStatus(EntityStatusType.ERROR)
                    .setOperation(mockEntity.getOperation())
                    .setErrorMessage((e instanceof DuplicateKeyException || e instanceof UncategorizedSQLException) ? errorText : e.getMessage())
                    .setExceptionTrace(ExceptionUtils.getStackTrace(e))
                    .setSystemDate(LocalDateTime.now());

            baseEntityStatusService.insert(baseEntityStatus);

            logger.error("Ошибка обработки сущности {} ", mockEntity);

            return false;
        }
    }

}
