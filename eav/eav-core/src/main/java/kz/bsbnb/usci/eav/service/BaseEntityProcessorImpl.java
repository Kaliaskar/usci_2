package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.brms.RulesSingleton;
import kz.bsbnb.usci.brms.exception.BrmsException;
import kz.bsbnb.usci.brms.model.RulePackage;
import kz.bsbnb.usci.brms.service.PackageService;
import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseSet;
import kz.bsbnb.usci.eav.model.base.BaseValue;
import kz.bsbnb.usci.eav.model.meta.MetaAttribute;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.eav.model.meta.MetaType;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.client.ConfigClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static kz.bsbnb.usci.model.Constants.NBK_AS_RESPONDENT_ID;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

@Service
public class BaseEntityProcessorImpl implements BaseEntityProcessor {
    private static final Logger logger = LoggerFactory.getLogger(BaseEntityProcessorImpl.class);
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    private RulesSingleton rulesSingleton;

    private final PackageService packageService;
    private final BaseEntityDateSerivce baseEntityDateSerivce;
    private final BaseEntityHubService baseEntityHubService;
    private final BaseEntityStoreService baseEntityStoreService;
    private final ProductService productService;

    @Autowired
    private ConfigClient configClient;

    public BaseEntityProcessorImpl(BaseEntityHubService baseEntityHubService,
                                   BaseEntityStoreService baseEntityStoreService,
                                   NamedParameterJdbcTemplate npJdbcTemplate,
                                   JdbcTemplate jdbcTemplate,
                                   PackageService packageService,
                                   BaseEntityDateSerivce baseEntityDateSerivce, ProductService productService) {
        this.baseEntityHubService = baseEntityHubService;
        this.baseEntityStoreService = baseEntityStoreService;
        this.npJdbcTemplate = npJdbcTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.packageService = packageService;
        this.baseEntityDateSerivce = baseEntityDateSerivce;
        this.productService = productService;
    }

