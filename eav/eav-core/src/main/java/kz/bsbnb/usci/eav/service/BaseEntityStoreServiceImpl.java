package kz.bsbnb.usci.eav.service;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import kz.bsbnb.usci.eav.dao.BaseEntityDateDao;
import kz.bsbnb.usci.eav.exception.ClosedBaseEntityException;
import kz.bsbnb.usci.eav.exception.UndefinedBaseEntityException;
import kz.bsbnb.usci.eav.model.Constants;
import kz.bsbnb.usci.eav.model.base.*;
import kz.bsbnb.usci.eav.model.core.BaseEntityDate;
import kz.bsbnb.usci.eav.model.core.BaseEntityUtils;
import kz.bsbnb.usci.eav.model.core.EavHub;
import kz.bsbnb.usci.eav.model.core.EavSchema;
import kz.bsbnb.usci.eav.model.meta.*;
import kz.bsbnb.usci.eav.model.meta.json.EntityExtJsTreeJson;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import oracle.jdbc.driver.OracleConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

@Service
public class BaseEntityStoreServiceImpl implements BaseEntityStoreService {
    private static final Logger logger = LoggerFactory.getLogger(BaseEntityStoreServiceImpl.class);

    private final BaseEntityHubService baseEntityHubService;
    private final EntityService entityService;
    private final BaseEntityLoadService baseEntityLoadService;
    private final ProductService productService;
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final BaseEntityDateDao baseEntityDateDao;
    private final BaseEntityDateSerivce baseEntityDateSerivce;
    private final SimpleJdbcInsert baseEntityMaintenceInsert;


    public BaseEntityStoreServiceImpl(NamedParameterJdbcTemplate npJdbcTemplate,
                                      JdbcTemplate jdbcTemplate,
                                      BaseEntityHubService baseEntityHubService,
                                      EntityService entityService,
                                      BaseEntityLoadService baseEntityLoadService,
                                      ProductService productService, BaseEntityDateDao baseEntityDateDao,
                                      BaseEntityDateSerivce baseEntityDateSerivce) {
        this.npJdbcTemplate = npJdbcTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.baseEntityHubService = baseEntityHubService;
        this.entityService = entityService;
        this.baseEntityLoadService = baseEntityLoadService;
        this.productService = productService;
        this.baseEntityDateDao = baseEntityDateDao;
        this.baseEntityDateSerivce = baseEntityDateSerivce;

        this.baseEntityMaintenceInsert =  new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("EAV_XML")
                .withTableName("EAV_ENTITY_MAINTENANCE")
                .usingGeneratedKeyColumns("ID");
    }

    /**
     * метод диспетчер отвечает за обработку сущности и выполняет:
     * новые сущности непосредственно отправляет на обработку
     * если сущность существует то предварительно подгружает ее из БД и потом отправляет на обработку
     * предварительно загружать сущность из БД нужно чтобы делать сверку с данными которые прислали
     * алгоритм обработки существующей сущности см. в методе processExistingBaseEntity
     * */
    @Override
    public BaseEntity processBaseEntity(final BaseEntity baseEntitySaving, BaseEntity baseEntityLoaded, final BaseEntityHolder baseEntityHolder) {
        MetaClass metaClass = baseEntitySaving.getMetaClass();
        BaseEntity baseEntityApplied;
        // udalil  uslovie !metaClass.isSearchable
        // byla problema byla s change
        if (baseEntitySaving.getId() == null)
            baseEntityApplied = processNewBaseEntity(baseEntitySaving, baseEntityHolder);
        else {
            if (baseEntityLoaded == null || baseEntitySaving.getReportDate().compareTo(baseEntityLoaded.getReportDate()) < 0) {
                baseEntityLoaded = baseEntityLoadService.loadBaseEntity(baseEntitySaving, baseEntitySaving.getReportDate());
                if (baseEntityLoaded != null && baseEntityLoaded.isClosed())
                    throw new ClosedBaseEntityException(baseEntityLoaded);
            }

            if (baseEntityLoaded != null) {
                baseEntityApplied = processExistingBaseEntity(baseEntitySaving, baseEntityLoaded, baseEntityHolder);
            } else {
                // оперативные данные имеют диентификатор посему попадают сюда как новые записи
                baseEntityApplied = processNewBaseEntity(baseEntitySaving, baseEntityHolder);
            }
        }

        return baseEntityApplied;
    }

    private void handleBaseEntityReceived(BaseEntity baseEntityApplied, BaseEntity baseEntitySaving, BaseEntity baseEntityLoaded) {
        MetaClass metaClass = baseEntitySaving.getMetaClass();

        // заполняем поля:
        // - X_RECEIVED - признак получения из батча (даже обнуления не важно главное есть в батче)
        baseEntityApplied.setReceived(new HashSet<>());
        for (MetaAttribute metaAttribute : metaClass.getAttributes()) {
            BaseValue baseValue = baseEntitySaving.getBaseValue(metaAttribute.getName());

            // если же ранее уже присылали данные за отчетный период то проставляем признак истина
            if (baseValue != null || (baseEntityLoaded != null &&
                    baseEntityLoaded.getReportDate().equals(baseEntitySaving.getReportDate()) &&
                    baseEntityLoaded.getReceived().contains(metaAttribute.getName())))
                baseEntityApplied.getReceived().add(metaAttribute.getName());

            if (baseValue != null)
                baseEntitySaving.getReceived().add(metaAttribute.getName());
        }
    }

    /**
     * метод занимается обработкой новых сущностей (то есть тех которых нет в БД):
     * - создаем новую сущность из реквизитов сущности из парсера.
     * - комплексные сеты: создаем новый сет для манипуляций с ним
     *   каждую сущность сета отправляем на обработку, затем добавляем его в новый сет
     *  - примитивный сет: создаем новый сет из реквизитов сета из парсера, затем мигрируем значения из сета от парсера в новый сет
     * - комплексный скаляр: отправляем атрибут на обработку, затем ложим его в новую сущность
     * - примитивный скаляр: просто ложим значение в новую сущность
     * */
    private BaseEntity processNewBaseEntity(BaseEntity baseEntitySaving, BaseEntityHolder baseEntityHolder) {
        BaseEntity foundProcessedBaseEntity = baseEntityHolder.getProcessedBaseEntity(baseEntitySaving);
        if (foundProcessedBaseEntity != null) {
            addBaseEntitySavingAppliedPair(baseEntitySaving, foundProcessedBaseEntity, baseEntityHolder);
            return  foundProcessedBaseEntity;
        }

        MetaClass metaClass = baseEntitySaving.getMetaClass();

        BaseEntity baseEntityApplied = new BaseEntity(baseEntitySaving);

        for (String attrName : baseEntitySaving.getAttributeNames()) {
            BaseValue baseValueSaving = baseEntitySaving.getBaseValue(attrName);

            // пропускает закрытые теги на новые сущности <tag/>
            if (baseValueSaving.getValue() == null)
                continue;

            MetaAttribute metaAttribute = baseEntitySaving.getMetaAttribute(attrName);
            MetaType metaType = metaAttribute.getMetaType();

            if (metaType.isComplex()) {
                if (metaType.isSet()) {
                    MetaSet childMetaSet = (MetaSet) metaType;
                    BaseSet childBaseSet = (BaseSet) baseValueSaving.getValue();

                    BaseSet childBaseSetApplied = new BaseSet(childMetaSet.getMetaType());
                    for (BaseValue childBaseValue : childBaseSet.getValues()) {
                        BaseEntity childBaseEntity = (BaseEntity) childBaseValue.getValue();
                        childBaseEntity.setProductId(baseEntitySaving.getProductId());
                        childBaseEntity.setBatchReceiptDate(baseEntitySaving.getBatchReceiptDate());

                        if (metaAttribute.isReference() && childBaseEntity.getId() == null)
                            throw new UsciException(String.format("В базе нет данных для записи %s до отчетной даты(включительно): %s",
                                    childBaseEntity, childBaseEntity.getReportDate()));

                        if (metaAttribute.isReference()) {
                            BaseEntity childBaseEntityApplied = baseEntityLoadService.loadByMaxReportDate(childBaseEntity, childBaseEntity.getReportDate());
                            if (childBaseEntityApplied == null)
                                throw new UsciException(String.format("В базе нет данных для записи %s до отчетной даты(включительно): %s",
                                        childBaseEntity, childBaseEntity.getReportDate()));

                            if (childBaseEntityApplied.isClosed())
                                throw new ClosedBaseEntityException(childBaseEntityApplied);
                            childBaseSetApplied.put(new BaseValue(childBaseEntityApplied));
                        } else {
                            BaseEntity childBaseEntityApplied = processBaseEntity(childBaseEntity, null, baseEntityHolder);
                            if (childBaseEntityApplied.isMaintenance())
                                baseEntityApplied.setMaintenance(true);
                            childBaseSetApplied.put(new BaseValue(childBaseEntityApplied));
                        }

                    }

                    baseEntityApplied.put(metaAttribute.getName(), new BaseValue(childBaseSetApplied));
                } else {
                    if (metaAttribute.isReference()) {
                        BaseEntity childBaseEntity = (BaseEntity) baseValueSaving.getValue();

                        // все reference сущности должны иметь id который должен присваивается в методе prepare
                        // если код зашел в данный if то это означает что методы отработали не правильно или данные переданы не корректные
                        // сущность должна иметь значения (передаются парсером) для его идентификаций
                        if (childBaseEntity.getValueCount() == 0)
                            throw new UsciException(String.format("Комплексный элемент не содержит внутренних элементов %s", childBaseEntity));
                        else if (childBaseEntity.getId() == null)
                            throw new UndefinedBaseEntityException(childBaseEntity);

                        BaseEntity childBaseEntityRef = baseEntityLoadService.loadByMaxReportDate(childBaseEntity, childBaseEntity.getReportDate());
                        if (childBaseEntityRef == null)
                            throw new UsciException(String.format("В базе нет данных для записи %s до отчетной даты(включительно): %s",
                                    childBaseEntity, childBaseEntity.getReportDate()));

                        if (childBaseEntityRef.isClosed())
                            throw new ClosedBaseEntityException(childBaseEntityRef);

                        baseEntityApplied.put(metaAttribute.getName(), new BaseValue(childBaseEntityRef));
                    } else {
                        BaseEntity childBaseEntity = (BaseEntity) baseValueSaving.getValue();
                        childBaseEntity.setProductId(baseEntitySaving.getProductId());
                        childBaseEntity.setBatchReceiptDate(baseEntitySaving.getBatchReceiptDate());
                        BaseEntity childBaseEntityApplied = processBaseEntity(childBaseEntity, null, baseEntityHolder);
                        if (childBaseEntityApplied.isMaintenance())
                            baseEntityApplied.setMaintenance(true);
                        baseEntityApplied.put(metaAttribute.getName(), new BaseValue(childBaseEntityApplied));
                    }
                }
            } else {
                if (metaType.isSet()) {
                    MetaSet childMetaSet = (MetaSet) metaType;
                    BaseSet childBaseSet = (BaseSet) baseValueSaving.getValue();

                    BaseSet childBaseSetApplied = new BaseSet(childMetaSet.getMetaType());
                    for (BaseValue childBaseValue : childBaseSet.getValues())
                        childBaseSetApplied.put(new BaseValue(childBaseValue.getValue()));

                    baseEntityApplied.put(metaAttribute.getName(), new BaseValue(childBaseSetApplied));
                } else {
                    baseEntityApplied.put(metaAttribute.getName(), baseValueSaving);
                }
            }
        }

        handleBaseEntityReceived(baseEntityApplied, baseEntitySaving, null);

        if (baseEntityApplied.getId() == null)
            baseEntityHolder.putAsNewBaseEntity(baseEntityApplied);
        // оперативные данные переиспользуют один и тот же id в течение всего периода
        // так что помечаем их как новые записи а не как новые сущности
        else
            baseEntityHolder.putAsNewBaseEntityEntry(baseEntityApplied);

        // зафиксируем что за отчетную дату пришла сущность
        if (!metaClass.isOperational())
            baseEntityHolder.putAsNewBaseEntityDate(new BaseEntityDate(baseEntityApplied));

        baseEntityHolder.saveBaseEntitySavingAppliedPair(baseEntitySaving, baseEntityApplied);
        baseEntityHolder.putAsProcessedBaseEntity(baseEntityApplied);
        return baseEntityApplied;
    }

