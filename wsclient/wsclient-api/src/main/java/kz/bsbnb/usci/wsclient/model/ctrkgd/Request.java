package kz.bsbnb.usci.wsclient.model.ctrkgd;

import kz.bsbnb.usci.model.persistence.Persistable;

import java.time.LocalDate;

public class Request extends Persistable {
    private LocalDate reportDate;
    private RequestStatus requestStatus;
    private String requestBody;
    private String responseBody;
    private Long entitiesCount;
    private String requestId;

    public Request() { super(); }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public RequestStatus getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(RequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Long getEntitiesCount() {
        return entitiesCount;
    }

    public void setEntitiesCount(Long entitiesCount) {
        this.entitiesCount = entitiesCount;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
