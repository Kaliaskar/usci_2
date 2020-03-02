package kz.bsbnb.usci.report.crosscheck;

import kz.bsbnb.usci.model.persistence.Persistable;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CrossCheck extends Persistable implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private LocalDateTime dateBegin;
    private LocalDateTime dateEnd;
    private Long creditorId;
    private LocalDate reportDate;
    private Long status;
    private String statusname;
    private String username;
    private String creditorname;

    public CrossCheck() {
    }

    public CrossCheck(Long id) {
        this.id = id;
    }

    public LocalDateTime getDateBegin() {
        return dateBegin;
    }

    public void setDateBegin(LocalDateTime dateBegin) {
        this.dateBegin = dateBegin;
    }

    public LocalDateTime getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(LocalDateTime dateEnd) {
        this.dateEnd = dateEnd;
    }

    public void setCreditorId(Long creditorId) {
        this.creditorId = creditorId;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public void setStatus(Long status) {
        this.status = status;
    }

    public Long getStatus() {
        return status;
    }

    public void setStatusName(String statusname){
        this.statusname = statusname;
    }

    public String getStatusName() {
        return status==null ? "" : statusname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setCreditorName(String creditorname) {
        this.creditorname = creditorname;
    }

    public String getCreditorName() {
         return creditorname;
        // return creditor==null ? "" : creditor.getName();
    }


    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CrossCheck)) {
            return false;
        }
        CrossCheck other = (CrossCheck) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "crosscheck[ id=" + id + " ]";
    }
}