    private BaseEntity processExistingBaseEntity(BaseEntity baseEntitySaving, BaseEntity baseEntityLoaded, BaseEntityHolder baseEntityHolder) {
        Objects.requireNonNull(baseEntityLoaded, "Загруженная сущность не инициализирована");
        Objects.requireNonNull(baseEntityLoaded.getId(), "По загруженной сущности отсутствует id");

        BaseEntity foundProcessedBaseEntity = baseEntityHolder.getProcessedBaseEntity(baseEntitySaving);
        if (foundProcessedBaseEntity != null) {
            addBaseEntitySavingAppliedPair(baseEntitySaving, foundProcessedBaseEntity, baseEntityHolder);
            return foundProcessedBaseEntity;
        }

        boolean optEntityIsPresent = false;
        Set<MetaAttribute> changedKeys = new HashSet<>();
        Set<MetaAttribute> changedKeysOnSet = new HashSet<>();

        LocalDate reportDateSaving = baseEntitySaving.getReportDate();
        LocalDate reportDateLoaded = baseEntityLoaded.getReportDate();

        int compareDates = reportDateSaving.compareTo(reportDateLoaded);

        MetaClass metaClass = baseEntitySaving.getMetaClass();

        if (metaClass.isOperational() && compareDates != 0)
            throw new UsciException("Оперативные сущности должны действовать только на отчетную дату");

        // устанавливает id для searchable=false
        if (baseEntitySaving.getId() == null && baseEntityLoaded.getId() != null) {
            if (metaClass.isSearchable())
                if (metaClass.isSearchable())
                    baseEntityHolder.putAsMigrationErrorsHub(baseEntitySaving);
                //throw new UsciException("Сущность не идентифицирована");

            baseEntitySaving.setId(baseEntityLoaded.getId());
        }

        // создаем новый обьект чтобы потом все изменения выполнять над ним
        BaseEntity baseEntityApplied = new BaseEntity(baseEntitySaving);

        for (String attrName : metaClass.getAttributeNames()) {
            BaseValue baseValueSaving = baseEntitySaving.getBaseValue(attrName);
            BaseValue baseValueLoaded = baseEntityLoaded.getBaseValue(attrName);

            // если по атрибуту не было и нет значения то и делать дальше нечего, просто игнорируем
            if (baseValueLoaded == null && baseValueSaving == null)
                continue;

            final MetaAttribute metaAttribute = metaClass.getMetaAttribute(attrName);
            final MetaType metaType = metaAttribute.getMetaType();

            // случай когда если атрибут не пришел в xml но у нас есть значение ранее подгруженное, смотрим на признак isNullify
            if (baseValueLoaded != null && baseValueSaving == null) {
                if (compareDates < 0 ) {//|| (metaAttribute.isNullify() && compareDates != 0 )) {
                    // добавлям пустое значение в атрибуты (см. алгоритм в processComplexBaseValue)
                    baseValueSaving = new BaseValue();
                    baseEntityApplied.put(attrName, baseValueSaving);

                } else
                // просто обогащаем атрибут ранее подгруженным значением
                baseEntityApplied.put(attrName, baseValueLoaded);
            }

            if (baseValueSaving == null)
                continue;

            if (metaType.isComplex()) {
                if (metaType.isSet()) {
                    boolean keyChangedOnSet = processComplexBaseSet(metaAttribute, baseEntityApplied, baseEntityLoaded, baseValueSaving, baseValueLoaded, baseEntityHolder);
                    if (keyChangedOnSet) {
                        //todo:временно
                        if (baseEntityApplied.getMetaClass().getId().equals(30L)) {
                            List<String> keysApplied = baseEntityHubService.getKeys(baseEntityApplied).values().stream()
                                    .flatMap(Collection::stream)
                                    .collect(toList());

                            List<String> keysLoaded = baseEntityHubService.getKeys(baseEntityLoaded).values().stream()
                                    .flatMap(Collection::stream)
                                    .collect(toList());

                            if (keysLoaded.size() > 0 && keysApplied.size() == keysLoaded.size()) {
                                baseEntityHolder.putAsAddBaseEntityHub(baseEntityApplied, keysLoaded);
                            }
                        } else {
                            List<String> keys = baseEntityHubService.getKeys(baseEntityLoaded).values().stream()
                                    .flatMap(Collection::stream)
                                    .collect(toList());
                            if (keys.size() > 0) {
                                baseEntityHolder.putAsAddBaseEntityHub(baseEntityApplied, keys);
                            }
                        }

                    }
                }
                else
                    processComplexBaseValue(metaAttribute, baseEntityApplied, baseEntityLoaded, baseValueSaving, baseValueLoaded, baseEntityHolder);
            } else {
                if (metaType.isSet())
                    processSimpleBaseSet(metaAttribute, baseEntityApplied, baseEntityLoaded, baseValueSaving, baseValueLoaded);
                else {
                    // помечаю какой именно ключевой атрибут изменился
                    boolean keyChanged = processSimpleBaseValue(metaAttribute, baseEntityApplied, baseValueSaving, baseValueLoaded);
                    if (keyChanged)
                        changedKeys.add(metaAttribute);
                }
            }
        }

        handleBaseEntityReceived(baseEntityApplied, baseEntitySaving, baseEntityLoaded);

        if (!metaClass.isOperational()) {
            // зафиксируем что за отчетную дату пришла сущность
            Optional<BaseEntityDate> optBaseEntityDate = baseEntityDateDao.find(baseEntityApplied);
            if (optBaseEntityDate.isPresent()) {
                // если уже есть запись за отчетную дату то проверим есть ли различия
                BaseEntityDate baseEntityDate = optBaseEntityDate.get();
                Set<String> newReceivedSet = BaseEntityUtils.getReceivedSet(baseEntitySaving);

                // если атрибут однажды пришел то он так и должен оставаться как пришел
                newReceivedSet.addAll(baseEntityDate.getReceived());

                if (!baseEntityDate.getReceived().equals(newReceivedSet)) {
                    baseEntityDate.setReceived(newReceivedSet);
                    baseEntityHolder.putAsUpdatedBaseEntityDate(baseEntityDate);
                }
            } else
                baseEntityHolder.putAsNewBaseEntityDate(new BaseEntityDate(baseEntityApplied));
        }

        // если есть изменения по ключам то необходимо зафиксировать в поле OLD_ENTITY_KEY для таблиц в схеме EAV_XML,
        // также апдейтнуть хаб так как ключи изменились
        // примечание: есть поддержка изменения только примитивных атрибутов, сеты и комплексные атрибуты не поддерживаются
        // потому что нет хороших тест кейсов чтобы их проверить, впрочем в самом ессп был заложено только примитивные атрибуты
        // также по причине нехватки тест кейсов поддерживается изменение только одной группы ключей
        boolean keyChanged = false;
        if (changedKeys.size() > 0) {
            Map<Short, List<String>> oldKeys = baseEntityHubService.getKeys(baseEntityLoaded);
            Map<Short, List<String>> newKeys = baseEntityHubService.getKeys(baseEntityApplied);

            if (oldKeys.size() == newKeys.size()) {
                String oldEntityKey;
                if (oldKeys.size() == 1 && oldKeys.values().size() == 1)
                    oldEntityKey = String.valueOf(oldKeys.values().stream().findFirst().get().get(0));
                else
                    throw new UsciException("Изменение множества ключей не поддерживается");

                String newEntityKey = String.valueOf(newKeys.values().stream().findFirst().get().get(0));

                // атрибуты сущности baseEntitySaving делаю чтобы отображали новые ключи чтобы видеть в схеме EAV_XML
                // потому что измененные ключи храняться в отдельном поле newBaseValue
                for (MetaAttribute key : changedKeys)
                    baseEntitySaving.put(key.getName(), baseEntityApplied.getBaseValue(key.getName()));

                // фиксирую изменение в хаб со старым и новым ключем
                EavHub updatedHub = new EavHub(baseEntityApplied, oldEntityKey, newEntityKey);
                baseEntityHolder.putAsUpdatedBaseEntityHub(updatedHub);

                baseEntitySaving.setOldEntityKey(oldEntityKey);
                keyChanged = true;
            }
            else {
                List<String> keys  = baseEntityHubService.getKeys(baseEntityLoaded).values().stream()
                        .flatMap(Collection::stream)
                        .collect(toList());
                baseEntityHolder.putAsAddBaseEntityHub(baseEntityApplied, keys);
            }
        }

        // сверяем отчетные даты сущности чтобы определить какую операцию проводить
        // если запись за отчетный период уже существует то делаем просто update, иначе по сущности добавляем новую историю
        // чтобы в холостую не выполнять операций в БД по сущности, сверяем имеющиеся атрибуты сущности из БД с атрибутами которые прислали;
        // если значения атрибутов фактический поменялись то помечаем сущность на обработку
        List<MetaAttribute> updatedAttributes = markBaseEntityChanges(baseEntityApplied, baseEntityLoaded);

        if (compareDates > 0) {
            if (!updatedAttributes.isEmpty())
                baseEntityHolder.putAsNewBaseEntityEntry(baseEntityApplied);
        }
            // если же не было измений и прислали данные задней датой то сдвигаем отчетную дату сущности
        else if (compareDates < 0 && !metaClass.isOperational() && updatedAttributes.isEmpty()) {
            baseEntityApplied.setOldReportDate(baseEntityLoaded.getReportDate());
            baseEntityHolder.putAsUpdateBaseEntityReportDate(baseEntityApplied);
            processBackMoveDateCheck(baseEntityApplied, baseEntityLoaded, updatedAttributes, baseEntityHolder);
        }
        else if (compareDates < 0 && !metaClass.isOperational() && !updatedAttributes.isEmpty()) {
            baseEntityHolder.putAsNewBaseEntityEntry(baseEntityApplied);
        }
        else if (!updatedAttributes.isEmpty() && compareDates == 0)
            baseEntityHolder.putAsUpdatedBaseEntityEntry(baseEntityApplied);

        baseEntityHolder.putAsProcessedBaseEntity(baseEntityApplied);

        // обработка когда идет загрузка задней датой
        if (!updatedAttributes.isEmpty() && !metaClass.isOperational())
            if(!keyChanged)
                processBackDate(baseEntityApplied, baseEntityLoaded, updatedAttributes, baseEntityHolder);

        return baseEntityApplied;
    }

