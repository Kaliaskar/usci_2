package kz.bsbnb.usci.report.crosscheck;


import java.io.Serializable;
import java.math.BigInteger;

/**
 *
 * @author Aidar.Myrzahanov
 */

public class CrossCheckMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String innerValue;
    private String outerValue;
    private String diff;
    private String description;
    private Long isError;
    private Message message;
    private Long crossCheckId;
    private Long messageId;
  //  private CrossCheck crossCheck;

    public CrossCheckMessage() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInnerValue() {
        return innerValue;
    }

    public void setInnerValue(String innerValue) {
        this.innerValue = innerValue;
    }

    public String getOuterValue() {
        return outerValue;
    }

    public void setOuterValue(String outerValue) {
        this.outerValue = outerValue;
    }

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean getIsError() {
        return BigInteger.ONE.equals(isError);
    }

    public boolean getIsNonCriticalError() {
        return isError != null && isError.equals(2L);
    }

    public void setIsError(Long isError) {
        this.isError = isError;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {

        this.message = message;
    }


    public void setCrossCheckId(Long crossCheckId) {
        this.crossCheckId = crossCheckId;
    }

    public Long getCrossCheckId() {
        return crossCheckId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
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
        if (!(object instanceof CrossCheckMessage)) {
            return false;
        }
        CrossCheckMessage other = (CrossCheckMessage) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "CrossCheckMessage[ id=" + id + " ]";
    }

}
