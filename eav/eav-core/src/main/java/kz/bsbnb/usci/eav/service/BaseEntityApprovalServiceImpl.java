package kz.bsbnb.usci.eav.service;


import kz.bsbnb.usci.brms.exception.BrmsException;
import kz.bsbnb.usci.eav.model.base.*;
import kz.bsbnb.usci.eav.model.core.BaseEntityStatus;
import kz.bsbnb.usci.eav.model.core.EntityStatusType;
import kz.bsbnb.usci.eav.model.meta.*;
import kz.bsbnb.usci.eav.model.meta.json.EntityExtJsTreeJson;
import kz.bsbnb.usci.model.exception.UsciException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;


@Service
public class BaseEntityApprovalServiceImpl implements  BaseEntityApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(BaseEntityApprovalServiceImpl.class);
    private final BaseEntityProcessor baseEntityProcessor;
    private final BaseEntityStatusService baseEntityStatusService;
    private final BaseEntityLoadXmlService baseEntityLoadXmlService;
    private final MetaClassService metaClassService;


    BaseEntityApprovalServiceImpl (BaseEntityProcessor baseEntityProcessor,
                                   BaseEntityStatusService baseEntityStatusService,
                                   BaseEntityLoadXmlService baseEntityLoadXmlService,
                                   MetaClassService metaClassService) {
        this.baseEntityProcessor = baseEntityProcessor;
        this.baseEntityStatusService = baseEntityStatusService;
        this.baseEntityLoadXmlService = baseEntityLoadXmlService;
        this.metaClassService = metaClassService;
    }


    @Override
    public void approveEntityMaintenance(List<BaseEntityJson> baseEntityList, Long batchId) {
        List<BaseEntityJson> syncedBaseEntityList = Collections.synchronizedList(baseEntityList);
        synchronized (syncedBaseEntityList) {
            syncedBaseEntityList.parallelStream().forEach(baseEntityJson -> {
                if (baseEntityJson.isPreApproved()) {
                    long startTime = System.currentTimeMillis();
                    MetaClass metaClass = metaClassService.getMetaClass(baseEntityJson.getMetaClassId());
                    EntityExtJsTreeJson entityJson = baseEntityLoadXmlService.loadBaseEntity(baseEntityJson.getId());
                    BaseEntity mockEntity = baseEntityLoadXmlService.getBaseEntityFromJsonTree(baseEntityJson.getRespondentId(), baseEntityJson.getReportDate(), entityJson, metaClass, batchId);
                    mockEntity.setOperation(baseEntityJson.getOperType());

                    try {
                        //logger.info("Начинаем обработку сущности {}", mockEntity);

                        BaseEntity baseEntityApplied = baseEntityProcessor.processBaseEntity(mockEntity);

                        BaseEntityStatus baseEntityStatus = new BaseEntityStatus()
                                .setEntityId(baseEntityApplied.getId())
                                .setBatchId(baseEntityApplied.getBatchId())
                                .setApprovedEntityId(mockEntity.getEavXmlId())
                                .setStatus(EntityStatusType.COMPLETED)
                                .setErrorMessage("Обработано после одобрения")
                                .setOperation(baseEntityApplied.getOperation())
                                .setSystemDate(LocalDateTime.now());

                        baseEntityStatusService.update(baseEntityStatus);
                        //baseEntityLoadXmlService.updateBaseEntity(baseEntityJson, true);
                        baseEntityLoadXmlService.deleteBaseEntity(baseEntityJson.getId());

                        logger.info("Завершена обработка сущности после одобрения, время {} {}", (System.currentTimeMillis() - startTime), baseEntityApplied);


                    } catch (BrmsException be) {

                        String entityText = BaseEntityOutput.getEntityAsString(mockEntity, true);
                        // ошибка бизнес правил: заливаем в базу все ошибки по сущности
                        for (String errorText : be.getErrorMessages()) {
                            String stackTrace = null;
                            if (errorText.contains("----")) {
                                stackTrace = errorText.substring(errorText.indexOf("-----"));
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
                            baseEntityLoadXmlService.updateBaseEntity(baseEntityJson, true);
                        }

                        logger.error("Ошибка проверки сущности на бизнес правила {}", mockEntity);

                    } catch (UsciException ue) {

                        BaseEntityStatus baseEntityStatus = new BaseEntityStatus()
                                .setEntityId(mockEntity.getId())
                                .setMetaClassId(mockEntity.getMetaClass().getId())
                                .setBatchId(mockEntity.getBatchId())
                                .setIndex(mockEntity.getBatchIndex())
                                .setEntityText(BaseEntityOutput.getEntityAsString(mockEntity, true))
                                .setStatus(EntityStatusType.ERROR)
                                .setOperation(mockEntity.getOperation())
                                .setErrorMessage(ue.getMessage())
                                .setErrorCode(ue.getErrorCode())
                                .setExceptionTrace(ExceptionUtils.getStackTrace(ue))
                                .setSystemDate(LocalDateTime.now());

                        baseEntityStatusService.insert(baseEntityStatus);
                        baseEntityLoadXmlService.updateBaseEntity(baseEntityJson, true);

                        logger.error("Ошибка обработки сущности {} ", mockEntity);

                    } catch (Exception e) {
                        String errorText = null;
                        if (e instanceof DuplicateKeyException) {
                            errorText = "Ошибка дублирования сущности";
                        } else if (e instanceof UncategorizedSQLException) {
                            errorText = "Ошибка БД";
                        }
                        // прочие ошибки (по БД, программные, разработчика) ловим здесь
                        BaseEntityStatus baseEntityStatus = new BaseEntityStatus()
                                .setEntityId(mockEntity.getId())
                                .setMetaClassId(mockEntity.getMetaClass().getId())
                                .setBatchId(mockEntity.getBatchId())
                                .setIndex(mockEntity.getBatchIndex())
                                .setEntityText(BaseEntityOutput.getEntityAsString(mockEntity, true))
                                .setStatus(EntityStatusType.ERROR)
                                .setOperation(mockEntity.getOperation())
                                .setErrorMessage((e instanceof DuplicateKeyException || e instanceof UncategorizedSQLException) ? errorText : e.getMessage())
                                .setExceptionTrace(ExceptionUtils.getStackTrace(e))
                                .setSystemDate(LocalDateTime.now());

                        baseEntityStatusService.insert(baseEntityStatus);
                        baseEntityLoadXmlService.updateBaseEntity(baseEntityJson, true);

                        logger.error("Ошибка обработки сущности {} ", mockEntity);

                    }
                } else if (baseEntityJson.isPreDeclined()){

                    BaseEntityStatus baseEntityStatus = new BaseEntityStatus()
                            .setEntityId(baseEntityJson.getEntityId())
                            .setApprovedEntityId(baseEntityJson.getEntityId())
                            .setBatchId(batchId)
                            .setStatus(EntityStatusType.ERROR)
                            .setErrorMessage("Отклонено после одобрения")
                            .setSystemDate(LocalDateTime.now());

                    baseEntityStatusService.update(baseEntityStatus);
                    baseEntityLoadXmlService.updateBaseEntity(baseEntityJson, false);

                    logger.error("Сущность отклонена после одобрения {} ", baseEntityJson);
                }
            });
        }
    }
}
