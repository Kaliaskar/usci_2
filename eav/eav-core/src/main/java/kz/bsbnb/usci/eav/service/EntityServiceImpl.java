package kz.bsbnb.usci.eav.service;

import com.google.common.collect.Lists;
import kz.bsbnb.usci.eav.dao.BaseEntityRegistryDao;
import kz.bsbnb.usci.eav.dao.BaseEntityStatusDao;
import kz.bsbnb.usci.eav.model.Constants;
import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseEntityRegistry;
import kz.bsbnb.usci.eav.model.core.BaseEntityStatus;
import kz.bsbnb.usci.eav.model.meta.MetaAttribute;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.eav.model.meta.MetaType;
import kz.bsbnb.usci.eav.repository.MetaClassRepository;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Artur Tkachenko
 * @author Alexandr Motov
 * @author Kanat Tulbassiev
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

@Service
public class EntityServiceImpl implements EntityService {
    private static final Logger logger = LoggerFactory.getLogger(BaseEntityProcessorImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final MetaClassRepository metaClassRepository;
    private final BaseEntityRegistryDao baseEntityRegistryDao;
    private final BaseEntityStatusDao baseEntityStatusDao;

    public EntityServiceImpl(JdbcTemplate jdbcTemplate,
                             MetaClassRepository metaClassRepository,
                             BaseEntityRegistryDao baseEntityRegistryDao,
                             BaseEntityStatusDao baseEntityStatusDao) {
        this.jdbcTemplate = jdbcTemplate;
        this.metaClassRepository = metaClassRepository;
        this.baseEntityRegistryDao = baseEntityRegistryDao;
        this.baseEntityStatusDao = baseEntityStatusDao;
    }

    /**
     * метод проверят имеется ли сущность за отчетную дату в таблице БД
     * запрос выполняется за INDEX RANGE SCAN, COST = 1
     * (сочетание ENTITY_ID, REPORT_DATE, CREDITOR_ID является PRIMARY KEY)
     * */
    @Override
    public boolean existsBaseEntity(BaseEntity baseEntity, LocalDate reportDate) {
        Objects.requireNonNull(baseEntity.getId(), String.format("Отсутствует ID у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getRespondentId(), String.format("Отсутствует ID кредитора у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getMetaClass(), String.format("Отсутствует мета класс у сущности %s", baseEntity));
        Objects.requireNonNull(reportDate, "Отчетная дата не задана");

        MetaClass metaClass = baseEntity.getMetaClass();

        String query = String.format("select ENTITY_ID from %s.%s where REPORT_DATE = ? and ENTITY_ID = ? and CREDITOR_ID = ? and rownum < 2",
                metaClass.getSchemaData(), metaClass.getTableName());

        List<Long> rows = jdbcTemplate.queryForList(query, new Object[] {SqlJdbcConverter.convertToSqlDate(reportDate),
                baseEntity.getId(), baseEntity.getRespondentId()}, Long.class);
        if (rows.size() > 1)
            throw new UsciException(String.format("Найдено более одной записи %s", baseEntity));

        return rows.size() == 1;
    }

    @Override
    public long countBaseEntityEntries(BaseEntity baseEntity) {
        Objects.requireNonNull(baseEntity.getId(), String.format("Отсутствует ID у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getRespondentId(), String.format("Отсутствует ID кредитора у сущности %s", baseEntity));
        Objects.requireNonNull(baseEntity.getMetaClass(), String.format("Отсутствует мета класс у сущности %s", baseEntity));

        MetaClass metaClass = baseEntity.getMetaClass();

        String query = String.format("select count(ENTITY_ID) from %s.%s where ENTITY_ID = ? and CREDITOR_ID = ?",
                metaClass.getSchemaData(), metaClass.getTableName());

        return jdbcTemplate.queryForObject(query, new Object[] {baseEntity.getId(), baseEntity.getRespondentId()}, Long.class);
    }

    /**
     * определяет существую ли ссылки на сущность из других сущностей
     * */
    @Override
    public boolean hasReference(BaseEntity baseEntity) {
        for (MetaClass metaClass : metaClassRepository.getMetaClasses()) {
            for (MetaAttribute metaAttribute : metaClass.getAttributes()) {
                MetaType metaType = metaAttribute.getMetaType();
                if (!metaType.isComplex() || metaType.isSet())
                    continue;

                MetaClass childMetaClass = (MetaClass) metaType;
                if (!childMetaClass.getId().equals(baseEntity.getMetaClass().getId()))
                    continue;

                List<Long> list = jdbcTemplate.queryForList(String.format("select ENTITY_ID from EAV_DATA.%s where %s = ? and rownum < 2", metaClass.getTableName(),
                        metaAttribute.getColumnName()), Long.class, baseEntity.getId());

                if (list.size() > 0)
                    return true;
            }
        }

        return false;
    }

    @Override
    public void insert(List<BaseEntityRegistry> baseEntityRegistries) {
        List<List<BaseEntityRegistry>> partitions = Lists.partition(baseEntityRegistries, Constants.OPTIMAL_BATCH_SIZE[1]);

        for (List<BaseEntityRegistry> partition: partitions) {
            if (partition.size() >= Constants.OPTIMAL_BATCH_SIZE[0] && partition.size() <= Constants.OPTIMAL_BATCH_SIZE[1])
                baseEntityRegistryDao.insert(partition);
            else {
                for (BaseEntityRegistry baseEntityRegistry : partition)
                    baseEntityRegistryDao.insert(Collections.singletonList(baseEntityRegistry));
            }
        }
    }

    @Override
    public Long addEntityStatus(BaseEntityStatus entityStatus) {
        return baseEntityStatusDao.insert(entityStatus).getId();
    }

}
