package kz.bsbnb.usci.report.service;


import kz.bsbnb.usci.report.dao.ReportDao;
import kz.bsbnb.usci.report.exception.ReportException;
import kz.bsbnb.usci.report.model.*;
import kz.bsbnb.usci.report.model.json.InputParametersJson;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsReportConfiguration;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.sql.Date;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ReportServiceImpl implements ReportService {

    @Value("${jasper.templatePath}")
    private String templatePath;

    @Value("${jasper.reportFilesFolder}")
    private String reportFilesFolder;

    @Value("${jasper.recordBySheet}")
    private int recordBySheet;

    private final ReportDao reportDao;
    private Integer headerBottom;
    private Properties reportProperties;

    private static final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);

    public ReportServiceImpl(ReportDao reportDao) {
        this.reportDao = reportDao;

    }
    @Override
    public List<Report>  loadReportList(String reportType) {
        return reportDao.loadReportList(reportType);
    }
    @Override
    public Report getReport(long reportId) {
        return  reportDao.getReport(reportId);
    }
    @Override
    public List<ReportInputParameter> loadReportInputParameter(Long reportId) {
        return  reportDao.loadReportInputParameter(reportId);
    }
    @Override
    public List<ExportType> loadExportType(Long reportId) {
      return  reportDao.loadExportType(reportId);
    }
    @Override
    public List<ReportLoad> loadReportLoads(Long userId) {
        return  reportDao.loadReportLoads(userId);
    }

    @Override
    public ClosableRS getDataFromProcudeure (long userId, String procedureName, List<InputParametersJson> parameterList)  throws SQLException {
        return  reportDao.getDataFromProcudeure(userId, procedureName, parameterList);
    }

    @Override
    public  List<ValuePair> getValueListFromStoredProcedure(String procedureName, String tableName, long userId) {
        return  reportDao.getValueListFromStoredProcedure(procedureName, tableName, userId);
    }

    @Override
    public Date loadInputDateValue(String procedureName, long userId) {
       return  reportDao.loadInputDateValue(procedureName, userId);
    }

    public CustomDataSource getDataSourceFromStoredProcedure(long userId, String procedureName, List<InputParametersJson> parameterList) throws SQLException {
        ClosableRS rs = getDataFromProcudeure(userId, procedureName, parameterList);
        CustomDataSource customDataSource = new CustomDataSource(rs.getResultSet());
        rs.close();
        return customDataSource;
    }

    @Override
    public ReportLoad loadStarted(Long userId, Report report, List<InputParametersJson> parameterList) {
        ReportLoad load = new ReportLoad();
        load.setPortalUserId(userId);
        load.setReport(report);
        load.setStartTime(LocalDateTime.now());

        List<String> parameterCaptions = new ArrayList<>();
        List<String> parameterValues = new ArrayList<>();

        for (ReportInputParameter inParam : report.getInputParameters()) {
            parameterCaptions.add(inParam.getNameRu());
        }
        for (InputParametersJson parameter : parameterList) {
            parameterValues.add(parameter.getDisplay());
        }

        StringBuilder noteBuilder = new StringBuilder();
        for (int i = 0; i < Math.min(parameterCaptions.size(), parameterValues.size()); i++) {
            if (i > 0) {
                noteBuilder.append(", ");
            }
            noteBuilder.append(parameterCaptions.get(i));
            noteBuilder.append(" = ");
            noteBuilder.append(parameterValues.get(i));
        }
        load.setNote(noteBuilder.toString());
        return load;
    }

    @Override
    public void loadFinished(ReportLoad reportLoad) {
        reportLoad.setFinishTime(LocalDateTime.now());
        reportDao.insertOrUpdateReportLoad(reportLoad);
    }


    public CustomDataSource loadData(long userId, Report report, List<InputParametersJson> parameterValues) throws SQLException {
        if(parameterValues == null) {
            return null;
        }
        return getDataSourceFromStoredProcedure(userId, report.getProcedureName(), parameterValues);
    }

    @Override
    public byte[] templateReportToXls (long userId, Report report, List<InputParametersJson> parameterValues) {
        try {
            ReportLoad reportLoad = loadStarted(userId, report, parameterValues);
            String confPath = templatePath + report.getName() + "/export.properties";
            loadConfig(confPath);
            headerBottom = getIntegerProperty("header-bottom", 0);
            byte[] file = generatePageXlsx(report.getName(), 1, userId, report, parameterValues);

            if (file == null)
                reportLoad.setNote("Empty report");

            loadFinished(reportLoad);
            return  file;


        } catch (Exception e) {
            throw new ReportException("Ошибка формирования отчета: "  + report.getName() + " message: " + e);
        }
    }

    protected byte[] generatePageXlsx(String exportFilePrefix, int sheetNumber, long userId, Report report, List<InputParametersJson> parameterValues)  {
        ResultSet dataSource;

        ByteArrayOutputStream zbaos = new ByteArrayOutputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ClosableRS closableRS = null;

        try {
            SXSSFWorkbook workbook = new SXSSFWorkbook(10000);
            workbook.setCompressTempFiles(true);
            CreationHelper createHelper = workbook.getCreationHelper();

            Font font = workbook.createFont();
            font.setFontName("Times New Roman");
            font.setFontHeightInPoints((short) 12);

            CellStyle mainCellStyle = workbook.createCellStyle();
            mainCellStyle.setFont(font);
            mainCellStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);
            mainCellStyle.setBorderBottom(BorderStyle.THIN);
            mainCellStyle.setBorderLeft(BorderStyle.THIN);
            mainCellStyle.setBorderTop(BorderStyle.THIN);
            mainCellStyle.setBorderRight(BorderStyle.THIN);
            mainCellStyle.setWrapText(true);

            CellStyle numberDoubleCellStyle = workbook.createCellStyle();
            numberDoubleCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("# ### ### ###.###"));
            numberDoubleCellStyle.setFont(font);
            numberDoubleCellStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);
            numberDoubleCellStyle.setBorderBottom(BorderStyle.THIN);
            numberDoubleCellStyle.setBorderLeft(BorderStyle.THIN);
            numberDoubleCellStyle.setBorderTop(BorderStyle.THIN);
            numberDoubleCellStyle.setBorderRight(BorderStyle.THIN);
            numberDoubleCellStyle.setShrinkToFit(true);

            CellStyle numberIntegerCellStyle = workbook.createCellStyle();
            numberIntegerCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("# ### ### ###"));
            numberIntegerCellStyle.setFont(font);
            numberIntegerCellStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);
            numberIntegerCellStyle.setBorderBottom(BorderStyle.THIN);
            numberIntegerCellStyle.setBorderLeft(BorderStyle.THIN);
            numberIntegerCellStyle.setBorderTop(BorderStyle.THIN);
            numberIntegerCellStyle.setBorderRight(BorderStyle.THIN);
            numberIntegerCellStyle.setShrinkToFit(true);

            CellStyle dateCellStyle = workbook.createCellStyle();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd.MM.yyyy"));
            dateCellStyle.setFont(font);
            dateCellStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);
            dateCellStyle.setBorderBottom(BorderStyle.THIN);
            dateCellStyle.setBorderLeft(BorderStyle.THIN);
            dateCellStyle.setBorderTop(BorderStyle.THIN);
            dateCellStyle.setBorderRight(BorderStyle.THIN);
            dateCellStyle.setShrinkToFit(true);

            int rowIndex = 1;

            int recordsBySheet = recordBySheet;

            int recordCounter = 0;
            int sheetCounter = 1;

            closableRS = getDataFromProcudeure(userId, report.getProcedureName(), parameterValues);
            dataSource = closableRS.getResultSet();

            ResultSetMetaData rsmd = dataSource.getMetaData();

            SXSSFSheet sheet = workbook.createSheet(String.format("Report-%d", sheetCounter));
            sheet.trackAllColumnsForAutoSizing();
            long startTime = System.currentTimeMillis();

            logger.info("Start of creating excel (sheets,rows): {} ", startTime);

            while (dataSource.next()) {
                if (rowIndex == 1) {
                    Row headerRow = sheet.createRow(rowIndex);
                    for (int columnIndex = 1; columnIndex <= rsmd.getColumnCount(); columnIndex++) {
                        Cell cell = headerRow.createCell(columnIndex);
                        cell.setCellValue(rsmd.getColumnName(columnIndex));
                        cell.setCellStyle(mainCellStyle);
                    }
                    rowIndex++;
                }
                Row row = sheet.createRow(rowIndex);
                for (int columnIndex = 1; columnIndex <= rsmd.getColumnCount(); columnIndex++) {
                    if (rsmd.getColumnType(columnIndex) == Types.INTEGER) {
                        Cell cell = row.createCell(columnIndex);
                        cell.setCellValue(dataSource.getInt(columnIndex));
                        cell.setCellStyle(numberIntegerCellStyle);
                    } else if (rsmd.getColumnType(columnIndex) == Types.DOUBLE) {
                        Cell cell = row.createCell(columnIndex);
                        cell.setCellValue(dataSource.getDouble(columnIndex));
                        cell.setCellStyle(numberDoubleCellStyle);
                    } else if (rsmd.getColumnType(columnIndex) == Types.TIMESTAMP && dataSource.getDate(columnIndex) != null) {
                        Cell cell = row.createCell(columnIndex);
                        cell.setCellValue(dataSource.getDate(columnIndex));
                        cell.setCellStyle(dateCellStyle);
                    } else {
                        Object value = dataSource.getObject(columnIndex);
                        if (value == null) {
                            value = "";
                        }
                        Cell cell = row.createCell(columnIndex);
                        cell.setCellValue(value.toString());
                        cell.setCellStyle(mainCellStyle);
                    }
                }
                if (rowIndex == 500001) {
                    sheetCounter ++;
                    sheet = workbook.createSheet(String.format("Report-%d", sheetCounter));
                    sheet.trackAllColumnsForAutoSizing();
                    rowIndex = 0;
                }
                rowIndex++;
                recordCounter++;
            }

            logger.info("End of creating excel (sheets,rows): {} ", (System.currentTimeMillis() - startTime));
            startTime = System.currentTimeMillis();

            for(Sheet sh: workbook) {
                for(int i = 1; i <= rsmd.getColumnCount(); i++) {
                    sh.autoSizeColumn(i);
                }
            }

            logger.info("End of autosizing columns in excel : {} ", (System.currentTimeMillis() - startTime));
            workbook.write(baos);
            workbook.close();
            workbook.dispose();

            if (recordCounter > 0) {
                String cleanFilename = String.format("%s-%d.xlsx", exportFilePrefix, recordCounter);
                ZipOutputStream zos = new ZipOutputStream(zbaos);
                ZipEntry entry = new ZipEntry(cleanFilename);
                entry.setSize(baos.toByteArray().length);
                zos.putNextEntry(entry);
                zos.write(baos.toByteArray());
                zos.closeEntry();
                zos.close();
                return zbaos.toByteArray();
            }

            return null;

        } catch (SQLException | IOException e)   {

            if(closableRS != null) {
                closableRS.close();
            }
            logger.info("Connection closed generatePage");
            if(baos!=null) {
                try {
                    baos.close();
                } catch(Exception ee) {
                    throw new ReportException("Failed to close baos stream" + ee);
                }
            }
            if(zbaos!=null) {
                try {
                    zbaos.close();
                } catch(Exception ee) {
                    throw new ReportException("Failed to close zbaos stream" + ee);
                }
            }
        } finally {

            if(closableRS != null) {
                closableRS.close();
            }
            logger.info("Connection closed generatePage finally");
            if(baos!=null) {
                try {
                    baos.close();
                } catch(Exception e) {
                    throw new ReportException("Failed to close baos stream" + e);
                }
            }
            if(zbaos!=null) {
                try {
                    zbaos.close();
                } catch(Exception e) {
                    throw new ReportException("Failed to close zbaos stream" + e);
                }
            }
        }
        return  null;
    }

    @Override
    public byte[] jasperToXls(long userId, Report report, List<InputParametersJson> parameterValues) {
        try {
            ReportLoad reportLoad = loadStarted(userId, report, parameterValues);
            CustomDataSource dataSource = loadData(userId, report, parameterValues);
            String reportName = report.getName();
            String path = templatePath + reportName + "//";
            String jasperFilePath = path + reportName + ".jrxml";
            String resourceFilePath = path + reportName + "_RU" +".properties";
            JasperDesign design = JRXmlLoader.load(jasperFilePath);

            //JasperDesign jasperDesign = JRXmlLoader.load(jasperFilePath);
            JasperReport jasperReport = JasperCompileManager.compileReport(design);

            PropertyResourceBundle resourceBundle = new PropertyResourceBundle(new FileInputStream(resourceFilePath));
            Enumeration<String> resourceEnumeration = resourceBundle.getKeys();
            while (resourceEnumeration.hasMoreElements()) {
                String resourceStringName = resourceEnumeration.nextElement();
                String resourceStringValue = resourceBundle.getString(resourceStringName);
            }
            HashMap<String, Object> reportParameters = new HashMap<String, Object>();
            reportParameters.put("REPORT_RESOURCE_BUNDLE", resourceBundle);
            reportParameters.put("USERNAME", "");
            List<ReportInputParameter> parameters = report.getInputParameters();
            int parametersCount = parameters.size();
            for (int parameterIndex = 0; parameterIndex < parametersCount; parameterIndex++) {
                ReportInputParameter parameter = parameters.get(parameterIndex);
                InputParametersJson value = parameterValues.get(parameterIndex);
                if (value != null) {
                    String valueString;
                    if (value.getType() == "DATE") {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                        valueString = sdf.format(value.getValue());
                    } else {
                        valueString = value.toString();
                    }
                    reportParameters.put(parameter.getParameterName(), valueString);
                }
            }

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, reportParameters, dataSource);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JRXlsExporter exporterXls = new JRXlsExporter();
            exporterXls.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporterXls.setExporterOutput(new SimpleOutputStreamExporterOutput(baos));
            SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
            configuration.setOnePagePerSheet(true);
            configuration.setDetectCellType(true);
            configuration.setCollapseRowSpan(false);
            configuration.isRemoveEmptySpaceBetweenColumns();
            configuration.isRemoveEmptySpaceBetweenRows();
            configuration.setMaxRowsPerSheet(100000);
            configuration.isDetectCellType();
            exporterXls.setConfiguration(configuration);
            exporterXls.exportReport();
            byte[] bytes = baos.toByteArray();
            loadFinished(reportLoad);
            return  bytes;

        } catch (JRException jre) {
            throw new ReportException("Ошибка при формировании jasper файла: " +  jre.getStackTrace());
        } catch (IOException ioe) {
            throw new ReportException("Ошибка при загрузке файла: " +  ioe.getMessage());
        } catch (SQLException sqle) {
            throw new ReportException("Ошибка при формировании отчета: " +  sqle.getMessage());
        }
    }

    @Override
    public void callRunReport(long userId, long isEscape, List<InputParametersJson> parameterList) {
        reportDao.callRunReport(userId, isEscape, parameterList);
    }

    @Override
    public List<UserReport> getReportList(List<Long> respondentIds, List<Long> productIds) {
        return reportDao.getReportList(respondentIds,productIds);
    }

    @Override
    public byte[] getReportFile(Long id) {
        return reportDao.getReportFile(id);
    }

    protected void loadConfig(String configFilePath) {
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            return;
        }
        FileInputStream configFileStream = null;
        try {
            configFileStream = new FileInputStream(configFile);
            reportProperties = new Properties();
            reportProperties.load(configFileStream);
        } catch (IOException ioe) {

        } finally {
            if (configFileStream != null) {
                try {
                    configFileStream.close();
                } catch (Exception e) {
                }
            }
        }
    }

    protected int getIntegerProperty(String key, int defaultValue) {
        try {
            return Integer.valueOf(getReportProperty(key));
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    private String getReportProperty(String key) {
        if (reportProperties == null) {
            return "";
        }
        return reportProperties.getProperty(key, "");
    }

    private boolean isConfigurationValid() {
        return headerBottom != null;
    }

}