    /**
     * обработка сущностей задней датой
     * заметки:
     * - baseEntityApplied сущность может как пойти на update или инсерт так что с ней нельзя проводит здесь операций
     * */
    private void processBackDate(BaseEntity baseEntityApplied, BaseEntity baseEntityLoaded,
                                 List<MetaAttribute> updatedAttributes, BaseEntityHolder baseEntityHolder) {
        MetaClass metaClass = baseEntityApplied.getMetaClass();

        // получаю все отчетные даты после даты батча
        // записи должны быть отсортированы по отчетной дате в возрастающем порядке
        List<BaseEntityDate> baseEntityDates = baseEntityDateDao.find(baseEntityApplied, baseEntityApplied.getReportDate());

        // если нет записей после отчетной даты батча то значит батчи грузят последовательно
        if (baseEntityDates.size() == 0)
            return;

        // подгружаю записи сущности за последующие отчетные даты от батча
        // подгружаю все записи сущности из БД от даты батча
        Map<LocalDate, BaseEntity> baseEntityEntriesDb = baseEntityLoadService.loadBaseEntityEntries(baseEntityApplied, baseEntityApplied.getReportDate())
                .stream().collect(Collectors.toMap(BaseEntity::getReportDate, o -> o));

        if (baseEntityEntriesDb.containsKey(baseEntityApplied.getReportDate()))
            baseEntityHolder.getNewBaseEntityEntries().remove(baseEntityApplied);

        // добавляю запись сущности в коллекцию для обогащения
        if (!baseEntityEntriesDb.containsKey(baseEntityLoaded.getReportDate()))
            baseEntityEntriesDb.put(baseEntityLoaded.getReportDate(), baseEntityLoaded);

        List<BaseEntity> processedBaseEntityEntries = new ArrayList<>();

        LocalDate existingReportDate = baseEntityLoaded.getReportDate();

        // все записи данной сущности от даты батча анализирую
        for (BaseEntityDate baseEntityDate : baseEntityDates) {
            LocalDate reportDate = baseEntityDate.getReportDate();

            // если в БД есть запись по сущности на дату то запоминаю ее чтобы в последующем находить запись
            if (baseEntityEntriesDb.containsKey(reportDate))
                existingReportDate = reportDate;

            // признак того присылали атрибут или нет
            Set<String> received = baseEntityDate.getReceived();

            // получаю непосредственно саму запись
            BaseEntity baseEntityEntry = baseEntityEntriesDb.getOrDefault(reportDate, baseEntityEntriesDb.get(existingReportDate)).clone();
            baseEntityEntry.setReportDate(reportDate);
            baseEntityEntry.setBatchId(baseEntityDate.getBatchId());

            for (Iterator<MetaAttribute> iterator = updatedAttributes.iterator(); iterator.hasNext(); ) {
                MetaAttribute metaAttribute = iterator.next();

                BaseValue baseValue = baseEntityApplied.getBaseValue(metaAttribute.getName());

                // если значение по атрибуту не присылали тогда заменяю его новым
                if (!received.contains(metaAttribute.getName()))
                    if (baseValue != null)
                    baseEntityEntry.put(metaAttribute.getName(), new BaseValue(baseValue.getValue()));
                // когда значение по атрибуту присылали то удаляю атрибут из списка отслеживаемых
                else
                    iterator.remove();
            }

            processedBaseEntityEntries.add(baseEntityEntry);

            // нет атрибутов которые нужно обогащать тогда выходим из цикла
            // необходимо делать выход из цикла именно здесь чтобы добавить запись для сверки дубликатов
            if (updatedAttributes.isEmpty())
                break;
        }

        // после обработки сверяем все записи в хронологическом порядке
        // если запись дублируется с предыдущей то помечаем ее на удаление
        Set<LocalDate> duplicates = new HashSet<>();


        for (int i = 0; i < processedBaseEntityEntries.size(); i++) {
            BaseEntity baseEntityEntryPrevious = (i == 0)? baseEntityApplied: processedBaseEntityEntries.get(i - 1);
            BaseEntity baseEntityEntryCurrent = processedBaseEntityEntries.get(i);

            boolean equalsByValue = true;
            for (MetaAttribute metaAttribute : metaClass.getAttributes()) {
                BaseValue childBaseValuePrevious = baseEntityEntryPrevious.getBaseValue(metaAttribute.getName());
                BaseValue childBaseValueCurrent = baseEntityEntryCurrent.getBaseValue(metaAttribute.getName());

                if (childBaseValuePrevious == null && childBaseValueCurrent == null)
                    continue;

                Object childBaseValuePreviousFact = childBaseValuePrevious != null? childBaseValuePrevious.getValue(): null;
                Object childBaseValueCurrentFact = childBaseValueCurrent != null? childBaseValueCurrent.getValue(): null;

                if (childBaseValuePreviousFact == null && childBaseValueCurrentFact == null)
                    continue;

                if ((childBaseValuePreviousFact == null || childBaseValueCurrentFact == null) ||
                        !childBaseValueCurrent.equalsByValue(childBaseValuePrevious)) {
                    equalsByValue = false;
                    break;
                }
            }

            if (equalsByValue)
                duplicates.add(baseEntityEntryCurrent.getReportDate());
        }

        for (BaseEntity processedBaseEntityEntry : processedBaseEntityEntries) {
            LocalDate reportDate = processedBaseEntityEntry.getReportDate();

            if (duplicates.contains(reportDate)) {
                // если запись содержится в БД и дублируется с предыдущей то удаляем ее
                if (baseEntityEntriesDb.containsKey(reportDate)) {
                    baseEntityHolder.putAsDeletedBaseEntityEntry(processedBaseEntityEntry);
                }
                continue;
            }

            // запись содержится в БД тогда делаем update
            if (baseEntityEntriesDb.containsKey(reportDate)) {
                BaseEntity baseEntityEntryDb = baseEntityEntriesDb.get(reportDate);

                // делаем повторную проверку чтобы удостовериться что действительно что то поменялось
                if (markBaseEntityChanges(processedBaseEntityEntry, baseEntityEntryDb).size() > 0)
                    baseEntityHolder.putAsUpdatedBaseEntityEntry(processedBaseEntityEntry);
            } else
                baseEntityHolder.putAsNewBaseEntityEntry(processedBaseEntityEntry);
        }
    }

    /**
     * метод сверяет имеющуюся атрибуты сущности из БД с атрибутами которые прислали
     * если значение атрибута поменялось то метод помечает его как измененный
     * */
    private List<MetaAttribute> markBaseEntityChanges(final BaseEntity baseEntityApplied, final BaseEntity baseEntityLoaded) {
        List<MetaAttribute> modifiedAttributes = new LinkedList<>();

        MetaClass metaClass = baseEntityApplied.getMetaClass();

        int compareDates = baseEntityLoaded.getReportDate().compareTo(baseEntityApplied.getReportDate());

        for (MetaAttribute metaAttribute : metaClass.getAttributes()) {
            BaseValue childBaseValueLoaded = baseEntityLoaded.getBaseValue(metaAttribute.getName());
            BaseValue childBaseValueApplied = baseEntityApplied.getBaseValue(metaAttribute.getName());

            if (childBaseValueLoaded == null && childBaseValueApplied == null)
                continue;

            if (childBaseValueLoaded == null && childBaseValueApplied.getValue() == null) {
                //  modifiedAttributes.add(metaAttribute);
                continue;
            }

            // очень важные проверки чтобы проверить насколько методы отработали правильно
            // пример: если значение в БД пустое то и должно быть childBaseValueLoaded=null
                if (childBaseValueLoaded != null && childBaseValueLoaded.getValue() == null)
                    throw new UsciException("Ошибка не правильно подгруженных данных");
                if (compareDates >= 0)
                    if (childBaseValueLoaded != null && childBaseValueApplied == null)
                        throw new UsciException(String.format("Ошибка не правильно отработанных методов. %s", metaAttribute.getName()));

                if (compareDates < 0 && childBaseValueLoaded != null && childBaseValueApplied == null) {
                    modifiedAttributes.add(metaAttribute);
                    continue;
                }

                childBaseValueApplied.setChanged(Boolean.FALSE);
                if ((childBaseValueLoaded == null && childBaseValueApplied.getValue() != null) ||
                        (childBaseValueLoaded != null && childBaseValueApplied.getValue() == null) ||
                        (!childBaseValueApplied.equalsByValue(childBaseValueLoaded))) {
                    childBaseValueApplied.setChanged(Boolean.TRUE);
                    modifiedAttributes.add(metaAttribute);
                }
        }

        return modifiedAttributes;
    }

    private boolean processSimpleBaseValue(MetaAttribute metaAttribute,
                                           BaseEntity baseEntityApplied,
                                           BaseValue baseValueSaving, BaseValue baseValueLoaded) {
        boolean keyChanged = false;
        if (baseValueLoaded == null && baseValueSaving == null)
            return false;

        BaseValue baseValueApplied = null;

        // есть значение по атрибуту но не пришло в батче выбрасываем исключение так как обработка
        // такой ситуаций требует анализа признака isNulable, см. код в processExistingBaseEntity
        if (baseValueLoaded != null && baseValueSaving == null)
            throw new UsciException("Ошибочные аргументы для метода");

        // значения по атрибуту не было и попытку обнуления
        // просто игнорируем (выходим из метода), не станем выбрасывать исключение
        if (baseValueSaving.getValue() == null) {
            // по ранее пустому ключу появилось значение, запрещаем такое
            // даже если такое может быть то все равно необходимо тестировать
            if (baseValueSaving.getNewBaseValue() != null)
                throw new UsciException("Ошибочное значение атрибута для изменения ключевых полей сущности");

            baseValueApplied = baseValueSaving;
            //return false;
        }

        // не было значения но в батче пришло значение
        if (baseValueLoaded == null && baseValueSaving.getValue() != null) {
            if (baseValueSaving.getNewBaseValue() != null)
                logger.error("Ошибочное значение атрибута для изменения ключевых полей сущности {} {}", baseEntityApplied, baseValueSaving.getNewBaseValue());

            baseValueApplied = baseValueSaving;
            if (metaAttribute.isKey())
                keyChanged = true;
        }
        // есть значение по атрибуту и в батче пришло какое то значение
        else if (baseValueLoaded != null && baseValueSaving.getValue() != null) {
            baseValueApplied = new BaseValue(baseValueSaving.getValue());

            if (baseValueSaving.getNewBaseValue() != null) {
                if (!metaAttribute.isKey())
                    throw new UsciException("Атрибут изменения ключевого поля предназначен только для ключевых полей");

                if (baseValueSaving.getNewBaseValue().getValue() == null)
                    throw new UsciException("Пустое значение атрибута для изменения ключевого поля сущности");

                // теоретический такое не возможно но необходимо перестраховаться
                if (!baseValueLoaded.equalsByValue(baseValueSaving))
                    throw new UsciException("Ошибочное значение атрибута для изменения ключевых полей сущности");

                baseValueApplied = baseValueSaving.getNewBaseValue();
                baseValueApplied.setOldBaseValue(baseValueSaving);

                // обновление ключевых полей в хабе
                // проверим поменялось ли ключевое поле на самом деле
                // a) случай когда ключ на самом деле не меняется (зачем тогда вообще высылать в батче) тогда просто логируем
                if (baseValueLoaded.equalsByValue(baseValueSaving.getNewBaseValue()))
                    logger.info("Изменение ключего поля сущности с одиноковыми ключами {}", baseEntityApplied, baseValueSaving);
                // b) ключ меняется по факту
                else {
                    if (!baseEntityApplied.isApproved()) {
                        LocalDate minReportDate = baseEntityDateSerivce.getMinReportDate(baseEntityApplied);
                        Long minReportDateMillis = minReportDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        Long productId = baseEntityApplied.getProductId();
                        Long receiptDateMillis = baseEntityApplied.getBatchReceiptDate().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

                        boolean isMaintenance = productService.checkForApprove(productId, receiptDateMillis, minReportDateMillis);
                        baseEntityApplied.setMaintenance(isMaintenance);
                    }
                    keyChanged = true;
                }
            }
        }

        baseEntityApplied.put(metaAttribute.getName(), baseValueApplied);

        return keyChanged;
    }

