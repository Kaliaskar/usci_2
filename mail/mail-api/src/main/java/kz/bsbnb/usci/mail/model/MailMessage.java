package kz.bsbnb.usci.mail.model;

import kz.bsbnb.usci.model.adm.User;
import kz.bsbnb.usci.model.persistence.Persistable;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author Aidar Myrzahanov
 */

public class MailMessage extends Persistable {
    private static final long serialVersionUID = 1L;

    private MailMessageStatus status;
    private User receiver;
    private MailTemplate mailTemplate;
    private LocalDateTime creationDate;
    private LocalDateTime sendingDate;
    private Map<String, String> params;

    public MailMessage() {
        super();
        this.creationDate = LocalDateTime.now();
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public MailMessageStatus getStatus() {
        return status;
    }

    public void setStatus(MailMessageStatus status) {
        this.status = status;
    }

    public MailTemplate getMailTemplate() {
        return mailTemplate;
    }

    public void setMailTemplate(MailTemplate mailTemplate) {
        this.mailTemplate = mailTemplate;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getSendingDate() {
        return sendingDate;
    }

    public void setSendingDate(LocalDateTime sendingDate) {
        this.sendingDate = sendingDate;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "MailMessage{" +
                "status=" + status +
                ", receiver=" + receiver +
                ", mailTemplate=" + mailTemplate +
                ", creationDate=" + creationDate +
                ", sendingDate=" + sendingDate +
                ", params=" + params +
                '}';
    }

}
