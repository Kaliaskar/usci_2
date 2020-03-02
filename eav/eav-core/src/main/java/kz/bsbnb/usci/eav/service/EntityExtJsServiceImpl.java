package kz.bsbnb.usci.eav.service;

import kz.bsbnb.usci.core.client.RespondentClient;
import kz.bsbnb.usci.eav.EavSqlConverter;
import kz.bsbnb.usci.eav.model.base.*;
import kz.bsbnb.usci.eav.model.meta.*;
import kz.bsbnb.usci.eav.model.meta.json.EntityExtJsTreeJson;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.persistence.Persistable;
import kz.bsbnb.usci.model.respondent.Respondent;
import kz.bsbnb.usci.model.ui.UiConfig;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import kz.bsbnb.usci.util.json.ext.ExtJsList;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static kz.bsbnb.usci.eav.model.Constants.ORACLE_OPTIMAL_FETCH_SIZE;
import static kz.bsbnb.usci.model.Constants.NBK_AS_RESPONDENT_ID;

/**
 * @author Jandos Iskakov
 */

@Service
public class EntityExtJsServiceImpl implements EntityExtJsService {
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final MetaClassService metaClassService;
    private final BaseEntityLoadService entityLoadService;
    private final BaseEntityProcessor baseEntityProcessor;
    private final RespondentClient respondentClient;
    private final BaseEntityLoadXmlService baseEntityLoadXmlService;
    private final ProductService productService;

    public EntityExtJsServiceImpl(JdbcTemplate jdbcTemplate,
                                  MetaClassService metaClassService,
                                  BaseEntityLoadService entityLoadService,
                                  BaseEntityProcessor baseEntityProcessor,
                                  RespondentClient respondentClient,
                                  BaseEntityLoadXmlService baseEntityLoadXmlService,
                                  ProductService productService) {
        jdbcTemplate.setFetchSize(ORACLE_OPTIMAL_FETCH_SIZE);
        this.npJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.metaClassService = metaClassService;
        this.entityLoadService = entityLoadService;
        this.baseEntityProcessor = baseEntityProcessor;
        this.respondentClient = respondentClient;
        this.baseEntityLoadXmlService = baseEntityLoadXmlService;
        this.productService = productService;
    }

    @Override
    public EntityExtJsTreeJson getEntityData(Long entityId, Long metaClassId, Long respondentId, LocalDate reportDate, boolean asRoot) {
        MetaClass metaClass = metaClassService.getMetaClass(metaClassId);
        BaseEntity entity = entityLoadService.loadByMaxReportDate(new BaseEntity(entityId, metaClass, respondentId), reportDate);

        return new EntityExtJsTreeJson()
                .setText(".")
                .addChild(entityToJson(entity, metaClass.getClassTitle(), metaClass.getClassName(), null, asRoot));
    }

    @Override
    public EntityExtJsTreeJson getEntityDataMaintenance(Long id) {
        return baseEntityLoadXmlService.loadBaseEntity(id);
    }

    @Override
    public ExtJsList getEntityListMaintenance(Long batchId) {
        return new ExtJsList(baseEntityLoadXmlService.loadEntityForApproval(batchId));
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

        if (entity.getOperation() != null && (entity.getOperation().equals(OperType.CLOSE) || entity.getOperation().equals(OperType.DELETE)))
            entityJsonTree.setCloseDate(entity.getReportDate());

        if (attr != null && attr.getMetaType().isComplex())
            entityJsonTree.setRefClassId(((MetaClass) attr.getMetaType()).getId());

        for (String attrName : metaClass.getAttributeNames()) {
            MetaAttribute metaAttribute = metaClass.getMetaAttribute(attrName);
            MetaType metaType = metaAttribute.getMetaType();

            String attrTitle = StringUtils.isEmpty(metaAttribute.getTitle())? attrName: metaAttribute.getTitle();

            BaseValue baseValue = entity.getBaseValue(attrName);

            if (baseValue == null || baseValue.getValue() == null)
                continue;

            if (metaType.isComplex() && !metaType.isSet())
                entityJsonTree.addChild(entityToJson((BaseEntity) (baseValue.getValue()), attrTitle, attrName, metaAttribute, false));
            else if ((metaType.isComplex() && metaType.isSet()) || (!metaType.isComplex() && metaType.isSet()))
                entityJsonTree.addChild(setToJson((BaseSet) baseValue.getValue(), attrTitle, attrName, metaAttribute));
            else if (!metaType.isComplex() && !metaType.isSet()) {
                entityJsonTree.addChild(new EntityExtJsTreeJson()
                        .setTitle(attrTitle)
                        .setName(attrName)
                        .setClassId(metaClass.getId())
                        .setValue(getBaseValueAsString(baseValue.getValue()))
                        .setSimple(true)
                        .setArray(false)
                        .setLeaf(true)
                        .setMetaType(((MetaValue) metaType).getMetaDataType().toString())
                        .setKey(metaAttribute.isKey())
                        .setRequired(metaAttribute.isRequired())
                        .setIconCls("file"));
            }
        }

        return entityJsonTree;
    }