    private void processComplexBaseValue(MetaAttribute metaAttribute, BaseEntity baseEntityApplied, BaseEntity baseEntityLoaded,
                                         BaseValue baseValueSaving, BaseValue baseValueLoaded, BaseEntityHolder baseEntityHolder) {
        Objects.requireNonNull(baseValueSaving, "Аргумент baseValueSaving равен null");

        MetaClass childMetaClass = (MetaClass) metaAttribute.getMetaType();

        // mock обьектом бывают оперативные данные
        // у сущности как кредит может быть mock обьект как change но у change может не быть запись за дату батча
        // так как change оперативный, (см. BaseEntityLoadService)
        BaseEntity childBaseEntityLoaded = null;
        if (baseValueLoaded != null && baseValueLoaded.getValue() != null && !baseValueLoaded.getMock())
            childBaseEntityLoaded = (BaseEntity) baseValueLoaded.getValue();

        BaseEntity childBaseEntitySaving = null;
        if (baseValueSaving.getValue() != null) {
            childBaseEntitySaving = (BaseEntity) baseValueSaving.getValue();
            childBaseEntitySaving.setProductId(baseEntityApplied.getProductId());
            childBaseEntitySaving.setBatchReceiptDate(baseEntityApplied.getBatchReceiptDate());
        }

        // переиспользую id из mock обьекта (см. BaseEntityLoadService)
        if (childBaseEntitySaving != null && childBaseEntitySaving.getId() == null && baseValueLoaded != null && baseValueLoaded.getMock())
            childBaseEntitySaving.setId(((BaseEntity) baseValueLoaded.getValue()).getId());

        BaseEntity childBaseEntityApplied;

        // случай когда ранее по атрибуту не было значения но теперь появилось
        if (childBaseEntityLoaded == null && childBaseEntitySaving != null) {
            if (metaAttribute.isReference()) {
                if (childBaseEntitySaving.getId() == null)
                    throw new UsciException(String.format("Сущность %s ссылка должна иметь идентификатор", childBaseEntitySaving));

                // подгружаем ссылочную сущность чтобы проверить статус закрытия
                // потому что ссылка на закрытую сущность не должна быть
                childBaseEntityApplied = baseEntityLoadService.loadByMaxReportDate(childBaseEntitySaving, childBaseEntitySaving.getReportDate());
                if (childBaseEntityApplied.isClosed())
                    throw new ClosedBaseEntityException(childBaseEntityApplied);
            }
            else {
                childBaseEntityApplied = processBaseEntity(childBaseEntitySaving, null, baseEntityHolder);
                if (childBaseEntityApplied.isMaintenance())
                    baseEntityApplied.setMaintenance(true);
            }

            baseEntityApplied.put(metaAttribute.getName(), new BaseValue(childBaseEntityApplied));
        }
        // случай когда ранее по атрибуту было значение но теперь его не стало:
        // a) если прислали пустой тег <debt></debt> или <head></head> то обнуляем атрибут,
        // затем удаляем ранее созданную запись по сущности debt(и все его поддерево) за отчетную дату
        // b) если ранее в этот же отчетный период было подгружено значение  (например <remains><debt>...</debt>...</remains>)
        // но при повторной загрузке (признак isNullify) его не отправили в xml <remains>...</remains> то обнуляем сущность (см. опцию "а")
        else if (childBaseEntityLoaded != null && baseValueSaving.getValue() == null) {
            LocalDate reportDateSaving = baseEntityApplied.getReportDate();
            LocalDate reportDateLoaded = baseEntityLoaded.getReportDate();

            // добавил проверку на обогащение для закрытых сущнестей справочников
            if (metaAttribute.isReference()) {
                if (childBaseEntityLoaded.isClosed())
                    throw new ClosedBaseEntityException(childBaseEntityLoaded);
            }
            baseEntityApplied.put(metaAttribute.getName(), new BaseValue());


            int compareDates = reportDateSaving.compareTo(reportDateLoaded);
            if (compareDates == 0) {
                // каскадное удаление исторических записей сущности
                // примечание: нельзя удалять сущность на которую ссылаемся
                // удаляем если сущность не searchable
                if (!childMetaClass.isSearchable() && !metaAttribute.isReference())
                    deleteBaseEntityEntry(childBaseEntityLoaded, childBaseEntityLoaded.getReportDate(), baseEntityHolder);
            }
        }
        // случай когда по атрибуту ранее было значение и заново пришло значение (возможно такое же или другое)
        // то есть в этот раз может придти совсем другое значение
        // допустим если ранее по кредиту субьект пришел как Канат Тулбасиев и теперь приходит как Артур Ткаченко
        // для этого сравниваем id двух сущностей
        else if (childBaseEntityLoaded != null && childBaseEntitySaving != null) {
            // необходимо заново подгружать ссылочную сущность потому что на момент батча она может закрыться
            // то есть ссылочная сущность которую мы подгружали может быть устаревшей
            if (metaAttribute.isReference()) {
                if (childBaseEntitySaving.getId() == null)
                    throw new UsciException(String.format("Сущность %s ссылка должна иметь идентификатор", childBaseEntitySaving));

                childBaseEntityApplied = baseEntityLoadService.loadByMaxReportDate(childBaseEntitySaving, childBaseEntitySaving.getReportDate());
                if (childBaseEntityApplied.isClosed())
                    throw new ClosedBaseEntityException(childBaseEntityApplied);
            }
            else {
                // случай когда значение атрибута не поменялось или не идентифицируемое комплексное значение
                if (Objects.equals(childBaseEntitySaving.getId(), childBaseEntityLoaded.getId()) || !childMetaClass.isSearchable()) {
                    childBaseEntityApplied = childMetaClass.isSearchable() ?
                            processBaseEntity(childBaseEntitySaving, childBaseEntityLoaded, baseEntityHolder) :
                            processExistingBaseEntity(childBaseEntitySaving, childBaseEntityLoaded, baseEntityHolder);
                    if (childBaseEntityApplied.isMaintenance())
                        baseEntityApplied.setMaintenance(true);
                } else {
                    // случай когда значение атрибута поменялось (см. пояснение выше)
                    childBaseEntityApplied = processBaseEntity(childBaseEntitySaving, null, baseEntityHolder);
                    if (childBaseEntityApplied.isMaintenance())
                        baseEntityApplied.setMaintenance(true);
                }
            }

            baseEntityApplied.put(metaAttribute.getName(), new BaseValue(childBaseEntityApplied));
        }
    }

    private void processSimpleBaseSet(MetaAttribute metaAttribute,
                                      BaseEntity baseEntityApplied,
                                      BaseEntity baseEntityLoaded,
                                      BaseValue baseValueSaving,
                                      BaseValue baseValueLoaded) {
        Objects.requireNonNull(baseEntityLoaded, "Загруженная сущность не инициализирована");
        Objects.requireNonNull(baseEntityLoaded.getId(), "По загруженной сущности отсутствует id");

        LocalDate reportDateSaving = baseEntityApplied.getReportDate();
        LocalDate reportDateLoaded = baseEntityLoaded.getReportDate();

        int compareDates = reportDateSaving.compareTo(reportDateLoaded);

        MetaType metaType = metaAttribute.getMetaType();

        MetaSet metaSet = (MetaSet) metaType;

        BaseSet childBaseSetSaving = null;
        if (baseValueSaving != null && baseValueSaving.getValue() != null)
            childBaseSetSaving = (BaseSet) baseValueSaving.getValue();

        BaseSet childBaseSetLoaded = null;
        if (baseValueLoaded != null && baseValueLoaded.getValue() != null)
            childBaseSetLoaded = (BaseSet) baseValueLoaded.getValue();

        // если ранее сета не было еще и пустой пришел сет то и нечего обрабатывать
        // запрещаем предоставлять такие аргументы методу
        if (baseValueLoaded == null && baseValueSaving == null)
            throw new UsciException("Ошибочные аргументы для метода, оба сета пустые");

        // если на данный момент уже есть сет но значения по нему отсутствуют то подгрузка сета или его сохранение ранее отработали не правильно
        // если сет пустой то должно быть baseValueLoaded = null иначе методы отработали не правильно
        if (baseValueLoaded != null && (baseValueLoaded.getValue() == null || childBaseSetLoaded.getValueCount() == 0))
            throw new UsciException("Не правильно загруженный сет");

        // случай когда может быть что сета ранее не было но теперь хотят сет обнулить
        // просто игнорируем (выходим из метода), не станем выбрасывать исключение
        if (baseValueLoaded == null && baseValueSaving.getValue() == null)
            return;

        BaseSet childBaseSetApplied = new BaseSet(metaSet.getMetaType());

        Map<Object, BaseValue> childBaseSetLoadedValues = null;
        if (childBaseSetLoaded != null)
            childBaseSetLoadedValues = childBaseSetLoaded.getValues().stream()
                    .collect(Collectors.toMap(BaseValue::getValue, childBaseSetValue -> childBaseSetValue));

        if (childBaseSetSaving != null) {
            for (BaseValue childBaseValueSaving : childBaseSetSaving.getValues()) {
                if (childBaseSetLoaded != null)
                    childBaseSetLoadedValues.remove(childBaseValueSaving.getValue());

                childBaseSetApplied.put(childBaseValueSaving);
            }
        }

        // признак обнуления сета
        boolean isBaseSetDeleted = baseValueLoaded != null && (childBaseSetSaving == null || childBaseSetSaving.getValueCount() == 0);

        // кумулятивный сет если даже значение не пришло в батче то все равно дополняем сет из ранее загруженных
        if (childBaseSetLoaded != null && !isBaseSetDeleted && metaAttribute.isCumulative() && compareDates >= 0)
            childBaseSetLoadedValues.values().forEach(childBaseSetApplied::put);

        // если обнулили сет то получим childBaseSetApplied.getValueCount = 0 но мы заменим его на new BaseValue()
        baseEntityApplied.put(metaAttribute.getName(), childBaseSetApplied.getValueCount() > 0?
                new BaseValue(childBaseSetApplied): new BaseValue());
    }

    private boolean processComplexBaseSet(MetaAttribute metaAttribute,
                                       BaseEntity baseEntityApplied,
                                       BaseEntity baseEntityLoaded,
                                       BaseValue baseValueSaving,
                                       BaseValue baseValueLoaded,
                                       BaseEntityHolder baseEntityHolder) {
        Objects.requireNonNull(baseEntityLoaded, "Загруженная сущность не инициализирована");
        Objects.requireNonNull(baseEntityLoaded.getId(), "По загруженной сущности отсутствует id");

        boolean keyChanged = false;

        MetaType metaType = metaAttribute.getMetaType();


        MetaSet childMetaSet = (MetaSet) metaType;
        MetaType childMetaType = childMetaSet.getMetaType();
        MetaClass childMetaClass = (MetaClass) childMetaType;

        LocalDate reportDateSaving = baseEntityApplied.getReportDate();
        LocalDate reportDateLoaded = baseEntityLoaded.getReportDate();

        int compareDates = reportDateSaving.compareTo(reportDateLoaded);

        BaseSet childBaseSetLoaded = null;
        if (baseValueLoaded != null && baseValueLoaded.getValue() != null)
            childBaseSetLoaded = (BaseSet) baseValueLoaded.getValue();

        BaseSet childBaseSetSaving = null;
        if (baseValueSaving != null && baseValueSaving.getValue() != null)
            childBaseSetSaving = (BaseSet) baseValueSaving.getValue();

        // если ранее сета не было еще и пустой пришел сет то и нечего обрабатывать
        // запрещаем предоставлять такие аргументы методу
        if (baseValueLoaded == null && baseValueSaving == null)
            throw new UsciException("Ошибочные аргументы для метода, оба сета пустые");

        // если на данный момент уже есть сет но значения по нему отсутствуют то подгрузка сета или его сохранение ранее отработали не правильно
        // если сет пустой то должно быть baseValueLoaded = null иначе методы отработали не правильно
        if (baseValueLoaded != null && (baseValueLoaded.getValue() == null || childBaseSetLoaded.getValueCount() == 0))
            throw new UsciException("Не правильно загруженный сет");

        // случай когда может быть что сета ранее не было но теперь хотят сет обнулить
        // просто игнорируем (выходим из метода), не станем выбрасывать исключение
        if (baseValueLoaded == null && (baseValueSaving.getValue() == null || ((BaseSet) baseValueSaving.getValue()).getValueCount() == 0))
            return keyChanged;

        Map<Long, BaseValue> childBaseSetLoadedValues = null;
        if (childBaseSetLoaded != null)
            childBaseSetLoadedValues = childBaseSetLoaded.getValues().stream()
                    .collect(Collectors.toMap(childBaseSetValue -> ((BaseEntity) childBaseSetValue.getValue()).getId(),
                            childBaseSetValue -> childBaseSetValue));

        BaseSet childBaseSetApplied = new BaseSet(childMetaClass);

        Set<UUID> processedUuidSet = new HashSet<>();

        if (childBaseSetSaving != null) {
            // обрабатываем каждую сущность сета
            // если сущность из батча уже существует то отправляем ее в processExistingBaseEntity
            for (BaseValue childBaseValueSaving : childBaseSetSaving.getValues()) {
                BaseEntity childBaseEntitySaving = (BaseEntity) childBaseValueSaving.getValue();
                childBaseEntitySaving.setProductId(baseEntityApplied.getProductId());
                childBaseEntitySaving.setBatchReceiptDate(baseEntityApplied.getBatchReceiptDate());
                BaseEntity childBaseEntityApplied = null;

                boolean baseValueFound = false;

                if (metaAttribute.isReference() && childBaseEntitySaving.getId() == null)
                    throw new UsciException(String.format("В базе нет данных для записи %s до отчетной даты(включительно): %s",
                            childBaseEntitySaving, childBaseEntitySaving.getReportDate()));

                if (childBaseSetLoaded != null) {
                    // сканируем весь массив childBaseSetLoaded чтобы найти сущность
                    for (BaseValue childBaseValueLoaded : childBaseSetLoaded.getValues()) {
                        BaseEntity childBaseEntityLoaded = (BaseEntity) childBaseValueLoaded.getValue();

                        // проверяяем обработанные сущности чтобы не допускать дубликатов
                        // processedUuidSet используется потому что мы не удаляем обработанные элементы из childBaseSetLoaded
                        // также удалять из childBaseSetLoaded не правильно, загруженные данные для сверки не должны меняться
                        if (processedUuidSet.contains(childBaseEntityLoaded.getUuid()))
                            continue;

                        // equals используется для не идентицифируемых сущностей (например bank_relation, contact)
                        // не идентифицируемые сущности могут встречаться в сете несколько раз
                        // то есть иметь те же значения и дублироваться но мы все равно должны их садить в базе столько сколько пришло
                        if (childBaseValueSaving.equals(childBaseValueLoaded) || childBaseEntityLoaded.getId().equals(childBaseEntitySaving.getId())) {
                            processedUuidSet.add(childBaseEntityLoaded.getUuid());
                            baseValueFound  = true;

                            // ранее загруженная сущность, отправляем ее на обработку
                            // потому что по сущности могут придти изменения (а могут и не придти)
                            // то есть ссылочная сущность которую мы подгружали может быть устаревшей
                            if (metaAttribute.isReference()) {
                                childBaseEntityApplied = baseEntityLoadService.loadByMaxReportDate(childBaseEntitySaving, childBaseEntitySaving.getReportDate());
                                if (childBaseEntityApplied.isClosed())
                                    throw new ClosedBaseEntityException(childBaseEntityApplied);
                                childBaseSetApplied.put(new BaseValue(childBaseEntityApplied));
                            } else {
                                childBaseEntityApplied = processExistingBaseEntity(childBaseEntitySaving, childBaseEntityLoaded, baseEntityHolder);
                                if (childBaseEntityApplied.isMaintenance())
                                    baseEntityApplied.setMaintenance(true);
                                childBaseSetApplied.put(new BaseValue(childBaseEntityApplied));
                            }

                            childBaseSetLoadedValues.remove(childBaseEntityLoaded.getId());

                            break;
                        }
                    }

                    // если сущность обработана то дальше не проходим иначе она будет воспринята как новый элемент сета и добавится туда
                    if (baseValueFound)
                        continue;
                }

                if (metaAttribute.isReference()) {
                    childBaseEntityApplied = baseEntityLoadService.loadByMaxReportDate(childBaseEntitySaving, childBaseEntitySaving.getReportDate());
                    if (childBaseEntityApplied.isClosed())
                        throw new ClosedBaseEntityException(childBaseEntityApplied);
                } else {
                    childBaseEntityApplied = processBaseEntity(childBaseEntitySaving, null, baseEntityHolder);
                    if (childBaseEntityApplied.isMaintenance())
                        baseEntityApplied.setMaintenance(true);
                }
                childBaseSetApplied.put(new BaseValue(childBaseEntityApplied));
                if (metaAttribute.isKey() & metaAttribute.isCumulative()) {
                    keyChanged = true;
                }
            }
        }

        // признак обнуления сета
        boolean isBaseSetDeleted = baseValueLoaded != null && (childBaseSetSaving == null || childBaseSetSaving.getValueCount() == 0);

        if (childBaseSetLoaded != null) {
            for (BaseValue childBaseValueLoaded : childBaseSetLoadedValues.values()) {
                BaseEntity childBaseEntityLoaded =  (BaseEntity) childBaseValueLoaded.getValue();

                // кейс когда сет не накопительный или накопительный и его хотят обнулить
                if (!metaAttribute.isCumulative() || isBaseSetDeleted) {
                    // случай когда ранее за отчетный период загружали сет и в ней была сущность
                    // но теперь перегружают и в этот раз в сете нет этой сущности (или вообще сет обнуляют)
                    // тогда удаляем если сущность searchable=false, для других это правило не распростаняется
                    // к примеру сущность как contact то есть не идентифицируемая может попасть под данное условие
                    if (childBaseEntityLoaded.getReportDate().equals(baseEntityApplied.getReportDate()) && !childMetaClass.isSearchable())
                        deleteBaseEntityEntry(childBaseEntityLoaded, childBaseEntityLoaded.getReportDate(), baseEntityHolder);
                }
                // когда если сет не обнуляют но ранее был сет и он кумулятивный дополняем сет из ранее существующих
                // но только если загружают по очереди; то есть если присылают задней датой то кумулятивный сет не обогащается
                else if (metaAttribute.isCumulative() && compareDates >= 0) {
                    childBaseSetApplied.put(new BaseValue(childBaseEntityLoaded));
                }
            }
        }

        // если обнулили сет то получим childBaseSetApplied.getValueCount = 0 но мы заменим его на new BaseValue()
        baseEntityApplied.put(metaAttribute.getName(), childBaseSetApplied.getValueCount() > 0?
                new BaseValue(childBaseSetApplied): new BaseValue());

        return keyChanged;
    }

