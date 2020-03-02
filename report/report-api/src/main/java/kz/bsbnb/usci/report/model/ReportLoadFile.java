package kz.bsbnb.usci.report.model;

import kz.bsbnb.usci.model.persistence.Persistable;

public class ReportLoadFile extends Persistable {
    private static final long serialVersionUID = 1L;

    private ReportLoad reportLoad;
    private String filename;
    private String mimeType;
    private String path;

    public ReportLoad getReportLoad() {
        return reportLoad;
    }

    public void setReportLoad(ReportLoad reportLoad) {
        this.reportLoad = reportLoad;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

