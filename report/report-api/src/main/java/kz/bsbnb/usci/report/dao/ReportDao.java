package kz.bsbnb.usci.report.dao;

import kz.bsbnb.usci.report.model.*;
import kz.bsbnb.usci.report.model.json.InputParametersJson;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;


public interface ReportDao {

    List<Report>  loadReportList(String reportType);

    Report getReport(long reportId);

    List<ReportInputParameter> loadReportInputParameter(Long reportId);

    List<ExportType> loadExportType(Long reportId);

    List<ReportLoad> loadReportLoads(Long userId);

    void insertOrUpdateReportLoad(ReportLoad reportLoad);

    ClosableRS getDataFromProcudeure (long userId, String procedureName, List<InputParametersJson> parameterList) throws SQLException;

    List<ValuePair> getValueListFromStoredProcedure(String procedureName, String tableName, long userId);

    Date loadInputDateValue(String procedureName, long userId);

    void callRunReport(long userId, long isEscape, List<InputParametersJson> parameterList);

    List<UserReport> getReportList(List<Long> respondentIds, List<Long> productIds);

    byte[] getReportFile(Long id);
}