    @Override
    @Transactional
    public void storeInDb(BaseEntityHolder baseEntityHolder, BaseEntity baseEntity) {

        if (baseEntity.isMaintenance() && !baseEntity.isApproved())
            storeBaseEntityMaintenance(baseEntity, baseEntityHolder);
        else
            storeBaseEntityHolder(baseEntityHolder);

        // заливаем данные непосредственно в схему EAV_XML
        // если сущность с одобрением, то он уже есть в EAV_XML
        if (!baseEntity.isApproved())
            storeBaseEntityInSchemaEavXml(baseEntity);
    }

    /**
     * метод выполняет DML операций в БД
     * - создает запись по новой сущности
     * - изменяет историю по существующим сущностям
     * - добавляет историю по существующим сущностям
     * - сдвигает отчетную дату назад (изменение отчетной даты когда прислали сущность задней датой но по ней нет никаких изменений)
     * - удаляет сущность
     * */
    private void storeBaseEntityHolder(BaseEntityHolder baseEntityHolder) {
        Map<MetaClass, List<BaseEntity>> newBaseEntities = baseEntityHolder.getNewBaseEntities();

        // необходимо заранее присвоить сущностям ID перед insert так как есть зависимость сущностей
        generateBaseEntityId(baseEntityHolder);

        // все searchable сущности ложим в лист чтобы потом прогонять батчами
        List<BaseEntity> searchableBaseEntities = new ArrayList<>();

        // инсертим непосредственно сущности в таблицы БД
        for (MetaClass metaClass : newBaseEntities.keySet()) {
            List<BaseEntity> baseEntities = newBaseEntities.get(metaClass);

            for (BaseEntity baseEntity: baseEntities) {
                //baseEntityHolder.putAsNewBaseEntityRegistry(new BaseEntityRegistry(baseEntity));

                // в хабе необходимо хранить только идентифицируемые сущности
                // остальным там делать нечего, потому как хаб нужен для поиска сущности
                if (metaClass.isSearchable())
                    searchableBaseEntities.add(baseEntity);            }

            insertBaseEntityToDb(EavSchema.EAV_DATA, metaClass, baseEntities);

            /*List<List<BaseEntity>> partitions = Lists.partition(baseEntities, Constants.OPTIMAL_BATCH_SIZE[1]);
            for (List<BaseEntity> partition: partitions) {
                // рекомендуемая мера batch insert кол-во записей между 5-30 (согласно оффициальной документаций оракла)
                // иначе batch insert показывает очень плохую производительность, тогда даже лучше делать insert обычным способом
                if (partition.size() >= Constants.OPTIMAL_BATCH_SIZE[0] && partition.size() <= Constants.OPTIMAL_BATCH_SIZE[1])
                    insertBaseEntityToDb(EavSchema.EAV_DATA, metaClass, partition);
                else {
                    for (BaseEntity baseEntity : partition)
                        insertBaseEntityToDb(EavSchema.EAV_DATA, metaClass, Collections.singletonList(baseEntity));
                }
            }*/

        }

        List<List<BaseEntityDate>> partitions = Lists.partition(baseEntityHolder.getNewBaseEntityDates(), Constants.OPTIMAL_BATCH_SIZE[1]);


        for (List<BaseEntityDate> partition: partitions) {
            if (partition.size() >= Constants.OPTIMAL_BATCH_SIZE[0] && partition.size() <= Constants.OPTIMAL_BATCH_SIZE[1])
                baseEntityDateDao.insert(partition);
            else
                for (BaseEntityDate baseEntityDate : partition)
                    baseEntityDateDao.insert(Collections.singletonList(baseEntityDate));
        }

        for (BaseEntityDate baseEntityDate : baseEntityHolder.getUpdatedBaseEntityDates())
            baseEntityDateDao.update(baseEntityDate);

        if (searchableBaseEntities.size() > 0)
            baseEntityHubService.insert(searchableBaseEntities);

        if (baseEntityHolder.getMigrationErrorsHub().size() > 0)
            baseEntityHubService.insert(baseEntityHolder.getMigrationErrorsHub());

       /* if (baseEntityHolder.getNewBaseEntityRegistries().size() > 0)
            entityService.insert(baseEntityHolder.getNewBaseEntityRegistries());*/

        for (BaseEntity baseEntity : baseEntityHolder.getDeletedBaseEntityEntries())
            deleteBaseEntityEntryFromDb(baseEntity);

        for (BaseEntity baseEntity : baseEntityHolder.getNewBaseEntityEntries())
            insertBaseEntityToDb(EavSchema.EAV_DATA, baseEntity.getMetaClass(), Collections.singletonList(baseEntity));

        for (BaseEntity baseEntity : baseEntityHolder.getUpdatedBaseEntityEntries())
            updateBaseEntityInDb(baseEntity, false);

        for (BaseEntity baseEntity : baseEntityHolder.getMovedBaseEntityDates())
            updateBaseEntityInDb(baseEntity, true);

        for (EavHub eavHub : baseEntityHolder.getUpdatedBaseEntityHubs())
            baseEntityHubService.update(eavHub);

        for (EavHub eavHub : baseEntityHolder.getDeletedBaseEntityHubs())
            baseEntityHubService.deleteAll(eavHub);

        for (Map.Entry<BaseEntity, List<String>> entry : baseEntityHolder.getAddBaseEntityHubs().entrySet())
            baseEntityHubService.add(entry.getKey(), entry.getValue());
    }

    private Long generateBaseEntityId(MetaClass metaClass) {
        Objects.requireNonNull(metaClass, "Мета класс пустой");

        return jdbcTemplate.queryForObject(String.format("select EAV_DATA.%s.NEXTVAL from dual", metaClass.getSequenceName()), Long.class);
    }

    /**
     * метод делает insert сущности в таблицы схемы EAV_XML
     * @param baseEntitySaving должен быть корень дерева и все сущности должны иметь ID. Например кредит является корнем дерева.
     * был выбран подход обхода дерева в ширину так как заливка в схему EAV_XML происходит
     * после заливки в схему EAV_DATA и всем сущностям уже был присвоен id
     * то есть нам абсолюто не важно в каком порядке мы обрабатываем сущности
     * */
    @Async
    @Transactional
    public void storeBaseEntityInSchemaEavXml(final BaseEntity baseEntitySaving) {
        Queue<BaseEntity> baseEntityQueue = new LinkedList<>();
        baseEntityQueue.add(baseEntitySaving);

        // группирую сущности по мета классу чтобы потом делать batch insert
        Map<MetaClass, List<BaseEntity>> baseEntities = new HashMap<>();

        while (baseEntityQueue.size() > 0) {
            BaseEntity baseEntityQueued = baseEntityQueue.poll();

            // при закрытий или открытий могут выслать новые записи их просто игнорируем
            if (baseEntityQueued.getId() == null && (baseEntitySaving.getOperation() == OperType.CLOSE ||
                    baseEntitySaving.getOperation() == OperType.OPEN ||
                    baseEntitySaving.getOperation() == OperType.DELETE))
                baseEntityQueued.setId(-1L);

            MetaClass metaClass = baseEntityQueued.getMetaClass();

            if (!baseEntities.containsKey(metaClass))
                baseEntities.put(metaClass, new ArrayList<>());

            baseEntities.get(metaClass).add(baseEntityQueued);

            for (String attribute : baseEntityQueued.getAttributeNames()) {
                MetaAttribute metaAttribute = baseEntityQueued.getMetaAttribute(attribute);
                MetaType metaType = metaAttribute.getMetaType();

                BaseValue baseValue = baseEntityQueued.getBaseValue(attribute);
                if (!(baseValue != null && baseValue.getValue() != null && metaType.isComplex() && !metaAttribute.isReference()))
                    continue;

                MetaType childMetaType = metaAttribute.getMetaType();

                if (childMetaType.isSet()) {
                    BaseSet childBaseSet = (BaseSet) baseValue.getValue();
                    for (BaseValue childBaseValue : childBaseSet.getValues())
                        if (childBaseValue.getValue() != null)
                            baseEntityQueue.add((BaseEntity) childBaseValue.getValue());
                } else {
                    baseEntityQueue.add((BaseEntity) baseValue.getValue());
                }
            }
        }

        for (MetaClass metaClass : baseEntities.keySet()) {
            List<BaseEntity> list = baseEntities.get(metaClass);
            insertBaseEntityToDb(EavSchema.EAV_XML, metaClass, list);
        }

    }

    @Transactional
    public void storeBaseEntityMaintenance(final BaseEntity baseEntitySaving, BaseEntityHolder baseEntityHolder) {
        generateBaseEntityId(baseEntityHolder);

        Gson gson = new Gson();

        EntityExtJsTreeJson json = entityToJson(baseEntitySaving, baseEntitySaving.getMetaClass().getClassTitle(), baseEntitySaving.getMetaClass().getClassName(), null, true);
        String result = gson.toJson(json);

        List<String> keys = baseEntityHubService.getKeys(baseEntitySaving).values().stream()
                .flatMap(Collection::stream)
                .collect(toList());

        StringBuilder entityKey = new StringBuilder();
        keys.forEach(key -> {
            entityKey.append(key);
            entityKey.append(",");
        });

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("BATCH_ID", baseEntitySaving.getBatchId())
                .addValue("CREDITOR_ID",baseEntitySaving.getRespondentId())
                .addValue("REPORT_DATE",SqlJdbcConverter.convertToSqlDate(baseEntitySaving.getReportDate()))
                .addValue("SYSTEM_DATE", SqlJdbcConverter.convertToSqlTimestamp(LocalDateTime.now()))
                .addValue("ENTITY_ID", baseEntitySaving.getId())
                .addValue("ENTITY_TEXT", baseEntitySaving.toString())
                .addValue("ENTITY_KEY", BaseEntityOutput.getEntityAsString(baseEntitySaving, true))
                .addValue("ENTITY_JSON", result)
                .addValue("OPERATION_ID", baseEntitySaving.getOperation() != null ? baseEntitySaving.getOperation().getId() : null)
                .addValue("META_CLASS_ID", baseEntitySaving.getMetaClass().getId())
                .addValue("APPROVED", 0)
                .addValue("DECLINED", 0)
                .addValue("PRE_APPROVED", 1)
                .addValue("PRE_DECLINED", 0);

        baseEntityMaintenceInsert.execute(params);
    }

