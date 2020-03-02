package kz.bsbnb.usci.eav.meta.service;

import com.google.gson.Gson;
import kz.bsbnb.usci.eav.meta.converter.MetaClassConverter;
import kz.bsbnb.usci.eav.model.meta.*;
import kz.bsbnb.usci.eav.model.meta.json.MetaAttributeJson;
import kz.bsbnb.usci.eav.model.meta.json.MetaClassJson;
import kz.bsbnb.usci.eav.repository.MetaClassRepository;
import kz.bsbnb.usci.eav.service.MetaClassService;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.ui.UiConfig;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import kz.bsbnb.usci.util.json.ext.ExtJsTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Types;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jandos Iskakov
 */

@Service
public class MetaClassServiceImpl implements MetaClassService {
    private static final Logger logger = LoggerFactory.getLogger(MetaClassServiceImpl.class);

    private SimpleJdbcCall syncCall;
    private final JdbcTemplate jdbcTemplate;
    private final MetaClassRepository metaClassRepository;
    private final MetaClassConverter metaClassConverter;
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private static final Gson gson = new Gson();

    public MetaClassServiceImpl(JdbcTemplate jdbcTemplate,
                                MetaClassRepository metaClassRepository,
                                MetaClassConverter metaClassConverter,
                                NamedParameterJdbcTemplate npJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

        this.syncCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("EAV_CORE")
                .withCatalogName("PKG_EAV_UTILITY")
                .withProcedureName("SYNC_EAV_MODEL");

        this.metaClassRepository = metaClassRepository;
        this.metaClassConverter = metaClassConverter;
        this.npJdbcTemplate = npJdbcTemplate;
    }

    @Override
    public List<MetaClass> getMetaClasses() {
        return metaClassRepository.getMetaClasses();
    }

    @Override
    public List<MetaClassJson> getMetaClassJsonList() {
        return getMetaClassJsonList(null, null);
    }

    @Override
    public List<MetaClassJson> getMetaClassJsonListByUserId(long userId) {
        return getMetaClassJsonList(null, userId);
    }

    @Override
    public List<MetaClassJson> getDictionaryJsonList() {
        return getMetaClassJsonList(true, null);
    }

    private List<MetaClassJson> getMetaClassJsonList(Boolean dictionary, Long userId) {
        List<MetaClassJson> metaClasses = new ArrayList<>();

        String query = "select ID,\n" +
                "NAME,\n" +
                "TITLE,\n" +
                "IS_DELETED,\n" +
                "IS_DICTIONARY,\n" +
                "IS_OPERATIONAL,\n" +
                "IS_SYNC\n" +
                "from EAV_CORE.EAV_M_CLASSES mc\n";

        query += "  where mc.IS_DELETED = '0'\n";

        if (dictionary != null)
            query += " and mc.IS_DICTIONARY = :isDict\n";

        if (userId != null)
            query += " and mc.ID in (select pc.META_CLASS_ID\n" +
                    "  from USCI_ADM.USER_PRODUCT up,\n" +
                    "       EAV_CORE.EAV_M_PRODUCT_CLASS pc\n" +
                    " where up.USER_ID = :userId\n" +
                    "   and up.PRODUCT_ID = pc.PRODUCT_ID)\n";

        query += "order by ID desc";

        MapSqlParameterSource params = new MapSqlParameterSource();
        if (userId != null)
            params.addValue("userId", userId);

        if (dictionary != null)
            params.addValue("isDict", dictionary? 1: 0);

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);

        for (Map<String, Object> row : rows) {
            MetaClassJson metaClassJson = new MetaClassJson();

            metaClassJson.setId(SqlJdbcConverter.convertToLong(row.get("id")));
            metaClassJson.setDeleted(SqlJdbcConverter.convertVarchar2ToBoolean(row.get("is_deleted")));
            metaClassJson.setDictionary(SqlJdbcConverter.convertVarchar2ToBoolean(row.get("is_dictionary")));
            metaClassJson.setOperational(SqlJdbcConverter.convertVarchar2ToBoolean(row.get("is_operational")));
            metaClassJson.setName((String) row.get("name"));
            metaClassJson.setSync(SqlJdbcConverter.convertVarchar2ToBoolean(row.get("is_sync")));

            String classTitle = (String) row.get("title");
            metaClassJson.setTitle(StringUtils.isEmpty(classTitle)? metaClassJson.getName(): classTitle);

            metaClasses.add(metaClassJson);
        }

