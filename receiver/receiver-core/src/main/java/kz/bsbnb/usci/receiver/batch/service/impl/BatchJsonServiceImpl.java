package kz.bsbnb.usci.receiver.batch.service.impl;

import kz.bsbnb.usci.core.client.RespondentClient;
import kz.bsbnb.usci.eav.client.ProductClient;
import kz.bsbnb.usci.eav.dao.BaseEntityStatusDao;
import kz.bsbnb.usci.eav.model.base.OperType;
import kz.bsbnb.usci.eav.model.core.BaseEntityStatus;
import kz.bsbnb.usci.eav.model.core.EntityStatusType;
import kz.bsbnb.usci.eav.model.meta.json.ProductJson;
import kz.bsbnb.usci.model.batch.Product;
import kz.bsbnb.usci.model.respondent.Respondent;
import kz.bsbnb.usci.model.respondent.RespondentJson;
import kz.bsbnb.usci.model.util.Text;
import kz.bsbnb.usci.model.util.TextJson;
import kz.bsbnb.usci.receiver.batch.service.BatchJsonService;
import kz.bsbnb.usci.receiver.batch.service.BatchService;
import kz.bsbnb.usci.receiver.model.BatchStatus;
import kz.bsbnb.usci.receiver.model.BatchStatusJson;
import kz.bsbnb.usci.receiver.model.BatchStatusType;
import kz.bsbnb.usci.receiver.model.json.BatchJson;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import kz.bsbnb.usci.util.client.TextClient;
import kz.bsbnb.usci.util.json.ext.ExtJsList;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Yernur Bakash
 * @author Jandos Iskakov
 */

@Service
public class BatchJsonServiceImpl implements BatchJsonService {
    private static final Logger logger = LoggerFactory.getLogger(BatchJsonServiceImpl.class);

    private final BatchService batchService;
    private final BaseEntityStatusDao baseEntityStatusDao;
    private final TextClient textClient;
    private final RespondentClient respondentClient;
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final ProductClient productClient;

    public BatchJsonServiceImpl(BatchService batchService,
                                BaseEntityStatusDao baseEntityStatusDao,
                                TextClient textClient,
                                RespondentClient respondentClient,
                                NamedParameterJdbcTemplate npJdbcTemplate,
                                ProductClient productClient) {
        this.batchService = batchService;
        this.baseEntityStatusDao = baseEntityStatusDao;
        this.textClient = textClient;
        this.respondentClient = respondentClient;
        this.npJdbcTemplate = npJdbcTemplate;
        this.productClient = productClient;
    }

    @Override
    public ExtJsList getBatchList(List<Long> respondentIds, long userId, boolean isNb, LocalDate reportDate, int pageIndex, int pageSize) {
        // если это респондент то по правам доступа он сможет увидеть только свои батчи
        if (!isNb) {
            Respondent respondent = respondentClient.getRespondentByUserId(userId, false);
            respondentIds = new ArrayList<>(Collections.singletonList(respondent.getId()));
        }

        long startTime = System.currentTimeMillis();
        String selectClause = "select b.*,\n" +
                //"(select count(ID) from EAV_DATA.EAV_ENTITY_STATUS where BATCH_ID = b.ID and STATUS_ID = :statusStored) STORED_ENTITY_COUNT,\n" +
                "(select count(1) from (select distinct ENTITY_ID from EAV_DATA.EAV_ENTITY_STATUS where BATCH_ID = b.ID and STATUS_ID = :statusStored)) STORED_ENTITY_COUNT,\n" +
                "(select count(1) from EAV_DATA.EAV_ENTITY_STATUS where BATCH_ID = b.ID and STATUS_ID = :statusError) ERROR_ENTITY_COUNT,\n" +
                "(select count(1) from EAV_DATA.EAV_ENTITY_STATUS where BATCH_ID = b.ID and STATUS_ID = :statusMaintenance) MAINTENANCE_ENTITY_COUNT\n" +
                "  from USCI_BATCH.BATCH b\n";

        String whereClause = "where IS_DISABLED = 0\n" +
                "  and CREDITOR_ID in (:respondentIds)\n";

        //if (!isNb) {
            whereClause += "  and (b.PRODUCT_ID in (select up.PRODUCT_ID from USCI_ADM.USER_PRODUCT up where USER_ID = :userId) or b.PRODUCT_ID is null)\n";
        //}

        if (reportDate != null)
            whereClause += " and REP_DATE = :reportDate\n";

        String query = selectClause + whereClause;

        query += "order by RECEIPT_DATE desc\n";

        query = "select * from (" + query + ")\n" +
                "offset :pageOffset rows fetch next :pageSize rows only\n";

        String countQuery = "select count(1) from USCI_BATCH.BATCH b\n" + whereClause;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("respondentIds", respondentIds)
                .addValue("userId", userId)
                .addValue("statusError", EntityStatusType.ERROR.getId())
                .addValue("statusStored", EntityStatusType.COMPLETED.getId())
                .addValue("statusMaintenance", EntityStatusType.MAINTENANCE.getId())
                .addValue("reportDate", SqlJdbcConverter.convertToSqlDate(reportDate))
                .addValue("pageOffset", (pageIndex - 1) * pageSize)
                .addValue("pageSize", pageSize);

        int count = npJdbcTemplate.queryForObject(countQuery, params, Integer.class);

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);
        if (rows.isEmpty())
            return new ExtJsList(Collections.emptyList());