    private void insertBaseEntityToDb(final EavSchema schema, MetaClass metaClass, final List<BaseEntity> baseEntities) {

        List<MapSqlParameterSource> params = new ArrayList<>();
        for (BaseEntity baseEntity : baseEntities) {
            // обязательные поля в реляционной таблице
            Set<String> columns = new HashSet<>(Arrays.asList("ENTITY_ID", "CREDITOR_ID", "REPORT_DATE",
                    "OPERATION_ID", "BATCH_ID", "SYSTEM_DATE", "X_RECEIVED"));
            MapSqlParameterSource param = new MapSqlParameterSource();


            param.addValue("ENTITY_ID", baseEntity.getId());
            param.addValue("BATCH_ID", baseEntity.getBatchId());
            param.addValue("CREDITOR_ID", baseEntity.getRespondentId());
            param.addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(baseEntity.getReportDate()));
            param.addValue("SYSTEM_DATE", SqlJdbcConverter.convertToSqlTimestamp(LocalDateTime.now()));
            param.addValue("X_RECEIVED", BaseEntityUtils.getReceivedAsString(baseEntity.getMetaClass(), baseEntity.getReceived()));

            // тип операций может быть установлен заранее например как закрытие или открытие
            // если же тип операций не проставлен то помечаем его как insert
            param.addValue("OPERATION_ID", baseEntity.getOperation() != null? baseEntity.getOperation().getId(): OperType.INSERT.getId());

            // фиксирую изменение ключей в схеме EAV_XML в поле OLD_ENTITY_KEY
            // для этого в классе BaseEntity есть поле oldEntityKey
            if (schema == EavSchema.EAV_XML && !StringUtils.isEmpty(baseEntity.getOldEntityKey())) {
                param.addValue("OLD_ENTITY_KEY", baseEntity.getOldEntityKey());
                param.addValue("STATUS_ID", 1L);

                columns.add("OLD_ENTITY_KEY");
                columns.add("STATUS_ID");
            }

            for (String attribute: baseEntity.getAttributeNames()) {
                MetaAttribute metaAttribute = baseEntity.getMetaAttribute(attribute);
                BaseValue baseValue = baseEntity.getBaseValue(attribute);

                Object sqlValue = null;
                if (baseValue.getValue() != null)
                    sqlValue = convertBaseValueToSqlValue(schema.getCode(), metaAttribute, baseValue);

                // согласно схеме EAV_XML если пришел пустой тег (<head></head>) то ставлю "1" в столбце с префиксом N_
                if (schema == EavSchema.EAV_XML && baseValue.getValue() == null) {
                    String columnName = "N_".concat(metaAttribute.getColumnName());

                    param.addValue(columnName, "1");
                    columns.add(columnName);
                }

                if (sqlValue != null) {
                    param.addValue(metaAttribute.getColumnName(), sqlValue);
                    columns.add(metaAttribute.getColumnName());
                }

            }
           // params.add(param);

            setMetaClassSqlTypes(schema, metaClass, param);
            List<String> columnsOrdered = new ArrayList<>(columns);

            String tableName = String.join(".", schema.getCode(), metaClass.getTableName());

            StringBuilder insertQueryBuilder = new StringBuilder("insert into ");
            insertQueryBuilder.append(tableName)
                    .append("\n\t(")
                    .append(StringUtils.collectionToCommaDelimitedString(columnsOrdered))
                    .append(")\n")
                    .append("values\n\t(");

            boolean isFirst = true;
            for (String columnName : columnsOrdered) {
                if (!isFirst)
                    insertQueryBuilder.append(",");
                insertQueryBuilder.append(":").append(columnName);

                isFirst = false;
            }

            insertQueryBuilder.append(")\n");

            String insertQuery = insertQueryBuilder.toString();



            try {
                int count = npJdbcTemplate.update(insertQuery, param);
                if (count == 0)
                    throw new UsciException(String.format("Ошибка завершения DML операций по таблице %s ", tableName));
            } catch (InvalidDataAccessApiUsageException e) {
                if (metaClass.getId() == 64)
                    throw new UsciException(String.format("Ошибка завершения: %s ", baseEntity.getBaseValue("is_preson")));
                else
                    throw new UsciException(String.format("Ошибка завершения: %s ", baseEntity));
            }

        }


    }

    private void setMetaClassSqlTypes(EavSchema schema, MetaClass metaClass, MapSqlParameterSource params) {
        for (MetaAttribute metaAttribute : metaClass.getAttributes()) {
            MetaType metaType = metaAttribute.getMetaType();

            String columnName = metaAttribute.getColumnName();

            params.registerSqlType("N_".concat(columnName), Types.VARCHAR);

            if (metaType.isSet()) {
                    params.registerSqlType(columnName, Types.ARRAY);

                    String sqlArrayType = schema.getCode().concat(".").concat(metaAttribute.getColumnType());
                    params.registerTypeName(columnName, sqlArrayType);
            } else if (metaType.isComplex())
                params.registerSqlType(columnName, Types.BIGINT);
            else {
                MetaDataType metaDataType = ((MetaValue) metaType).getMetaDataType();
                if (metaDataType == MetaDataType.DATE)
                    params.registerSqlType(columnName, Types.DATE);
                else if (metaDataType == MetaDataType.DOUBLE)
                    params.registerSqlType(columnName, Types.FLOAT);
                else if (metaDataType == MetaDataType.STRING || metaDataType == MetaDataType.BOOLEAN)
                    params.registerSqlType(columnName, Types.VARCHAR);
                else if (metaDataType == MetaDataType.INTEGER)
                    params.registerSqlType(columnName, Types.BIGINT);
            }

        }
    }

    /**
     * метод делает update сущности в БД (принцип следующий: одна сущность = одна запись в базе)
     * @param baseEntitySaving означает если мы хотим только обновить отчетную дату сущности
     * необходимо передавать сущность лишь в случае если есть атрибуты которые изменились, иначе метод выбрасывает исключение.
     * update производится лишь по измененным атрибутам(столбцам)
     * поиск записи сущности делается по полям CREDITOR_ID, ENTITY_ID, REPORT_DATE (PRIMARY KEY)
     * столбец BATCH_ID добавляется в update запрос чтобы зафиксировать последний батч по которому прилетела инфа
     * пример SQL запроса:
     * update EAV_DATA.PLEDGE
     *   set BATCH_ID = 45,
     *       VALUE = 30000
     *  where CREDITOR_ID = :CREDITOR_ID
     *    and ENTITY_ID = :ENTITY_ID
     *    and REPORT_DATE = :REPORT_DATE
     * если параметр baseEntitySaving = true то метод генерирует аналог SQL запроса:
     * update EAV_DATA.PLEDGE
     *   set BATCH_ID = 45,
     *       REPORT_DATE = :NEW_REPORT_DATE
     *  where CREDITOR_ID = :CREDITOR_ID
     *    and ENTITY_ID = :ENTITY_ID
     *    and REPORT_DATE = :REPORT_DATE
     * */
    private void updateBaseEntityInDb(final BaseEntity baseEntitySaving, boolean shiftReportDate) {
        Objects.requireNonNull(baseEntitySaving.getId(), "Ошибка отсутствия у сущности id");

        if (baseEntitySaving.getValueCount() == 0)
            throw new UsciException("У сущности отсутствуют значения");

        long changedAttributes = baseEntitySaving.getValues().stream().filter(BaseValue::isChanged).count();

        if (shiftReportDate) {
            if (baseEntitySaving.getOldReportDate() == null)
                throw new UsciException("Отсутствует старая отчетная дата у сущности");
            else {
                int compareDates = baseEntitySaving.getReportDate().compareTo(baseEntitySaving.getOldReportDate());
                if (compareDates >= 0)
                    throw new UsciException("Новая отчетная дата сущности дожна быть меньше старой");
            }

            if (changedAttributes > 0)
                throw new UsciException("Есть измененные атрибуты у сущности");
        }
        else {
            if (baseEntitySaving.getOldReportDate() != null)
                throw new UsciException("Присутствует старая отчетная дата у сущности");
            if (changedAttributes == 0 && (baseEntitySaving.getOperation() == null || baseEntitySaving.getOperation() == OperType.UPDATE))
                throw new UsciException("Нет измененных атрибутов у сущности");
        }

        MetaClass metaClass = baseEntitySaving.getMetaClass();

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("CREDITOR_ID", baseEntitySaving.getRespondentId());
        params.addValue("ENTITY_ID", baseEntitySaving.getId());
        params.addValue("OPERATION_ID", baseEntitySaving.getOperation() != null? baseEntitySaving.getOperation().getId(): OperType.UPDATE.getId());
        params.addValue("BATCH_ID", baseEntitySaving.getBatchId());
        params.addValue("SYSTEM_DATE", SqlJdbcConverter.convertToSqlTimestamp(LocalDateTime.now()));
        params.addValue("X_RECEIVED", BaseEntityUtils.getReceivedAsString(baseEntitySaving.getMetaClass(), baseEntitySaving.getReceived()));

        if (shiftReportDate) {
            params.addValue("NEW_REPORT_DATE", SqlJdbcConverter.convertToSqlDate(baseEntitySaving.getReportDate()));
            params.addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(baseEntitySaving.getOldReportDate()));
        }
        else
            params.addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(baseEntitySaving.getReportDate()));

        StringBuilder fixedColumns = new StringBuilder();
        fixedColumns.append("BATCH_ID = :BATCH_ID,\n");
        fixedColumns.append("OPERATION_ID = :OPERATION_ID,\n");
        fixedColumns.append("SYSTEM_DATE = :SYSTEM_DATE,\n");
        fixedColumns.append("X_RECEIVED = :X_RECEIVED\n");

        StringBuilder varColumns = new StringBuilder();

        if (!shiftReportDate) {
            for (String attrName : baseEntitySaving.getAttributeNames()) {
                MetaAttribute metaAttribute = baseEntitySaving.getMetaAttribute(attrName);
                String columnName = metaAttribute.getColumnName();

                BaseValue baseValue = baseEntitySaving.getBaseValue(attrName);
                if (baseValue == null || !baseValue.isChanged())
                    continue;

                Object value = null;
                if (baseValue.getValue() != null) {
                    value = convertBaseValueToSqlValue(metaClass.getSchemaData(), metaAttribute, baseValue);
                    params.addValue(columnName, value);
                }

                varColumns.append(",\n");
                varColumns.append(columnName);

                // выжное замечание если передавать по сетам null через map то выходит ошибка
                // так что обнуление столбцов нужно делать через запрос (то есть мы не фиксируем его в мэпе)
                if (value != null)
                   varColumns.append(" = :").append(columnName);
                else
                    varColumns.append(" = null");
            }
        }

        StringBuilder updateQuery = new StringBuilder();
        updateQuery.append("UPDATE ")
                .append(metaClass.getSchemaData()).append(".").append(metaClass.getTableName()).append("\n");
        updateQuery.append("  set ");
        updateQuery.append(fixedColumns);

        if (shiftReportDate) {
            updateQuery.append(",\n");
            updateQuery.append("REPORT_DATE = :NEW_REPORT_DATE\n");
        }
        else
            updateQuery.append(varColumns).append("\n");

        updateQuery.append("where CREDITOR_ID = :CREDITOR_ID\n");
        updateQuery.append("  and ENTITY_ID = :ENTITY_ID\n");
        updateQuery.append("  and REPORT_DATE = :REPORT_DATE\n");

        int count = npJdbcTemplate.update(updateQuery.toString(), params);

        if (count == 0)
            throw new UsciException(String.format("Ошибка завершения DML операций по таблице %s и сущности %s",
                   String.join(".", metaClass.getSchemaXml(), metaClass.getTableName()), baseEntitySaving));
    }

    private void deleteBaseEntityEntry(final BaseEntity baseEntity, LocalDate reportDate, final BaseEntityHolder baseEntityHolder) {
        MetaClass metaClass = baseEntity.getMetaClass();

        for (String attrName : baseEntity.getAttributeNames()) {
            MetaAttribute metaAttribute = baseEntity.getMetaAttribute(attrName);
            MetaType metaType = metaAttribute.getMetaType();

            BaseValue childBaseValue = baseEntity.getBaseValue(attrName);

            if (childBaseValue == null || childBaseValue.getValue() == null || !metaType.isComplex() || metaAttribute.isReference())
                continue;

            if (metaType.isSet()) {
                BaseSet childBaseSet = (BaseSet) childBaseValue.getValue();
                for (BaseValue childBaseSetValue : childBaseSet.getValues()) {
                    BaseEntity childBaseSetEntity = (BaseEntity) childBaseSetValue.getValue();
                    MetaClass childSetMetaClass = childBaseSetEntity.getMetaClass();
                    if (childBaseSetEntity.getReportDate().equals(reportDate) && !childSetMetaClass.isSearchable()) {
                        childBaseSetEntity.setParentInfo(baseEntity, metaAttribute);
                        deleteBaseEntityEntry(childBaseSetEntity, reportDate, baseEntityHolder);
                    }
                }
            }
            else {
                BaseEntity childBaseEntity = (BaseEntity) childBaseValue.getValue();
                MetaClass childMetaClass = childBaseEntity.getMetaClass();
                if (childBaseEntity.getReportDate().equals(reportDate) && !childMetaClass.isSearchable()) {
                    childBaseEntity.setParentInfo(baseEntity, metaAttribute);
                    deleteBaseEntityEntry(childBaseEntity, reportDate, baseEntityHolder);
                }
            }
        }

        baseEntityHolder.putAsDeletedBaseEntityEntry(baseEntity);

        long entityRecords = entityService.countBaseEntityEntries(baseEntity);
        if (entityRecords == 1 && metaClass.isSearchable())
            baseEntityHolder.putAsDeletedBasEntityHub(new EavHub(baseEntity, null));
    }

    /**
     * удаляет (физическое удаление) историю сущности(запись на отчетную дату) из БД
     * */
    private void deleteBaseEntityEntryFromDb(final BaseEntity baseEntity) {
        MetaClass metaClass = baseEntity.getMetaClass();

        String deleteQuery = String.format("delete from %s.%s where CREDITOR_ID = ? and REPORT_DATE = ? and ENTITY_ID = ?",
                metaClass.getSchemaData(), metaClass.getTableName());

        int tableCount = jdbcTemplate.update(deleteQuery, baseEntity.getRespondentId(),
                SqlJdbcConverter.convertToSqlDate(baseEntity.getReportDate()), baseEntity.getId());
        if (tableCount == 0)
            throw new UsciException(String.format("Ошибка delete из таблицы %s", String.join(".", metaClass.getSchemaData(), metaClass.getTableName())));
    }

    /**
     * удаление сущности подразумевает помечание ее как удален
     * то есть физическое удаление из БД не происходит, делается update поля operation=delete
     * перед удалением проверяется чтобы на сущность не было ссылок из других сущностей
     * удалять нужно только корень в дереве, это значит все поддерево кроме корня остается не изменным
     * */
    @Override
    public BaseEntity markBaseEntityAsDeleted(final BaseEntity baseEntitySaving, BaseEntityHolder baseEntityHolder) {
        Objects.requireNonNull(baseEntitySaving.getId(), "Сущность для удаления не найдена");

        if (entityService.hasReference(baseEntitySaving))
            throw new UsciException("Запись не может быть удалена. На запись ссылаются другие объекты");

        MetaClass metaClass = baseEntitySaving.getMetaClass();
        if (!metaClass.isSearchable())
            throw new UsciException("Удаление доступно только по идентифицирумым сущностям");

        BaseEntity baseEntityApplied = baseEntityLoadService.loadByMaxReportDate(baseEntitySaving, baseEntitySaving.getReportDate());
        baseEntityApplied.setOperation(OperType.DELETE);
        baseEntityApplied.setBatchId(baseEntitySaving.getBatchId());

        baseEntityHolder.putAsUpdatedBaseEntityEntry(baseEntityApplied);

        // удаляю все ключи по данной сущности из хаба
        if (metaClass.isSearchable())
            baseEntityHolder.putAsDeletedBasEntityHub(new EavHub(baseEntityApplied, null));

        // отправляю изменения в БД, также сущность из батча зафиксируем в схеме EAV_XML
        storeInDb(baseEntityHolder, baseEntitySaving);

        return baseEntityApplied;
    }

    /**
     * помечает сущность как закрытой
     * закрытие доступно только для внутреннего парсера бсб
     * парсер для респондентов запрещает использование данного статуса
     * обычно закрытие используется для справочников
     * вцелом под закрытием необходимо понимать что мы помечаем запись как не активной
     * то есть ссылаться на такую запись и проводить над ней операций после закрытия запрещается
     * */
    @Override
    public BaseEntity closeBaseEntity(BaseEntity baseEntitySaving, BaseEntityHolder baseEntityHolder) {
        Objects.requireNonNull(baseEntitySaving.getId(), "Сущность для закрытия не найдена");

        LocalDate minReportDate = baseEntityDateSerivce.getMinReportDate(baseEntitySaving);
        if (minReportDate.compareTo(baseEntitySaving.getReportDate()) >= 0)
            throw new UsciException("Дата закрытия не может быть одинаковой или раньше даты открытия");

        // проверяю запись на дату закрытия, если же запись уже имеется то делаю update иначе инсерт
        // следует учесть что мы выполняем закрытие по сущности из БД а не той которая пришла в батче
        boolean exists = entityService.existsBaseEntity(baseEntitySaving, baseEntitySaving.getReportDate());

        BaseEntity baseEntityLoaded = baseEntityLoadService.loadByMaxReportDate(baseEntitySaving, baseEntitySaving.getReportDate());
        if (baseEntityLoaded.isClosed())
            throw new UsciException("Закрытие допустимо только по открытым сущностям");

        baseEntityLoaded.setOperation(OperType.CLOSE);
        baseEntityLoaded.setBatchId(baseEntitySaving.getBatchId());

        if (exists)
            baseEntityHolder.putAsUpdatedBaseEntityEntry(baseEntityLoaded);
        else {
            // ставлю отчетную дату из батча чтобы заинсертить на эту дату
            baseEntityLoaded.setReportDate(baseEntitySaving.getReportDate());
            baseEntityHolder.putAsNewBaseEntityEntry(baseEntityLoaded);
        }

        // отправляю изменения в БД, также сущность из батча зафиксируем в схеме EAV_XML
        storeInDb(baseEntityHolder, baseEntitySaving);

        return baseEntitySaving;
    }

    /**
     * открытие ранее закрытой сущности
     * открытие доступно только для внутреннего парсера бсб
     * парсер для респондентов запрещает использование данного статуса
     * */
    @Override
    public BaseEntity openBaseEntity(BaseEntity baseEntitySaving, BaseEntityHolder baseEntityHolder) {
        Objects.requireNonNull(baseEntitySaving.getId(), "Сущность для открытия не найдена");

        LocalDate minReportDate = baseEntityDateSerivce.getMinReportDate(baseEntitySaving);
        if (minReportDate.compareTo(baseEntitySaving.getReportDate()) >= 0)
            throw new IllegalStateException("Дата открытия не может быть одинаковой или раньше даты открытия");

        BaseEntity baseEntityLoaded = baseEntityLoadService.loadByMaxReportDate(baseEntitySaving, baseEntitySaving.getReportDate());
        if (baseEntityLoaded.getOperation() != OperType.CLOSE)
            throw new UsciException("Открытие допустимо только по ранее закрытым сущностям");

        baseEntityLoaded.setOperation(OperType.OPEN);
        baseEntityLoaded.setBatchId(baseEntitySaving.getBatchId());

        // проверяю запись на дату закрытия, если же запись уже имеется то делаю update иначе инсерт
        // следует учесть что мы выполняем закрытие по сущности из БД а не той которая пришла в батче
        boolean exists = entityService.existsBaseEntity(baseEntitySaving, baseEntitySaving.getReportDate());

        if (exists)
            baseEntityHolder.putAsUpdatedBaseEntityEntry(baseEntityLoaded);
        else {
            // ставлю отчетную дату из батча чтобы заинсертить на эту дату
            baseEntityLoaded.setReportDate(baseEntitySaving.getReportDate());
            baseEntityHolder.putAsNewBaseEntityEntry(baseEntityLoaded);
        }

        // отправляю изменения в БД, также сущность из батча зафиксируем в схеме EAV_XML
        storeInDb(baseEntityHolder, baseEntitySaving);

        return baseEntitySaving;
    }

    /**
     * метод конвертирует EAV значение в значение реляционной таблицы
     * для комплексных сетов берем только id сущностей, конвертируем коллекцию в обычный массив
     * для обычных сетов берем массив значений
     * для скалярных сущностей берем только id самой сущности
     * для скалярных примитивных значений берем само значение привиденное к jdbc
     * */
    private Object convertBaseValueToSqlValue(final String schema, final MetaAttribute metaAttribute, final BaseValue baseValue) {
        MetaType metaType = metaAttribute.getMetaType();

        Object value;
        if (metaType.isSet()) {
            Object array = null;


            MetaSet childMetaSet = (MetaSet) metaType;
            BaseSet childBaseSet = (BaseSet) baseValue.getValue();

            if (metaType.isComplex())
                array = childBaseSet.getValues().stream()
                        .map(childBaseValue -> ((BaseEntity) childBaseValue.getValue()).getId())
                        .distinct().toArray();
            else
                array = childBaseSet.getValues().stream()
                        .map(childBaseValue -> MetaDataType.convertMetaValueToSqlValue(((MetaValue) childMetaSet.getMetaType()).getMetaDataType(), childBaseValue))
                        .distinct().toArray();


            Connection conn = null;
            DataSource dataSource = jdbcTemplate.getDataSource();
            OracleConnection oraConn = null;
            try {
                // особенность Oracle, для создания массива обязательно пользоваться createARRAY а не createArrayOf
                // также необходимо получить соединение с базой spring утилитой иначе получим только прокси обьект
                conn = DataSourceUtils.getConnection(dataSource);
                oraConn = conn.unwrap(OracleConnection.class);

                value = oraConn.createARRAY(String.join(".", schema, metaAttribute.getColumnType()), array);
            } catch (SQLException e) {

                // ловим exception и конвертируем в unchecked чтобы везде не добавлять try catch
                DataSourceUtils.releaseConnection(conn, dataSource);
                throw new UsciException(e.getMessage());
            }
            finally {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        } else if (metaType.isComplex())
            value = ((BaseEntity) baseValue.getValue()).getId();
        else
            value = MetaDataType.convertMetaValueToSqlValue(((MetaValue) metaAttribute.getMetaType()).getMetaDataType(), baseValue.getValue());

        return value;
    }

    private EntityExtJsTreeJson entityToJson(BaseEntity entity, String title, String code, MetaAttribute attr, boolean asRoot) {
        MetaClass metaClass = entity.getMetaClass();

        if (title == null) {
            title = code;
        }

        EntityExtJsTreeJson entityJsonTree = new EntityExtJsTreeJson()
                .setTitle(title)
                .setName(code)
                .setValue(entity.getId())
                .setDictionary(metaClass.isDictionary())
                .setRoot(asRoot)
                .setClassId(metaClass.getId())
                .setMetaType("META_CLASS")
                .setClassId(metaClass.getId())
                .setIconCls("folder")
                .setSimple(false)
                .setArray(false)
                .setOpenDate(entity.getReportDate())
                .setKey(attr != null && attr.isKey())
                .setRequired(attr != null && attr.isRequired());

        if (entity.getOperation() != null && entity.getOperation().equals(OperType.CLOSE))
            entityJsonTree.setCloseDate(entity.getReportDate());

        if (attr != null && attr.getMetaType().isComplex())
            entityJsonTree.setRefClassId(((MetaClass) attr.getMetaType()).getId());

        for (String attrName : metaClass.getAttributeNames()) {
            MetaAttribute metaAttribute = metaClass.getMetaAttribute(attrName);
            MetaType metaType = metaAttribute.getMetaType();

            String attrTitle = StringUtils.isEmpty(metaAttribute.getTitle())? attrName: metaAttribute.getTitle();

            BaseValue baseValue = entity.getBaseValue(attrName);

            if (baseValue == null)// || baseValue.getValue() == null)
                continue;

            if (metaType.isComplex() && !metaType.isSet())
                if (baseValue.getValue() == null){
                    entityJsonTree.addChild(new EntityExtJsTreeJson()
                            .setTitle(attrTitle)
                            .setName(attrName)
                            .setValue(null)
                            .setDictionary(metaAttribute.isReference())
                            .setClassId(((MetaClass) metaType).getId())
                            .setMetaType("META_CLASS")
                            .setSimple(false)
                            .setArray(false)
                            .setOpenDate(entity.getReportDate())
                            .setKey(metaAttribute != null && metaAttribute.isKey())
                            .setRequired(metaAttribute != null && metaAttribute.isRequired())
                            .setIconCls("folder")
                            .setChildren(null));
                } else
                entityJsonTree.addChild(entityToJson((BaseEntity) (baseValue.getValue()), attrTitle, attrName, metaAttribute, false));
            else if ((metaType.isComplex() && metaType.isSet()) || (!metaType.isComplex() && metaType.isSet()))
                if (baseValue.getValue() == null){
                    entityJsonTree.addChild(new EntityExtJsTreeJson()
                            .setTitle(attrTitle)
                            .setName(attrName)
                            .setValue(1.0)
                            .setLeaf(false)
                            .setKey(metaAttribute.isKey())
                            .setCumulative(metaAttribute.isCumulative())
                            .setSimple(!metaAttribute.getMetaType().isComplex())
                            .setDictionary(metaAttribute.isReference())
                            .setClassId(0L)
                            .setRefClassId(((MetaClass) ((MetaSet) metaAttribute.getMetaType()).getMetaType()).getId())
                            .setRefType(getMetaTypeStr(metaType))
                            .setArray(true)
                            .setMetaType("META_SET")
                            .setIconCls("folder")
                            .setChildren(null));
                } else
                entityJsonTree.addChild(setToJson((BaseSet) baseValue.getValue(), attrTitle, attrName, metaAttribute));
            else if (!metaType.isComplex() && !metaType.isSet()) {
                if (baseValue.getOldBaseValue() != null ) {
                    entityJsonTree.addChild(new EntityExtJsTreeJson()
                            .setTitle(attrTitle)
                            .setName(attrName)
                            .setClassId(metaClass.getId())
                            .setValue(getBaseValueAsString(baseValue.getOldBaseValue().getValue()))
                            .setNewValue(getBaseValueAsString(baseValue.getValue()))
                            .setSimple(true)
                            .setArray(false)
                            .setLeaf(true)
                            .setMetaType(((MetaValue) metaType).getMetaDataType().toString())
                            .setKey(metaAttribute.isKey())
                            .setRequired(metaAttribute.isRequired())
                            .setIconCls("file"));
                } else {
                    entityJsonTree.addChild(new EntityExtJsTreeJson()
                            .setTitle(attrTitle)
                            .setName(attrName)
                            .setClassId(metaClass.getId())
                            .setValue(baseValue.getValue() == null ? null : getBaseValueAsString(baseValue.getValue()))
                            .setSimple(true)
                            .setArray(false)
                            .setLeaf(true)
                            .setMetaType(((MetaValue) metaType).getMetaDataType().toString())
                            .setKey(metaAttribute.isKey())
                            .setRequired(metaAttribute.isRequired())
                            .setIconCls("file"));
                }
            }
        }

        return entityJsonTree;
    }

    private EntityExtJsTreeJson setToJson(BaseSet set, String title, String code, MetaAttribute attr) {
        MetaType metaType = set.getMetaType();

        if (title == null) {
            title = code;
        }

        String typeCode = null;
        if (metaType.isSet()) {
            typeCode = ((MetaSet) ((MetaSet) metaType).getMetaType()).getMetaDataType().toString();
        }

        // сеты отображаем как папочки, в качестве значения выводим количество записей
        EntityExtJsTreeJson setJsonTree = new EntityExtJsTreeJson()
                .setTitle(title)
                .setName(code)
                .setValue(set.getValueCount())
                .setKey(attr.isKey())
                .setCumulative(attr.isCumulative())
                .setSimple(!attr.getMetaType().isComplex())
                .setDictionary(attr.isReference())
                .setArray(true)
                .setTypeCode(typeCode)
                .setMetaType("META_SET")
                .setIconCls("folder");

        if (metaType.isComplex())
            setJsonTree.setRefClassId(((MetaClass) metaType).getId());

        setJsonTree.setRefType(getMetaTypeStr(metaType));

        int i = 0;

        if (metaType.isComplex()) {
            for (BaseValue baseSetValue : set.getValues()) {
                if (baseSetValue == null || baseSetValue.getValue() == null)
                    continue;

                BaseEntity childSetEntity = (BaseEntity) baseSetValue.getValue();

                // в заголовках элементов сета выводим порядковый номер в сете
                setJsonTree.addChild(entityToJson(childSetEntity, "[" + i + "]", "[" + i + "]", null, false));

                i++;
            }
        } else if (metaType.isSet()) {
            for (BaseValue baseSetValue : set.getValues()) {
                if (baseSetValue == null || baseSetValue.getValue() == null)
                    continue;

                MetaType metaTypeSet = ((MetaSet) metaType).getMetaType();
                String jsonValue = null;

                if (((MetaSet) metaTypeSet).getMetaDataType() == MetaDataType.DATE)
                    jsonValue = ((LocalDate) baseSetValue.getValue()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                else if (((MetaSet) metaTypeSet).getMetaDataType() == MetaDataType.DATE_TIME)
                    jsonValue = ((LocalDateTime) baseSetValue.getValue()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
                else
                    jsonValue = String.valueOf(baseSetValue.getValue().toString());


                setJsonTree.addChild(new EntityExtJsTreeJson()
                        .setTitle("[" + i + "]")
                        .setName("[" + i + "]")
                        .setValue(jsonValue)
                        .setSimple(true)
                        .setArray(false)
                        .setMetaType(((MetaSet) metaTypeSet).getMetaDataType().toString())
                        .setLeaf(true)
                        .setIconCls("file"));

                i++;
            }
        } else {
            for (BaseValue baseSetValue : set.getValues()) {
                if (baseSetValue == null || baseSetValue.getValue() == null)
                    continue;

                String jsonValue = null;
                if (((MetaValue) metaType).getMetaDataType() == MetaDataType.DATE)
                    jsonValue = ((LocalDate) baseSetValue.getValue()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                else if (((MetaValue) metaType).getMetaDataType() == MetaDataType.DATE_TIME)
                    jsonValue = ((LocalDateTime) baseSetValue.getValue()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
                else
                    jsonValue = String.valueOf(baseSetValue.getValue().toString());

                setJsonTree.addChild(new EntityExtJsTreeJson()
                        .setTitle("[" + i + "]")
                        .setName("[" + i + "]")
                        .setValue(jsonValue)
                        .setSimple(true)
                        .setArray(true)
                        .setMetaType(((MetaValue) metaType).getMetaDataType().toString())
                        .setLeaf(true)
                        .setIconCls("file"));

                i++;
            }
        }

        return setJsonTree;
    }

    private Object getBaseValueAsString(Object value) {
        if (value == null)
            return null;

        if (value instanceof LocalDate)
            return ((LocalDate)value).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        else if (value instanceof LocalDateTime)
            return ((LocalDateTime)value).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        else if (value instanceof Double) {
            DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(Locale.US);
            formatSymbols.setDecimalSeparator('.');

            DecimalFormat df = new DecimalFormat("#.0#", formatSymbols);

            return df.format(value);
        }
        else if (value instanceof Boolean)
            return value;

        return value;
    }

    private String getMetaTypeStr(MetaType metaType) {
        if (metaType.isSet())
            return "META_SET";
        else if (metaType.isComplex()) {
            return "META_CLASS";
        } else {
            return ((MetaValue)metaType).getMetaDataType().name();
        }
    }

    private void generateBaseEntityId(BaseEntityHolder baseEntityHolder) {
        Map<MetaClass, List<BaseEntity>> newBaseEntities = baseEntityHolder.getNewBaseEntities();

        // необходимо заранее присвоить сущностям ID перед insert так как есть зависимость сущностей
        for (MetaClass metaClass : newBaseEntities.keySet()) {
            List<BaseEntity> baseEntities = newBaseEntities.get(metaClass);

            for (BaseEntity baseEntity : baseEntities) {
                if (baseEntity.getId() != null && metaClass.isSearchable())
                    throw new UsciException("Попытка присвоить ID сущности у которой уже есть ID");
                if (baseEntity.isApproved()) {
                        List<BaseEntity> baseEntityXmlList = baseEntityHolder.getBaseEntityPairs().get(baseEntity.getUuid().toString());
                        baseEntity.setId(baseEntityXmlList.get(0).getEavXmlId());
                        for (BaseEntity baseEntityXml: baseEntityXmlList) {
                            baseEntityXml.setId(baseEntity.getId());
                        }
                } else {
                    if (baseEntity.getId() == null) {
                        Long newId = generateBaseEntityId(metaClass);
                        baseEntity.setId(newId);
                    }
                    // необходимо присвоить id сущности которая затем инсертится в схему EAV_XML
                    List<BaseEntity> baseEntityXmlList = baseEntityHolder.getBaseEntityPairs().get(baseEntity.getUuid().toString());
                    for (BaseEntity baseEntityXml: baseEntityXmlList) {
                        baseEntityXml.setId(baseEntity.getId());
                    }
                }
            }
        }
    }

    private void addBaseEntitySavingAppliedPair(BaseEntity baseEntitySaving, BaseEntity baseEntityFound, BaseEntityHolder baseEntityHolder) {
        for (String attrName : baseEntitySaving.getAttributeNames()) {
            BaseValue baseValueSaving = baseEntitySaving.getBaseValue(attrName);
            BaseValue baseValueFound = baseEntityFound.getBaseValue(attrName);

            // пропускает закрытые теги на новые сущности <tag/>
            if (baseValueSaving.getValue() == null)
                continue;

            MetaAttribute metaAttribute = baseEntitySaving.getMetaAttribute(attrName);
            MetaType metaType = metaAttribute.getMetaType();

            if (metaType.isComplex()) {
                if (metaType.isSet()) {
                    BaseSet childBaseSet = (BaseSet) baseValueSaving.getValue();
                    BaseSet childBaseSetFound = (BaseSet) baseValueFound.getValue();
                    for (BaseValue childBaseValue : childBaseSet.getValues()) {
                        for (BaseValue childBaseValueFound : childBaseSetFound.getValues()) {
                            if (((BaseEntity) childBaseValue.getValue()).equalsByKey((BaseEntity) childBaseValueFound.getValue()))
                                addBaseEntitySavingAppliedPair((BaseEntity) childBaseValue.getValue(), (BaseEntity) childBaseValueFound.getValue(), baseEntityHolder);
                        }
                    }
                } else
                    addBaseEntitySavingAppliedPair((BaseEntity) baseValueSaving.getValue(), (BaseEntity) baseValueFound.getValue(), baseEntityHolder);
            }

        }
        baseEntityHolder.saveBaseEntitySavingAppliedPair(baseEntitySaving, baseEntityFound);
    }

    private void processBackMoveDateCheck(BaseEntity baseEntityApplied, BaseEntity baseEntityLoaded,
                                 List<MetaAttribute> updatedAttributes, BaseEntityHolder baseEntityHolder) {
        MetaClass metaClass = baseEntityApplied.getMetaClass();

        // получаю все отчетные даты после даты батча
        // записи должны быть отсортированы по отчетной дате в возрастающем порядке
        List<BaseEntityDate> baseEntityDates = baseEntityDateDao.find(baseEntityApplied, baseEntityApplied.getReportDate());

        // если нет записей после отчетной даты батча то значит батчи грузят последовательно
        if (baseEntityDates.size() == 0)
            return;

        // подгружаю записи сущности за последующие отчетные даты от батча
        // подгружаю все записи сущности из БД от даты батча
        Map<LocalDate, BaseEntity> baseEntityEntriesDb = baseEntityLoadService.loadBaseEntityEntries(baseEntityApplied, baseEntityApplied.getReportDate())
                .stream().collect(Collectors.toMap(BaseEntity::getReportDate, o -> o));

        // добавляю запись сущности в коллекцию для обогащения
        if (!baseEntityEntriesDb.containsKey(baseEntityLoaded.getReportDate()))
            baseEntityEntriesDb.put(baseEntityLoaded.getReportDate(), baseEntityLoaded);

        List<BaseEntity> processedBaseEntityEntries = new ArrayList<>();

        // все записи данной сущности от даты батча анализирую
        if (baseEntityEntriesDb.containsKey(baseEntityApplied.getReportDate())) {
            BaseEntity baseEntityEntry = baseEntityEntriesDb.get(baseEntityApplied.getReportDate()).clone();
            if (markBaseEntityChanges(baseEntityApplied, baseEntityEntry).size() > 0) {
                baseEntityHolder.putAsUpdatedBaseEntityEntry(baseEntityApplied);
            }
            baseEntityHolder.getMovedBaseEntityDates().remove(baseEntityApplied);
        }
    }

}