    @Override
    public EntityExtJsTreeJson searchEntity(Long respondentId, Long metaClassId, LocalDate reportDate, EntityExtJsTreeJson rootNode) {
        MetaClass metaClass = metaClassService.getMetaClass(metaClassId);

        //todo
        //need to
        if (metaClass.isDictionary()) {

            //List<Long> products = productService.getProductIdsByMetaClassId(metaClassId);

            if (metaClass.getProductId().equals(9L)) {
                respondentId = NBK_AS_RESPONDENT_ID;
            }
        }

       /* if (metaClass.isDictionary())
            respondentId = Constants.NBK_AS_RESPONDENT_ID;*/

        BaseEntity baseEntity = getBaseEntityFromJsonTree(respondentId, reportDate, rootNode, metaClass);

        // необходимо найти саму сущность
        baseEntity = baseEntityProcessor.prepareBaseEntity(baseEntity, respondentId);
        if (baseEntity.getId() == null)
            throw new UsciException("Сущность с данными параметрами не найдена");

        // теперь подгружаем данные полностью по сущности
        baseEntity = entityLoadService.loadByMaxReportDate(baseEntity, reportDate);
        if (baseEntity == null)
            throw new UsciException("Отсутствуют данные за отчетный период по сущности");

        return new EntityExtJsTreeJson()
                .setText(".")
                .addChild(entityToJson(baseEntity, metaClass.getClassTitle(), metaClass.getClassName(), null, true));
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

    /**
     * Собирает сущность из дерева ExtJS
     */
    private BaseEntity getBaseEntityFromJsonTree(Long respondentId, LocalDate reportDate, EntityExtJsTreeJson node, MetaClass metaClass) {
        BaseEntity baseEntity = new BaseEntity(metaClass, reportDate, respondentId);

        for (EntityExtJsTreeJson childNode: node.getChildren()) {
            MetaAttribute metaAttribute = metaClass.getMetaAttribute(childNode.getName());
            MetaType metaType = metaAttribute.getMetaType();

            if (metaType.isComplex()) {
                if (metaType.isSet()) {
                    MetaClass childSetMeta = (MetaClass) ((MetaSet) metaType).getMetaType();

                    BaseSet childBaseSet = new BaseSet(childSetMeta);
                    baseEntity.put(metaAttribute.getName(), new BaseValue(childBaseSet));

                    for (EntityExtJsTreeJson childSetNode : childNode.getChildren()) {
                        BaseEntity childBaseSetEntity = getBaseEntityFromJsonTree(respondentId, reportDate, childSetNode, childSetMeta);
                        childBaseSet.put(new BaseValue(childBaseSetEntity));
                    }
                }
                else {
                    BaseEntity childBaseEntity = getBaseEntityFromJsonTree(respondentId, reportDate, childNode, (MetaClass) metaType);
                    baseEntity.put(metaAttribute.getName(), new BaseValue(childBaseEntity));
                }
            }
            else {
                if (metaType.isSet())
                    throw new UsciException("Простые множества в роли ключей не поддерживаются");

                Object value = MetaDataType.getCastObject(((MetaValue) metaType).getMetaDataType(), String.valueOf(childNode.getValue()));
                baseEntity.put(metaAttribute.getName(), new BaseValue(value));
            }
        }

        return baseEntity;
    }

    /**
     * в дереве ExtJs отправляем данные отформатированные
      */
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

    @Override
    public List<Map<String, Object>> loadEntityEntries(MetaClass metaClass, LocalDate reportDate, Long userId, boolean isNb) {
        List<Long> respondentIds = new ArrayList<>();

        if (!isNb) {
            Respondent respondent = respondentClient.getRespondentByUserId(userId, isNb);
            if (respondent == null)
                throw new UsciException("Ошибка определения респондента пользователя");
            respondentIds.add(respondent.getId());
            respondentIds.add(NBK_AS_RESPONDENT_ID);
        }

        Map<Long, UiConfig> uiConfigMap = metaClassService.getUiConfigs();

        StringBuilder query = new StringBuilder();
        query.append("select ENTITY_ID,\n");
        query.append("REPORT_DATE,\n");
        query.append("CREDITOR_ID,\n");
        query.append("OPERATION_ID,\n");
        query.append("OPEN_DATE,\n");
        query.append("CLOSE_DATE\n");

        for (MetaAttribute attribute : metaClass.getAttributes()) {
            MetaType metaType = attribute.getMetaType();

            if (metaType.isSet())
                continue;

            query.append(",\n");

            /*if (metaType.isComplex()) {
                MetaClass childMetaClass = (MetaClass) metaType;

                String compMetaSql = "(select $tableAlias.$displayColumnName\n" +
                        "  from EAV_DATA.$tableName $tableAlias\n" +
                        " where $tableAlias.ENTITY_ID = t1.$columnValue\n" +
                        "   and $tableAlias.REPORT_DATE = (select max($subTableAlias.REPORT_DATE)\n" +
                        "                        from EAV_DATA.$tableName $subTableAlias\n" +
                        "                       where $subTableAlias.ENTITY_ID = $tableAlias.ENTITY_ID\n" +
                        "                         and $subTableAlias.CREDITOR_ID = $tableAlias.CREDITOR_ID\n" +
                        "                         and $subTableAlias.REPORT_DATE <= t1.REPORT_DATE)) as $attributeName\n";

                compMetaSql = compMetaSql.replace("$tableName", childMetaClass.getTableName());
                compMetaSql = compMetaSql.replace("$tableAlias", childMetaClass.getTableName().toLowerCase());
                compMetaSql = compMetaSql.replace("$subTableAlias", "sub_" + childMetaClass.getTableName());
                compMetaSql = compMetaSql.replace("$columnValue", attribute.getColumnName());
                compMetaSql = compMetaSql.replace("$attributeName", attribute.getName());

                UiConfig uiConfig = uiConfigMap.get(childMetaClass.getId());
                compMetaSql = compMetaSql.replace("$displayColumnName", uiConfig.getDisplayField());

                query.append(compMetaSql);
            }
            else*/
                query.append(attribute.getColumnName())
                        .append(" as ")
                        .append(attribute.getName());
        }

        query.append("\n");
        query.append("from ");
        query.append("(select d.report_date as open_date\n" +
                "   , case when d.operation_id in (3,5)\n" +
                "  then d.report_date\n" +
                "    else nvl(lead(d.report_date) over (partition by creditor_id, entity_id order by report_date)\n" +
                "    , to_date('01.01.2100', 'dd.mm.yyyy'))\n" +
                "    end as close_date\n" +
                "    , d.*\n" +
                "    from ");
        query.append(String.join(".", metaClass.getSchemaData(), metaClass.getTableName()));
        query.append(" d)\n");
        //Мы выводим все значения, у которых выполняется два условия одновременно
        //1) дата начала действия меньше или равна отчетной дате;
        //2) дата закрытия пустая или больше отчетной даты.
        query.append("where open_date <> close_date and\n" +
                " open_date <= :REPORT_DATE and\n" +
                " (close_date is null OR close_date > :REPORT_DATE )\n");

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (!isNb) {
            query.append("and creditor_id in (:CREDITOR_IDS)\n");
            params.addValue("CREDITOR_IDS", respondentIds);
        }

        params.addValue("REPORT_DATE", SqlJdbcConverter.convertToSqlDate(reportDate));

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query.toString(), params);

        //addOpenCloseDates(rows);

        // привожу все наименование атрибутов в нижний регистр
        // для этого создаю по новой мэп и заливаю обратно
        List<Map<String, Object>> list = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            if (SqlJdbcConverter.convertToLocalDate((Timestamp) row.get("CLOSE_DATE")).isEqual(LocalDate.of(2100,1,1))) {
                row.put("CLOSE_DATE", null);
            }
            Map<String, Object> lowerCaseRow = new HashMap<>();

            for (String columnName : row.keySet())
                lowerCaseRow.put(columnName.toLowerCase(), row.get(columnName));

            list.add(lowerCaseRow);
        }