        return metaClasses;
    }

    @Override
    public MetaClass getMetaClass(String className) {
        return metaClassRepository.getMetaClass(className);
    }

    @Override
    public MetaClass getMetaClass(Long metaId) {
        return metaClassRepository.getMetaClass(metaId);
    }

    @Override
    public MetaClassJson getMetaClassJson(Long metaId) {
        MetaClass metaClass = metaClassRepository.getMetaClass(metaId);

        MetaClassJson metaClassJson = metaClassConverter.convertToDto(metaClass);
        metaClassJson.setUiConfig(getUiConfig(metaId));

        return metaClassJson;
    }

    @Override
    public Map<Long, UiConfig> getUiConfigs() {
        List<Map<String, Object>> rawUiConfig = jdbcTemplate.queryForList("select ID, UI_CONFIG\n" +
                "  from EAV_CORE.EAV_M_CLASSES\n" +
                "where IS_DICTIONARY = '1'\n" +
                "  and UI_CONFIG is not null\n");

        return rawUiConfig.stream().collect(Collectors.toMap(o -> SqlJdbcConverter.convertToLong(o.get("ID")),
                o-> gson.fromJson(String.valueOf(o.get("UI_CONFIG")), UiConfig.class)));
    }

    @Override
    public UiConfig getUiConfig(Long metaClassId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select ID, UI_CONFIG\n" +
                "  from EAV_CORE.EAV_M_CLASSES\n" +
                "where ID = ?\n", metaClassId);

        if (rows.size() != 1)
            throw new UsciException("Ошибка выборки из таблицы EAV_CORE.EAV_M_CLASSES");

        return gson.fromJson(String.valueOf(rows.get(0).get("UI_CONFIG")), UiConfig.class);
    }

    private boolean saveMetaClass(MetaClass metaClass) {
        metaClassRepository.saveMetaClass(metaClass);

        return true;
    }

    @Override
    public boolean saveMetaClass(MetaClassJson metaClassJson) {
        MetaClass meta;

        // если запись по мета классу уже есть в БД то достаю инфу
        // также запрещаю менять название мета класса
        if (metaClassJson.getId() != null) {
            meta = metaClassRepository.getMetaClass(metaClassJson.getId());
        } else {
            meta = new MetaClass();
            meta.setClassName(metaClassJson.getName());
        }

        meta.setClassTitle(metaClassJson.getTitle());
        meta.setPeriodType(PeriodType.getPeriodType(metaClassJson.getPeriodTypeId()));
        meta.setDeleted(metaClassJson.isDeleted());
        meta.setDictionary(metaClassJson.isDictionary());
        meta.setOperational(metaClassJson.isOperational());
        meta.setHashSize(metaClassJson.getHashSize());
        meta.setUiConfig(metaClassJson.getUiConfig());

        return saveMetaClass(meta);
    }

    @Override
    public boolean delMetaClass(Long metaClassId) {
        return metaClassRepository.delMetaClass(metaClassId);
    }

    /**
     * возвращает список мета атрибутов определенного узла в дереве
     * node высылается из ExtJs в формате метакласс.атрибут.aтрибут...атрибутN
     */
    @Override
    public List<ExtJsTree> getMetaClassAttributes(String node) {
        if (StringUtils.isEmpty(node))
            return Collections.emptyList();

        List<ExtJsTree> list = new ArrayList<>();

        String className = null;
        String attrName = null;

        int dotIndex = node.indexOf(".");

        if (dotIndex < 0) {
            className = node;
        } else {
            className = node.substring(0, dotIndex);
            attrName = node.substring(dotIndex + 1);
        }

        MetaClass metaClass = metaClassRepository.getMetaClass(className);
        MetaType attribute = metaClass;

        if (attrName != null) {
            attribute = metaClass.getEl(attrName);
        }

        MetaClass attrMetaClass = null;

        if (attribute.isSet()) {
            MetaSet attrMetaSet = (MetaSet) attribute;
            if (attrMetaSet.isComplex())
                attrMetaClass = (MetaClass) attrMetaSet.getMetaType();
        }
        else if (attribute.isComplex())
            attrMetaClass = (MetaClass) attribute;

        if (attrMetaClass == null)
            return list;

        for (String childAttrName : attrMetaClass.getAttributeNames()) {
            MetaAttribute childMetaAttr = attrMetaClass.getMetaAttribute(childAttrName);
            MetaType childMetaType = childMetaAttr.getMetaType();

            Map<String, Object> data = new HashMap<>();
            data.put("id", childMetaAttr.getId());
            data.put("classId", attrMetaClass.getId());
            data.put("isSync", childMetaAttr.isSync());

            ExtJsTree treeNode = new ExtJsTree()
                    .setId(node + "." + childAttrName)
                    .setText(StringUtils.isEmpty(childMetaAttr.getTitle())? childAttrName: childMetaAttr.getTitle())
                    .setData(data);

            if (childMetaType.isComplex())
                treeNode.setCls("folder");
            else {
                treeNode.setLeaf(true);
                treeNode.setCls("file");
            }

            list.add(treeNode);
        }

        return list;
    }

    @Override
    public List<MetaAttributeJson> getMetaClassAttributes(Long metaClassId) {
        MetaClass metaClass = metaClassRepository.getMetaClass(metaClassId);

        List<MetaAttributeJson> list = new ArrayList<>();

        for (MetaAttribute metaAttribute : metaClass.getAttributes()) {
            MetaAttributeJson metaAttributeJson = convertMetaAttributeJson(metaClass, metaAttribute);

            list.add(metaAttributeJson);
        }

        return list;
    }

    /**
     * Возвращает информацию по мета атрибуту мета класса
     */
    @Override
    public MetaAttributeJson getMetaAttribute(Long metaClassId, Long attributeId) {
        MetaClass metaClass = getMetaClass(metaClassId);

        MetaAttribute metaAttribute = metaClass.getAttributes().stream()
                .filter(attribute -> attribute.getId().equals(attributeId))
                .findAny()
                .get();

        return convertMetaAttributeJson(metaClass, metaAttribute);
    }

    /**
     * Конвертирует мета атриубат в DTO обьект для передачи во фронтенд
     */
    private MetaAttributeJson convertMetaAttributeJson(MetaClass metaClass, MetaAttribute metaAttribute) {
        MetaType metaType = metaAttribute.getMetaType();

        MetaAttributeJson attributeJson = new MetaAttributeJson()
                .setId(metaAttribute.getId())
                .setName(metaAttribute.getName())
                .setTitle(metaAttribute.getTitle())
                .setClassId(metaClass.getId())
                .setKeyType(metaAttribute.getKeyType())
                .setKey(metaAttribute.getKeyType() == 1)
                .setKeySet(metaAttribute.getKeySet())
                .setParentIsKey(metaAttribute.isParentIsKey())
                .setDeleted(metaAttribute.isDeleted())
                .setNullify(metaAttribute.isNullify())
                .setDictionary(metaType.isDictionary())
                .setReference(metaAttribute.isReference())
                .setRequired(metaAttribute.isRequired())
                .setCumulative(metaAttribute.isCumulative())
                .setSync(metaAttribute.isSync())
                .setSimple(!metaType.isComplex())
                .setArray(metaType.isSet())
                .setMetaType(getMetaTypeStr(metaType));

        if (!metaType.isSet()) {
            if (metaType.isComplex()) {
                MetaClass childMetaClass = (MetaClass) metaType;

                attributeJson.setType((byte) 2);
                attributeJson.setRefClassId(childMetaClass.getId());
            } else {
                MetaValue value = (MetaValue) metaType;

                attributeJson.setType((byte) 1);
                attributeJson.setTypeCode(value.getMetaDataType().name());
            }
        } else {
            MetaSet attrMetaSet = (MetaSet) metaType;
            attributeJson.setSetKeyType(attrMetaSet.getKeyType().name());

            if (attrMetaSet.getMetaType().isComplex()) {
                MetaClass childMetaClass = (MetaClass)attrMetaSet.getMetaType();

                attributeJson.setType((byte) 4);
                attributeJson.setRefClassId(childMetaClass.getId());
            } else {
                attributeJson.setType((byte) 3);
                attributeJson.setTypeCode(attrMetaSet.getMetaDataType().name());
            }
        }

        return attributeJson;
    }

    @Override
    public void delMetaAttribute(String attrPathPart, String attrPathCode) {
        int dotIndex = attrPathPart.indexOf(".");

        String className = "";
        String attrName = "";

        if (dotIndex < 0) {
            className = attrPathPart;
        } else {
            className = attrPathPart.substring(0, dotIndex);
            attrName = attrPathPart.substring(dotIndex + 1);
        }

        MetaClass rootMetaClass = getMetaClass(className);
        MetaType metaAttribute = rootMetaClass;

        if (attrName.length() > 0) {
            metaAttribute = rootMetaClass.getEl(attrName);
        }

        if (metaAttribute.isComplex()) {
            MetaClass metaClass = (MetaClass) metaAttribute;
            metaClass.removeAttribute(attrPathCode);

            saveMetaClass(metaClass);
        }
    }

    @Override
    public boolean saveMetaAttribute(MetaAttributeJson metaAttributeJson) {
        String attrPath = metaAttributeJson.getAttrPathPart();

        int dotIndex = attrPath.indexOf(".");
        String className = "";
        String attrName = "";

        if (dotIndex < 0) {
            className = attrPath;
        } else {
            className = attrPath.substring(0, dotIndex);
            attrName = attrPath.substring(dotIndex + 1);
        }

        MetaClass attrMetaClass = getMetaClass(className);
        MetaType attribute = attrMetaClass;

        if (attrName.length() > 0) {
            attribute = attrMetaClass.getEl(attrName);
        }

        MetaClass metaClass = (attribute instanceof MetaClass) ? (MetaClass) attribute :
                (attribute instanceof MetaSet) ? (MetaClass) ((MetaSet) attribute).getMetaType() :
                        null;

        // если мета атрибут новый но создаем пустой обьект
        // если же он уже есть в БД то находим его в мета классах
        // то есть извлекает из БД
        MetaAttribute metaAttribute = null;
        if (metaAttributeJson.getId() == null)
            metaAttribute = new MetaAttribute();
        else
            metaAttribute = metaClass.getAttributes().stream()
                    .filter(attr -> attr.getId().equals(metaAttributeJson.getId()))
                    .findFirst()
                    .get();

        MetaType metaType = null;

        // по типу атребута определяем тип столбца в БД Oracle и тип меты
        switch (metaAttributeJson.getType()) {
            case 1:
                MetaDataType metaDataType = MetaDataType.valueOf(metaAttributeJson.getTypeCode());
                metaType = new MetaValue(metaDataType);
                break;
            case 2:
                metaType = getMetaClass(metaAttributeJson.getRefClassId());
                break;
            case 3:
                metaType = new MetaSet(new MetaValue(MetaDataType.valueOf(metaAttributeJson.getTypeCode())));
                break;
            case 4:
                metaType = new MetaSet(getMetaClass(metaAttributeJson.getRefClassId()));
                break;
            default:
                break;
        }

        // запрещаем менять некоторые поля атрибута после создания
        if (metaAttribute.getId() == null) {
            metaAttribute.setName(metaAttributeJson.getName());
            metaAttribute.setMetaType(metaType);
            metaAttribute.setKeyType(metaAttributeJson.getKeyType());
            metaAttribute.setRequired(metaAttributeJson.isRequired());
            metaAttribute.setKeySet(metaAttributeJson.getKeySet());
            metaAttribute.setNullify(metaAttributeJson.isNullify());
            metaAttribute.setCumulative(metaAttributeJson.isCumulative());
            metaAttribute.setParentIsKey(metaAttributeJson.isParentIsKey());
            metaAttribute.setReference(metaAttributeJson.isReference());
            metaAttribute.setReportDate(LocalDate.of(2016, 10, 22));
        }

        // изменение разрешается только описание атрибута и активность
        metaAttribute.setTitle(metaAttributeJson.getTitle());
        metaAttribute.setDeleted(metaAttributeJson.isDeleted());

        metaClass.setMetaAttribute(metaAttributeJson.getName(), metaAttribute);

        saveMetaClass(metaClass);

        return false;
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

    /**
     * Вызывает пакет EAV_CORE.PKG_EAV_UTILITY
     * Пакет создает таблицы и столбцы в Oracle БД на основе метаданных
     */
    @Override
    public void syncWithDb() {
        syncCall.declareParameters(
                new SqlOutParameter("X_RETCODE", Types.VARCHAR),
                new SqlOutParameter("X_RETMSG", Types.VARCHAR));

        Map<String, Object> out = syncCall.execute();

        String errCode = String.valueOf(out.get("X_RETCODE"));
        String errMsg = String.valueOf(out.get("X_RETMSG"));

        metaClassRepository.resetCache();

        logger.info(errCode);
        logger.info(errMsg);

        if (!errCode.equals("S"))
            throw new UsciException(errMsg);
    }

}
