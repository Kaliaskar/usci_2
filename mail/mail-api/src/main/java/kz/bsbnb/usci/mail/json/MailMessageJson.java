package kz.bsbnb.usci.mail.json;

import kz.bsbnb.usci.mail.model.MailTemplate;
import kz.bsbnb.usci.model.persistence.Persistable;

import java.time.LocalDateTime;

/**
 * @author Zhanar the best
 */

public class MailMessageJson extends Persistable {
    private static final long serialVersionUID = 1L;

    private String status;
    private MailTemplate mailTemplate;
    private LocalDateTime creationDate;
    private LocalDateTime sendingDate;

    public MailMessageJson() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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


}