        return list;
    }

    private void addOpenCloseDates(List<Map<String, Object>> rows) {
        Map<String, Object> prev = null;
        for (Map<String, Object> row : rows) {
            LocalDate currRepDate = SqlJdbcConverter.convertToLocalDate(row.get("REPORT_DATE"));
            row.put("OPEN_DATE", currRepDate);

            // операция закрытия
            OperType operationId = OperType.getOperType(SqlJdbcConverter.convertToShort(row.get("OPERATION_ID")));
            if (operationId == OperType.CLOSE || operationId == OperType.DELETE)
                row.put("CLOSE_DATE", currRepDate);

            if (prev != null) {
                Long currEntityId = SqlJdbcConverter.convertToLong(row.get("ENTITY_ID"));
                Long prevEntityId = SqlJdbcConverter.convertToLong(prev.get("ENTITY_ID"));

                if (currEntityId.equals(prevEntityId))
                    prev.put("CLOSE_DATE", currRepDate);
            }

            prev = row;
        }
    }

    @Override
    public byte[] exportDictionaryToMsExcel(MetaClass metaClass, LocalDate reportDate, Long userId, boolean isNb) {
        String dictTitle = metaClass.getClassTitle() != null ? metaClass.getClassTitle() : metaClass.getClassName();

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        boolean withHis = false;

        List<Map<String, Object>> entityEntries = loadEntityEntries(metaClass, reportDate, userId, isNb);

        Workbook workbook = new HSSFWorkbook();

        Sheet sheet1 = workbook.createSheet("Sheet1");

        int rowIndex = 1;

        Font font = workbook.createFont();
        font.setFontHeightInPoints((short)10);
        font.setFontName("Times New Roman");

        CellStyle infoStyle = workbook.createCellStyle();
        infoStyle.setFont(font);

        sheet1.createRow(rowIndex)
                .createCell(0)
                .setCellValue("Название справочника: " + dictTitle);

        sheet1.getRow(rowIndex).getCell(0).setCellStyle(infoStyle);

        rowIndex++;

        sheet1.createRow(rowIndex)
                .createCell(0)
                .setCellValue("По состоянию на: " + reportDate.format(dateFormat));

        sheet1.getRow(rowIndex).getCell(0).setCellStyle(infoStyle);

        rowIndex++;

        sheet1.createRow(rowIndex)
                .createCell(0)
                .setCellValue("С историей: " + (withHis ? "Да" : "Нет"));

        sheet1.getRow(rowIndex).getCell(0).setCellStyle(infoStyle);

        rowIndex++;

        sheet1.createRow(rowIndex)
                .createCell(0)
                .setCellValue("Дата формирования: " + LocalDateTime.now().format(dateTimeFormat));

        sheet1.getRow(rowIndex).getCell(0).setCellStyle(infoStyle);

        rowIndex++;

        List<MetaAttribute> attributes = metaClass.getAttributes().stream()
                .filter(metaAttribute -> !metaAttribute.getMetaType().isSet())
                .sorted(Comparator.comparing(Persistable::getId))
                .collect(toList());

        int columnIndex = 0;

        rowIndex++;

        // Заголовок
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
        headerStyle.setWrapText(true);

        Font headerFont = workbook.createFont();
        headerFont.setFontHeightInPoints((short)10);
        headerFont.setFontName("Times New Roman");
        headerFont.setBold(true);

        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        Row headerRow = sheet1.createRow(rowIndex++);
        for (MetaAttribute metaAttribute : attributes) {
            Cell headerCell = headerRow.createCell(columnIndex);

            headerCell.setCellStyle(headerStyle);
            headerCell.setCellValue(metaAttribute.getTitle());

            sheet1.setColumnWidth(columnIndex, 5000);

            columnIndex++;
        }

        Cell openDateHeaderCell = headerRow.createCell(columnIndex);
        openDateHeaderCell.setCellStyle(headerStyle);
        openDateHeaderCell.setCellValue("Дата открытия");

        sheet1.setColumnWidth(columnIndex, 5000);

        columnIndex++;

        Cell closeDateHeaderCell = headerRow.createCell(columnIndex);
        closeDateHeaderCell.setCellStyle(headerStyle);
        closeDateHeaderCell.setCellValue("Дата закрытия");

        sheet1.setColumnWidth(columnIndex, 5000);

        CellStyle valueStyle = workbook.createCellStyle();
        valueStyle.setBorderBottom(BorderStyle.THIN);
        valueStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        valueStyle.setBorderLeft(BorderStyle.THIN);
        valueStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        valueStyle.setBorderRight(BorderStyle.THIN);
        valueStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
        valueStyle.setBorderTop(BorderStyle.THIN);
        valueStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
        valueStyle.setFont(font);
        valueStyle.setWrapText(true);

        DataFormat format = workbook.createDataFormat();

        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.cloneStyleFrom(valueStyle);
        dateStyle.setDataFormat(format.getFormat("d.m.yyyy"));

        CellStyle doubleStyle = workbook.createCellStyle();
        doubleStyle.cloneStyleFrom(valueStyle);
        doubleStyle.setDataFormat(format.getFormat("#,##0.0000"));

        CellStyle intStyle = workbook.createCellStyle();
        intStyle.cloneStyleFrom(valueStyle);
        intStyle.setDataFormat(format.getFormat("#,##0"));

        for (Map<String, Object> entityEntry: entityEntries) {
            Row row = sheet1.createRow(rowIndex);

            columnIndex = 0;
            for (MetaAttribute attribute: attributes) {
                MetaType metaType = attribute.getMetaType();

                Object sqlValue = entityEntry.get(attribute.getName());

                Cell cellValue = row.createCell(columnIndex);

                columnIndex++;

                if (sqlValue == null) {
                    cellValue.setCellStyle(valueStyle);

                    continue;
                }

                Object javaValue = null;
                if (metaType.isComplex())
                    javaValue = String.valueOf(sqlValue);
                else
                    javaValue = EavSqlConverter.convertSqlValueToJavaType(attribute, sqlValue);

                if (javaValue instanceof LocalDate) {
                    cellValue.setCellValue(((LocalDate) javaValue).format(dateFormat));
                    cellValue.setCellStyle(dateStyle);
                } else if (javaValue instanceof Boolean) {
                    cellValue.setCellValue((Boolean) javaValue ? "Да" : "Нет");
                    cellValue.setCellStyle(valueStyle);
                } else if (javaValue instanceof String) {
                    cellValue.setCellValue(String.valueOf(javaValue));
                    cellValue.setCellStyle(valueStyle);
                } else if (javaValue instanceof Double) {
                    cellValue.setCellValue((Double) javaValue);
                    cellValue.setCellStyle(doubleStyle);
                } else if (javaValue instanceof Integer) {
                    cellValue.setCellValue((Integer) javaValue);
                    cellValue.setCellStyle(doubleStyle);
                }
            }

            Cell cellValueOpenDate = row.createCell(columnIndex++);
            cellValueOpenDate.setCellValue((SqlJdbcConverter.convertToLocalDate((Timestamp) entityEntry.get("open_date"))).format(dateFormat));
            cellValueOpenDate.setCellStyle(dateStyle);

            Cell cellValueCloseDate = row.createCell(columnIndex);
            cellValueCloseDate.setCellStyle(dateStyle);

            if (entityEntry.get("close_date") != null) {
                cellValueCloseDate.setCellValue((SqlJdbcConverter.convertToLocalDate((Timestamp) entityEntry.get("close_date"))).format(dateFormat));
            }

            rowIndex++;
        }

        byte[] bytes = null;

        try  {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            workbook.write(baos);
            workbook.close();

            bytes = baos.toByteArray();

            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bytes;
    }

}
