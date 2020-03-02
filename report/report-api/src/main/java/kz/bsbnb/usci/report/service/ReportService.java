package kz.bsbnb.usci.report.service;

import kz.bsbnb.usci.report.model.*;
import kz.bsbnb.usci.report.model.json.InputParametersJson;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

public interface ReportService {

    List<Report> loadReportList(String reportType);

    Report getReport(long reportId);

    List<ReportInputParameter> loadReportInputParameter(Long reportId);

    List<ExportType> loadExportType(Long reportId);

    List<ReportLoad> loadReportLoads(Long userId);

    ClosableRS getDataFromProcudeure (long userId, String procedureName, List<InputParametersJson> parameterList)  throws SQLException;

    List<ValuePair> getValueListFromStoredProcedure(String procedureName, String tableName, long userId);

    ReportLoad loadStarted(Long userId, Report report, List<InputParametersJson> parameterList);

    void loadFinished(ReportLoad reportLoad);

    Date loadInputDateValue(String procedureName, long userId);

    CustomDataSource getDataSourceFromStoredProcedure(long userId, String procedureName, List<InputParametersJson> parameterList) throws SQLException;

    byte[] templateReportToXls (long userId, Report report, List<InputParametersJson> parameterValues);

    byte[] jasperToXls(long userId, Report report, List<InputParametersJson> parameterValues);

    void callRunReport(long userId, long isEscape, List<InputParametersJson> parameterList);

    List<UserReport> getReportList(List<Long> respondentIds, List<Long> productIds);

    byte[] getReportFile(Long id);
}
