package kz.bsbnb.usci.report.model;

import kz.bsbnb.usci.model.persistence.Persistable;

import java.time.LocalDateTime;
import java.util.List;

public class ReportLoad extends Persistable {

    public ReportLoad() {
    }
    private static final long serialVersionUID = 1L;

    private Report report;
    private long portalUserId;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private String note;
    private String userName;
    private List<ReportLoadFile> files;

    public Report getReport() {
        return report;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public long getPortalUserId() {
        return portalUserId;
    }

    public void setPortalUserId(long portalUserId) {
        this.portalUserId = portalUserId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        String newValue = note;
        if (newValue == null) {
            newValue = "";
        }
        if (newValue.length() > 200) {
            newValue = newValue.substring(0, 200);
        }
        this.note = newValue;
    }

    public List<ReportLoadFile> getFiles() {
        return files;
    }

    public void setFiles(List<ReportLoadFile> files) {
        for (ReportLoadFile file : files) {
            file.setReportLoad(this);
        }
        this.files = files;
    }

    public String getReportName() {
        return report.getName();
    }

    public void addFile(ReportLoadFile file) {
        file.setReportLoad(this);
        this.files.add(file);
    }
}