    @Override
    public BaseEntity processBaseEntity(final BaseEntity baseEntitySaving) {
        BaseEntityHolder baseEntityHolder = new BaseEntityHolder();

        BaseEntity baseEntityPrepared = baseEntitySaving.clone();
        BaseEntity baseEntityApplied;

        // все сущности кроме справочников должны иметь респондента
        if (!baseEntitySaving.getMetaClass().isDictionary() && Objects.equals(baseEntitySaving.getRespondentId(), NBK_AS_RESPONDENT_ID))
            throw new UsciException("Кредитор установлен не правильно");

        prepareBaseEntity(baseEntityPrepared, baseEntitySaving.getRespondentId());
        // baseEntitySaving не модифицируется потому что baseEntityPrepared его клон
        // чтобы идентифицировать сущность в протоколе мы присваиваем ей id после метода prepareBaseEntity
        baseEntitySaving.setId(baseEntityPrepared.getId());

        Long productId = null;
        Long receiptDateMillis = null;
        Long reportDateMillis = null;
        boolean isMaintenance;
        if (!baseEntityPrepared.isApproved()){
            productId = baseEntityPrepared.getMetaClass().getProductId();
            receiptDateMillis = baseEntityPrepared.getBatchReceiptDate().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            reportDateMillis = baseEntityPrepared.getReportDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        baseEntityPrepared.setProductId(productId);

        //baseEntityPrepared.setMaintenance(baseEntitySaving.isMaintenance());

        if (baseEntityPrepared.getOperation() != null) {
            switch (baseEntityPrepared.getOperation()) {
                case INSERT:
                    checkForRules(baseEntityPrepared, "_parser");
                    if (baseEntityPrepared.getId() != null)
                        throw new UsciException(String.format("Сущнось c id = %s найдена в базе. Вставка не произведена", baseEntityPrepared.getId()));

                    if (!baseEntityPrepared.isApproved()){
                        LocalDateTime localDateTimeJuly = LocalDateTime.of(2019,6,28,0,0,0);
                        Long dateTimeMillisJuly = localDateTimeJuly.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        LocalDateTime localDateTimeFeb = LocalDateTime.of(2020,2,1,23,59,0);
                        Long dateTimeMillisFeb = localDateTimeFeb.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        if (!(reportDateMillis.compareTo(dateTimeMillisJuly) >= 0) && !(reportDateMillis.compareTo(dateTimeMillisFeb) <= 0)) {
                            isMaintenance = productService.checkForApprove(productId, receiptDateMillis, reportDateMillis);
                            baseEntityPrepared.setMaintenance(isMaintenance);
                        }
                    }
                    baseEntityApplied = baseEntityStoreService.processBaseEntity(baseEntityPrepared, null, baseEntityHolder);
                    checkForRules(baseEntityApplied, "_process");
                    if (!baseEntityPrepared.isApproved()&& !baseEntityPrepared.isMaintenance() && baseEntityApplied.isMaintenance())
                        baseEntityPrepared.setMaintenance(true);
                    baseEntityStoreService.storeInDb(baseEntityHolder, baseEntityPrepared);

                    break;
                case UPDATE:
                    checkForRules(baseEntityPrepared, "_parser");
                    Objects.requireNonNull(baseEntityPrepared.getId(), "Запись не найдена в базе. Обновление не выполнено");

                    if (!baseEntityPrepared.isApproved()){
                        LocalDateTime localDateTimeJuly = LocalDateTime.of(2019,6,28,0,0,0);
                        Long dateTimeMillisJuly = localDateTimeJuly.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        LocalDateTime localDateTimeFeb = LocalDateTime.of(2020,2,1,23,59,0);
                        Long dateTimeMillisFeb = localDateTimeFeb.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        if (!(reportDateMillis.compareTo(dateTimeMillisJuly) >= 0) && !(reportDateMillis.compareTo(dateTimeMillisFeb) <= 0)) {
                            isMaintenance = productService.checkForApprove(productId, receiptDateMillis, reportDateMillis);
                            baseEntityPrepared.setMaintenance(isMaintenance);
                        }
                    }
                    baseEntityApplied = baseEntityStoreService.processBaseEntity(baseEntityPrepared, null, baseEntityHolder);
                    checkForRules(baseEntityApplied, "_process");
                    if (!baseEntityPrepared.isApproved()&& !baseEntityPrepared.isMaintenance() && baseEntityApplied.isMaintenance())
                        baseEntityPrepared.setMaintenance(true);
                    baseEntityStoreService.storeInDb(baseEntityHolder, baseEntityPrepared);

                    break;
                case DELETE:
                    //if (!productId.equals(10L)) {
                    if (!baseEntityPrepared.isApproved()){
                        LocalDate minReportDate = baseEntityDateSerivce.getMinReportDate(baseEntityPrepared);
                        Long minReportDateMillis = minReportDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        isMaintenance = productService.checkForApprove(productId, receiptDateMillis, minReportDateMillis);
                        baseEntityPrepared.setMaintenance(isMaintenance);
                    }
                    baseEntityApplied = baseEntityStoreService.markBaseEntityAsDeleted(baseEntityPrepared, baseEntityHolder);
                    /*} else {
                        throw new UsciException("Удаление сущности временно недоступно");
                    }*/
                    break;
                case CLOSE:
                    if (!baseEntityPrepared.isApproved()){
                        LocalDateTime localDateTimeJuly = LocalDateTime.of(2019,6,28,0,0,0);
                        Long dateTimeMillisJuly = localDateTimeJuly.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        LocalDateTime localDateTimeFeb = LocalDateTime.of(2020,2,1,23,59,0);
                        Long dateTimeMillisFeb = localDateTimeFeb.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        if (!(reportDateMillis.compareTo(dateTimeMillisJuly) >= 0) && !(reportDateMillis.compareTo(dateTimeMillisFeb) <= 0)) {
                            isMaintenance = productService.checkForApprove(productId, receiptDateMillis, reportDateMillis);
                            baseEntityPrepared.setMaintenance(isMaintenance);
                        }
                    }
                    checkForRules(baseEntityPrepared, "_parser");
                    baseEntityApplied = baseEntityStoreService.closeBaseEntity(baseEntityPrepared, baseEntityHolder);
                    break;
                case OPEN:
                    if (!baseEntityPrepared.isApproved()){
                        LocalDateTime localDateTimeJuly = LocalDateTime.of(2019,6,28,0,0,0);
                        Long dateTimeMillisJuly = localDateTimeJuly.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        LocalDateTime localDateTimeFeb = LocalDateTime.of(2020,2,1,23,59,0);
                        Long dateTimeMillisFeb = localDateTimeFeb.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        if (!(reportDateMillis.compareTo(dateTimeMillisJuly) >= 0) && !(reportDateMillis.compareTo(dateTimeMillisFeb) <= 0)) {
                            isMaintenance = productService.checkForApprove(productId, receiptDateMillis, reportDateMillis);
                            baseEntityPrepared.setMaintenance(isMaintenance);
                        }
                    }
                    checkForRules(baseEntityPrepared, "_parser");
                    baseEntityApplied = baseEntityStoreService.openBaseEntity(baseEntityPrepared, baseEntityHolder);
                    break;
                default:
                    throw new UsciException(String.format("Операция %s не поддерживается", baseEntityPrepared.getOperation()));
            }
        }
        else {
            checkForRules(baseEntityPrepared, "_parser");
            if (!baseEntityPrepared.isApproved()){
                LocalDateTime localDateTimeJuly = LocalDateTime.of(2019,6,28,0,0,0);
                Long dateTimeMillisJuly = localDateTimeJuly.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                LocalDateTime localDateTimeFeb = LocalDateTime.of(2020,2,1,23,59,0);
                Long dateTimeMillisFeb = localDateTimeFeb.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                if (!(reportDateMillis.compareTo(dateTimeMillisJuly) >= 0) && !(reportDateMillis.compareTo(dateTimeMillisFeb) <= 0)) {
                    isMaintenance = productService.checkForApprove(productId, receiptDateMillis, reportDateMillis);
                    baseEntityPrepared.setMaintenance(isMaintenance);
                }
            }
            baseEntityApplied = baseEntityStoreService.processBaseEntity(baseEntityPrepared, null, baseEntityHolder);
            checkForRules(baseEntityApplied, "_process");
            if (!baseEntityPrepared.isApproved()&& !baseEntityPrepared.isMaintenance() && baseEntityApplied.isMaintenance())
                baseEntityPrepared.setMaintenance(true);
            baseEntityStoreService.storeInDb(baseEntityHolder, baseEntityPrepared);
        }

        return baseEntityApplied;
    }

    private void checkForRules(BaseEntity baseEntity, String packageType) {
        List<RulePackage> rulePackageList = packageService.getAllPackages();
        Set<String> metaRules = new HashSet<>();
        for (RulePackage rulePackage : rulePackageList) {
            String temp = rulePackage.getName().substring(0, rulePackage.getName().lastIndexOf("_"));
            metaRules.add(temp);
        }

        if (metaRules.contains(baseEntity.getMetaClass().getClassName())) {
            try {
                rulesSingleton.runRules(baseEntity, baseEntity.getMetaClass().getClassName() + packageType, baseEntity.getReportDate());
            } catch (Exception e) {
                logger.error("Не могу применить бизнес правила", e);
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                String excTrace;
                try {
                    e.printStackTrace(pw);
                    excTrace = sw.toString();
                } finally {
                    pw.close();
                }
                if (excTrace != null && excTrace.length() > 4000)
                    excTrace = excTrace.substring(0, 3200);
                throw new BrmsException(Collections.singletonList("Не могу применить бизнес правила: " + e.toString() + "-----" + excTrace));
            }

            if (baseEntity.getValidationErrors().size() > 0)
                throw new BrmsException(baseEntity.getValidationErrors());
        }
    }

    /**
     * для бизнес правил чтобы не отправляли лишние параметры
      */
    @Override
    public BaseEntity prepareBaseEntity(final BaseEntity baseEntity, Long respondentId) {
        return prepareBaseEntity(baseEntity, respondentId, null);
    }

    private BaseEntity prepareBaseEntity(final BaseEntity baseEntity, Long respondentId, MetaAttribute attribute) {
        MetaClass metaClass = baseEntity.getMetaClass();

        // если родителем является справочник то все поддерево тоже является нулевым кредитором (НБК), обратное не верно
        // пример: a) у справочника может быть сущность документ то и он имеет признак нулевого кредитора
        if (metaClass.isDictionary()) {
            //List<Long> products = productService.getProductIdsByMetaClassId(metaClass.getId());

            if (metaClass.getProductId().equals(9L)) {
                respondentId = NBK_AS_RESPONDENT_ID;
            }
        }

        baseEntity.setRespondentId(respondentId);

        // блок кода обрабатывает только комплексные атрибуты
        // отрабатывает рекурсивно потому что сущность может зависеть от других сущностей
        // найденный id сущности служит ключом у другой сущности
        for (String attrName : baseEntity.getAttributeNames()) {
            MetaAttribute metaAttribute = metaClass.getMetaAttribute(attrName);
            MetaType metaType = metaAttribute.getMetaType();
            BaseValue baseValue = baseEntity.getBaseValue(attrName);

            if (!metaAttribute.isKey() && !metaAttribute.isNullableKey())
                continue;

            if (metaType.isComplex() && baseValue.getValue() != null) {
                if (metaType.isSet()) {
                    BaseSet childBaseSet = (BaseSet) baseValue.getValue();
                    for (BaseValue childBaseValue : childBaseSet.getValues()) {
                        BaseEntity childBaseEntity = (BaseEntity) childBaseValue.getValue();
                        childBaseEntity.setRespondentId(respondentId);
                        childBaseEntity.setParentInfo(baseEntity, metaAttribute);

                        if (childBaseEntity.getValueCount() != 0)
                            prepareBaseEntity(childBaseEntity, respondentId, metaAttribute);
                    }
                }
                else {
                    BaseEntity childBaseEntity = (BaseEntity) baseValue.getValue();
                    childBaseEntity.setRespondentId(respondentId);
                    childBaseEntity.setParentInfo(baseEntity, metaAttribute);

                    if (childBaseEntity.getValueCount() != 0)
                        prepareBaseEntity(childBaseEntity, respondentId, metaAttribute);

                    if (metaAttribute.isReference() && childBaseEntity.getId() == null)
                        throw new UsciException(String.format("В базе нет данных для записи %s до отчетной даты(включительно): %s",
                                childBaseEntity, childBaseEntity.getReportDate()));
                }
            }
        }

        // нахожу сущность из хаба если она идентифицируемая
        if (metaClass.isSearchable() && baseEntity.getId() == null) {
            baseEntity.setId(baseEntityHubService.find(baseEntity).orElse(null));
        }

        // заполним id у сущностей если они не идентифицируемые и которые не являются ссылкой
        if (baseEntity.getId() != null && (attribute == null || !attribute.isReference()))
            setNonSearchableBeIds(baseEntity);

        for (String attrName : baseEntity.getAttributeNames()) {
            MetaAttribute metaAttribute = metaClass.getMetaAttribute(attrName);
            MetaType metaType = metaAttribute.getMetaType();
            BaseValue baseValue = baseEntity.getBaseValue(attrName);

            if (metaAttribute.isKey() || metaAttribute.isNullableKey())
                continue;

            if (metaType.isComplex() && baseValue.getValue() != null) {
                if (metaType.isSet()) {
                    BaseSet childBaseSet = (BaseSet) baseValue.getValue();
                    for (BaseValue childBaseValue : childBaseSet.getValues()) {
                        BaseEntity childBaseEntity = (BaseEntity) childBaseValue.getValue();
                        childBaseEntity.setRespondentId(respondentId);

                        // заполняем у дочерних узлов(кроме ключевых) признак родителя
                        childBaseEntity.setParentInfo(baseEntity, metaAttribute);

                        if (childBaseEntity.getValueCount() != 0)
                            prepareBaseEntity(childBaseEntity, respondentId, metaAttribute);
                    }
                }
                else {
                    BaseEntity childBaseEntity = (BaseEntity) baseValue.getValue();
                    childBaseEntity.setRespondentId(respondentId);
                    childBaseEntity.setParentInfo(baseEntity, metaAttribute);

                    if (childBaseEntity.getValueCount() != 0)
                        prepareBaseEntity(childBaseEntity, respondentId, metaAttribute);
                    if (metaAttribute.isReference() && childBaseEntity.getId() == null)
                        throw new UsciException(String.format("В базе нет данных для записи %s до отчетной даты(включительно): %s",
                                childBaseEntity, childBaseEntity.getReportDate()));
                }
            }
        }

        return baseEntity;
    }

    /**
     * метод заполняет id сущностей searchable=false
     * как например head, organization_info, person_info, change
     * чтобы переиспользовать существующий id иначе класс сгенерирует новый id и получится так что
     * сущность потеряет связь со старыми записями
     */
    private void setNonSearchableBeIds(BaseEntity baseEntity) {
        Objects.requireNonNull(baseEntity.getId(), "Сущность должна иметь идентификатор");

        MetaClass metaClass = baseEntity.getMetaClass();

        for (String attrName: baseEntity.getAttributeNames()) {
            MetaAttribute metaAttribute = metaClass.getMetaAttribute(attrName);
            MetaType metaType = metaAttribute.getMetaType();

            if (!metaType.isComplex() || metaAttribute.isReference() || metaType.isSet())
                continue;

            MetaClass childMetaClass = (MetaClass) metaType;
            if (childMetaClass.isSearchable())
                continue;

            BaseValue baseValue = baseEntity.getBaseValue(attrName);
            if (baseValue == null || baseValue.getValue() == null)
                continue;

            BaseEntity childBaseEntity = (BaseEntity) baseValue.getValue();

            String query = "select distinct " + metaAttribute.getColumnName() + "\n" +
                           "  from EAV_DATA." + metaClass.getTableName() + "\n" +
                           " where CREDITOR_ID = :CREDITOR_ID\n" +
                           "   and ENTITY_ID = :ENTITY_ID\n" +
                           "   and " + metaAttribute.getColumnName() + " is not null";

            try {
                Long id = npJdbcTemplate.queryForObject(query, new MapSqlParameterSource()
                        .addValue("CREDITOR_ID", baseEntity.getRespondentId())
                        .addValue("ENTITY_ID", baseEntity.getId()), Long.class);

                childBaseEntity.setId(id);
            } catch (EmptyResultDataAccessException e) {
                // значит ранее по сущности не было записей
            }
        }
    }

}