        logger.info("End of select fot protocol: {} ", (System.currentTimeMillis() - startTime));
        List<BatchJson> batches = prepareBatchJson(rows);
        logger.info("End of select fot batch status: {} ", (System.currentTimeMillis() - startTime));
        return new ExtJsList(batches, count);
    }

    /**
     * Возвращает протокол по батчу
     */
    @Override
    public ExtJsList getBatchStatusList(long batchId, List<String> statusTypes) {
        Object[] data = null;
        if (statusTypes.size() == 3) {
            data = baseEntityStatusDao.getStatusList(batchId,
                    Arrays.asList(EntityStatusType.ERROR, EntityStatusType.COMPLETED, EntityStatusType.MAINTENANCE));
        } else if (statusTypes.size() == 1) {
            if (statusTypes.get(0).equals("ERROR")) {
                data = baseEntityStatusDao.getStatusList(batchId,
                        Arrays.asList(EntityStatusType.ERROR));
            } else if (statusTypes.get(0).equals("STORED")) {
                data = baseEntityStatusDao.getStatusList(batchId,
                        Arrays.asList(EntityStatusType.COMPLETED));
            } else if (statusTypes.get(0).equals("MAINTENANCE")) {
                data = baseEntityStatusDao.getStatusList(batchId,
                        Arrays.asList(EntityStatusType.MAINTENANCE));
            }
        } else if (statusTypes.size() == 0) {
            return new ExtJsList(Collections.emptyList());
        }

        // фильтр по протоколу для старых батчей
        List<BaseEntityStatus> entityStatusList = (List<BaseEntityStatus>) data[0];

        List<Text> textList = textClient.getTextListByType(Arrays.asList("ENTITY_STATUS", "BATCH_STATUS"));

        Map<Long, Text> textMap = textList.stream()
                .collect(Collectors.toMap(Text::getId, constant -> constant));

        List<BatchStatusJson> list = new ArrayList<>();

        for (BaseEntityStatus entityStatus : entityStatusList) {
            BatchStatusJson batchStatusJson = new BatchStatusJson()
                    .setBatchId(entityStatus.getBatchId())
                    .setEntityId(entityStatus.getEntityId())
                    .setMetaClassId(entityStatus.getMetaClassId())
                    .setEntityText(entityStatus.getEntityText())
                    .setTextCode(entityStatus.getErrorCode())
                    .setTextRu(entityStatus.getErrorMessage())
                    .setTextKz(entityStatus.getErrorMessage())
                    .setComments(entityStatus.getErrorCode());

            if (entityStatus.getStatus() != null) {
                Text textStatus = textMap.get((long) entityStatus.getStatus().getId());
                batchStatusJson.setStatus(new TextJson(textStatus));
            }

            if (entityStatus.getOperation() != null) {
                TextJson operation = new TextJson();
                operation.setId((long)entityStatus.getOperation().getId());

                if (entityStatus.getOperation() == OperType.INSERT)
                    operation.setNameRu("Добавление");
                else if (entityStatus.getOperation() == OperType.UPDATE)
                    operation.setNameRu("Обновление");

                batchStatusJson.setOperation(operation);
            }

            list.add(batchStatusJson);
        }

        // добавляю в протокол ошибки самого батча (xsd валидация, админ права)
        if (entityStatusList.isEmpty()) {
            List<BatchStatus> batchStatusList = batchService.getBatchStatusList(Collections.singletonList(batchId));

            Optional<BatchStatus> optErrorBatchStatus = batchStatusList.stream()
                    .filter(bs -> bs.getStatus().equals(BatchStatusType.ERROR))
                    .findFirst();

            if (optErrorBatchStatus.isPresent()) {
                BatchStatus errorBatchStatus = optErrorBatchStatus.get();
                Text textError = textMap.get((long) errorBatchStatus.getStatus().getId());

                list.add(new BatchStatusJson(batchId)
                        .setStatus(new TextJson(textError))
                        .setTextKz(errorBatchStatus.getText())
                        .setTextRu(errorBatchStatus.getText()));
            }
        }

        return new ExtJsList(list, Math.max(list.size(),(Integer) data[1]));
    }

    @Override
    public List<BatchJson> getPendingBatchList(List<Long> respondentIds) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("respondentIds", respondentIds)
                .addValue("statusCompleted", BatchStatusType.COMPLETED.getId())
                .addValue("statusError", BatchStatusType.ERROR.getId())
                .addValue("statusCancelled", BatchStatusType.CANCELLED.getId());

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList("select *" +
                "  from USCI_BATCH.BATCH b\n" +
                " where b.CREDITOR_ID in (:respondentIds)\n" +
                "   and b.IS_DISABLED = 0\n" +
                "   and b.STATUS_ID not in (:statusError, :statusCancelled, :statusCompleted)\n" +
                "   and (b.IS_MAINTENANCE = 0 or (b.IS_MAINTENANCE = 1 and b.IS_MAINTENANCE_APPROVED = 1))\n", params);

        if (rows.isEmpty())
            return Collections.emptyList();

        return prepareBatchJson(rows);
    }

    @Override
    public List<BatchJson> getBatchListToSign(long respondentId, long userId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("respondentId", respondentId)
                .addValue("status", BatchStatusType.WAITING_FOR_SIGNATURE.getId())
                .addValue("userId", userId);

        String query = "select *\n" +
                "  from USCI_BATCH.BATCH b\n" +
                " where b.CREDITOR_ID = :respondentId\n" +
                "   and b.STATUS_ID = :status\n" +
              //  "   and b.SIGN is null\n" +
                "   and b.PRODUCT_ID in (select up.PRODUCT_ID from USCI_ADM.USER_PRODUCT up where USER_ID = :userId)";

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);

        if (rows.isEmpty())
            return Collections.emptyList();

        return prepareBatchJson(rows);
    }

    @Override
    public List<BatchJson> getMaintenanceBatchList(List<Long> respondentIds, LocalDate reportDate, long userId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("respondentIds", respondentIds)
                .addValue("reportDate", SqlJdbcConverter.convertToSqlDate(reportDate))
                .addValue("userId", userId);

        String query = "select b.*\n" +
                "  from USCI_BATCH.BATCH b\n" +
                " where IS_MAINTENANCE = 1\n" +
                "   and CREDITOR_ID in (:respondentIds)\n" +
                "   and IS_MAINTENANCE_APPROVED = 0\n" +
                "   and IS_MAINTENANCE_DECLINED = 0\n" +
                "   and b.PRODUCT_ID in (select up.PRODUCT_ID from USCI_ADM.USER_PRODUCT up where USER_ID = :userId)\n" ;

        if (reportDate != null)
            query += "  and REP_DATE = :reportDate\n";

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);

        if (rows.isEmpty())
            return Collections.emptyList();

        return prepareBatchJson(rows);
    }

    @Override
    public List<BatchJson> getBatchListToApprove(Long productId, Long respondentId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("productId", productId)
                .addValue("respondentId", respondentId);

        String query = "select b.* from USCI_BATCH.BATCH b\n" +
                "       where b.PRODUCT_ID = :productId and b.CREDITOR_ID = :respondentId and b.ID in (select /*+parallel(16)*/ distinct m.BATCH_ID \n" +
                "        from EAV_XML.EAV_ENTITY_MAINTENANCE m \n" +
                "        where m.APPROVED = 0 and m.DECLINED = 0)" +
                "        order by b.id";

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);

        if (rows.isEmpty())
            return Collections.emptyList();

        return prepareBatchJson(rows);
    }

    /**
     * Заполняет batchJson необходимыми данными для отображения на фронте
     */
    private List<BatchJson> prepareBatchJson(List<Map<String, Object>> rows) {
        List<BatchJson> batches = new ArrayList<>();

        // продукты, респонденты извлекаем все сразу для производительности
        // так как извлечение по отдельности затратно
        Map<Long, Product> products = productClient.getProducts().stream()
                .collect(Collectors.toMap(Product::getId, o -> o));

        Map<Long, Respondent> respondents = respondentClient.getRespondentList().stream()
                .collect(Collectors.toMap(Respondent::getId, o -> o));

        Map<Long, Text> statuses = textClient.getTextListByType(Collections.singletonList("BATCH_STATUS")).stream()
                .collect(Collectors.toMap(Text::getId, o -> o));

        for (Map<String, Object> row : rows) {
            BatchJson batchJson = getBatchJsonFromJdbcMap(row);

            if (batchJson.getProductId() != null)
                batchJson.setProduct(new ProductJson(products.get(batchJson.getProductId())));

            if (batchJson.getStatusId() != null)
                batchJson.setStatus(new TextJson(statuses.get(batchJson.getStatusId())));

            if (batchJson.getRespondentId() != null)
                batchJson.setRespondent(new RespondentJson(respondents.get(batchJson.getRespondentId())));

            batches.add(batchJson);
        }

        List<BatchStatus> batchStatuses = batchService.getBatchStatusList(batches.stream()
                .map(BatchJson::getId)
                .collect(Collectors.toList()));

        Map<Long, List<BatchStatus>> batchStatusMap = new HashMap<>();

        for (BatchStatus batchStatus : batchStatuses) {
            if (!batchStatusMap.containsKey(batchStatus.getBatchId()))
                batchStatusMap.put(batchStatus.getBatchId(), new LinkedList<>());

            batchStatusMap.get(batchStatus.getBatchId()).add(batchStatus);
        }

        for (BatchJson batch : batches) {
            if (!batchStatusMap.containsKey(batch.getId()))
                continue;

            List<BatchStatus> batchStatusList = batchStatusMap.get(batch.getId());

            batchStatusList.sort((o1, o2) -> o2.getReceiptDate().compareTo(o1.getReceiptDate()));

            // заполним даты начала обработки и даты завершения согласно статусам
            for (BatchStatus batchStatus : batchStatusList) {
                if (batchStatus.getStatus() == BatchStatusType.PROCESSING) {
                    batch.setProcessBeginDate(batchStatus.getReceiptDate());
                } else if (batchStatus.getStatus() == BatchStatusType.COMPLETED || batchStatus.getStatus() == BatchStatusType.ERROR) {
                    batch.setProcessEndDate(batchStatus.getReceiptDate());
                }
            }
        }

        return batches;
    }

    private BatchJson getBatchJsonFromJdbcMap(Map<String, Object> row) {
        BatchJson batchJson = new BatchJson();
        batchJson.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
        batchJson.setRespondentId(SqlJdbcConverter.convertToLong(row.get("CREDITOR_ID")));
        batchJson.setProductId(SqlJdbcConverter.convertToLong(row.get("PRODUCT_ID")));
        batchJson.setFileName(String.valueOf(row.get("FILE_NAME")));
        batchJson.setReportDate(SqlJdbcConverter.convertToLocalDate(row.get("REP_DATE")));
        batchJson.setTotalEntityCount(SqlJdbcConverter.convertToLong(row.get("TOTAL_COUNT")));
        batchJson.setActualEntityCount(SqlJdbcConverter.convertToLong(row.get("ACTUAL_COUNT")));
        batchJson.setHash(SqlJdbcConverter.convertObjectToString(row.get("HASH")));
        batchJson.setReceiverDate(SqlJdbcConverter.convertToLocalDateTime(row.get("RECEIPT_DATE")));
        batchJson.setStatusId(SqlJdbcConverter.convertToLong(row.get("STATUS_ID")));
        batchJson.setSuccessEntityCount(SqlJdbcConverter.convertToLong(row.get("STORED_ENTITY_COUNT")));
        batchJson.setErrorEntityCount(SqlJdbcConverter.convertToLong(row.get("ERROR_ENTITY_COUNT")));
        batchJson.setMaintenanceEntityCount(SqlJdbcConverter.convertToLong(row.get("MAINTENANCE_ENTITY_COUNT")));
        batchJson.setSignInfo(String.valueOf(row.get("SIGN_INFO")));
        return batchJson;
    }

    @Override
    public byte[] getExcelFromBatch(List<BatchJson> batchList, List<String> columnNames) {
        try {
            Workbook workbook = new XSSFWorkbook();
            CreationHelper createHelper = workbook.getCreationHelper();
            Sheet sheet = workbook.createSheet("Sheet");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontName("Times New Roman");
            headerFont.setFontHeightInPoints((short) 12);

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
            headerCellStyle.setVerticalAlignment(VerticalAlignment.TOP);
            headerCellStyle.setBorderBottom(BorderStyle.THIN);
            headerCellStyle.setBorderLeft(BorderStyle.THIN);
            headerCellStyle.setBorderTop(BorderStyle.THIN);
            headerCellStyle.setBorderRight(BorderStyle.THIN);

            Font mainFont = workbook.createFont();
            mainFont.setFontName("Times New Roman");
            mainFont.setFontHeightInPoints((short) 12);

            CellStyle mainCellStyle = workbook.createCellStyle();
            mainCellStyle.setFont(mainFont);
            mainCellStyle.setVerticalAlignment(VerticalAlignment.TOP);
            mainCellStyle.setBorderBottom(BorderStyle.THIN);
            mainCellStyle.setBorderLeft(BorderStyle.THIN);
            mainCellStyle.setBorderTop(BorderStyle.THIN);
            mainCellStyle.setBorderRight(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columnNames.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnNames.get(i));
                cell.setCellStyle(headerCellStyle);
            }

            CellStyle dateCellStyle = workbook.createCellStyle();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd.MM.yyyy"));

            CellStyle dateTimeCellStyle = workbook.createCellStyle();
            dateTimeCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd.MM.yyyy HH:mm:ss"));

            int rowNum = 1;
            for(BatchJson batch: batchList) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(batch.getRespondent().getName());

                row.createCell(1).setCellValue(batch.getFileName().substring((batch.getFileName().lastIndexOf("\\")) + 1));

                if (batch.getProduct() != null) {
                    row.createCell(2).setCellValue(batch.getProduct().getName());
                } else {
                    row.createCell(2).setCellValue("");
                }

                row.createCell(3).setCellValue(batch.getStatus().getNameRu());

                if (batch.getTotalEntityCount() != null) {
                    row.createCell(4).setCellValue(batch.getTotalEntityCount());
                } else {
                    row.createCell(4).setCellValue("");
                }

                if (batch.getActualEntityCount() != null) {
                    row.createCell(5).setCellValue(batch.getActualEntityCount());
                } else {
                    row.createCell(5).setCellValue("");
                }

                if (batch.getSuccessEntityCount() != null) {
                    row.createCell(6).setCellValue(batch.getSuccessEntityCount());
                } else {
                    row.createCell(6).setCellValue("");
                }

                if (batch.getProcessBeginDate() != null) {
                    Cell startedDateCell = row.createCell(7);
                    startedDateCell.setCellValue(SqlJdbcConverter.convertToSqlTimestamp(batch.getProcessBeginDate()));
                    startedDateCell.setCellStyle(dateTimeCellStyle);
                } else {
                    row.createCell(7).setCellValue("");
                }

            }

            for(int i = 0; i < columnNames.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();

            return baos.toByteArray();

        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    @Override
    public byte[] getExcelFromProtocol(List<BatchStatusJson> protocolList, List<String> columnNames) {
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Sheet");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontName("Times New Roman");
            headerFont.setFontHeightInPoints((short) 12);

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
            headerCellStyle.setVerticalAlignment(VerticalAlignment.TOP);
            headerCellStyle.setBorderBottom(BorderStyle.THIN);
            headerCellStyle.setBorderLeft(BorderStyle.THIN);
            headerCellStyle.setBorderTop(BorderStyle.THIN);
            headerCellStyle.setBorderRight(BorderStyle.THIN);

            Font mainFont = workbook.createFont();
            mainFont.setFontName("Times New Roman");
            mainFont.setFontHeightInPoints((short) 12);

            CellStyle mainCellStyle = workbook.createCellStyle();
            mainCellStyle.setFont(mainFont);
            mainCellStyle.setVerticalAlignment(VerticalAlignment.TOP);
            mainCellStyle.setBorderBottom(BorderStyle.THIN);
            mainCellStyle.setBorderLeft(BorderStyle.THIN);
            mainCellStyle.setBorderTop(BorderStyle.THIN);
            mainCellStyle.setBorderRight(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columnNames.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnNames.get(i));
                cell.setCellStyle(headerCellStyle);
            }

            int rowNum = 1;
            for(BatchStatusJson protocol: protocolList) {
                Row row = sheet.createRow(rowNum++);

                if (protocol.getEntityId() != null) {
                    row.createCell(0).setCellValue(protocol.getEntityId());
                } else {
                    row.createCell(0).setCellValue("");
                }

                if (protocol.getEntityText() != null) {
                    row.createCell(1).setCellValue(protocol.getEntityText());
                } else {
                    row.createCell(1).setCellValue("");
                }

                if (protocol.getStatus() != null) {
                    row.createCell(2).setCellValue(protocol.getStatus().getNameRu());
                    row.createCell(3).setCellValue(protocol.getStatus().getCode());
                } else {
                    row.createCell(2).setCellValue("");
                    row.createCell(3).setCellValue("INFO");
                }

                row.createCell(4).setCellValue(protocol.getTextRu());

                row.createCell(5).setCellValue(protocol.getComments());
            }

            for(int i = 0; i < columnNames.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();

            return baos.toByteArray();

        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    @Override
    public byte[] getExcelFromQueueBatch(List<BatchJson> batchList, List<String> columnNames) {
        try {
            Workbook workbook = new XSSFWorkbook();
            CreationHelper createHelper = workbook.getCreationHelper();
            Sheet sheet = workbook.createSheet("Sheet");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontName("Times New Roman");
            headerFont.setFontHeightInPoints((short) 12);

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
            headerCellStyle.setVerticalAlignment(VerticalAlignment.TOP);
            headerCellStyle.setBorderBottom(BorderStyle.THIN);
            headerCellStyle.setBorderLeft(BorderStyle.THIN);
            headerCellStyle.setBorderTop(BorderStyle.THIN);
            headerCellStyle.setBorderRight(BorderStyle.THIN);

            Font mainFont = workbook.createFont();
            mainFont.setFontName("Times New Roman");
            mainFont.setFontHeightInPoints((short) 12);

            CellStyle mainCellStyle = workbook.createCellStyle();
            mainCellStyle.setFont(mainFont);
            mainCellStyle.setVerticalAlignment(VerticalAlignment.TOP);
            mainCellStyle.setBorderBottom(BorderStyle.THIN);
            mainCellStyle.setBorderLeft(BorderStyle.THIN);
            mainCellStyle.setBorderTop(BorderStyle.THIN);
            mainCellStyle.setBorderRight(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columnNames.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnNames.get(i));
                cell.setCellStyle(headerCellStyle);
            }

            CellStyle dateTimeCellStyle = workbook.createCellStyle();
            dateTimeCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd.MM.yyyy HH:mm:ss"));

            int rowNum = 1;
            for(BatchJson batch: batchList) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(rowNum-1);

                row.createCell(1).setCellValue(batch.getRespondent().getName());

                row.createCell(2).setCellValue(batch.getFileName().substring((batch.getFileName().lastIndexOf("\\")) + 1));

                row.createCell(3).setCellValue(batch.getStatus().getNameRu());

                Cell receivedDateCell = row.createCell(4);
                receivedDateCell.setCellValue(SqlJdbcConverter.convertToSqlTimestamp(batch.getReceiverDate()));
                receivedDateCell.setCellStyle(dateTimeCellStyle);

                row.createCell(5).setCellValue(batch.getActualEntityCount());
            }

            for(int i = 0; i < columnNames.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();

            return baos.toByteArray();

        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

}
