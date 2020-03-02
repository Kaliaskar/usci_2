package kz.bsbnb.usci.mail.model.dto;

import kz.bsbnb.usci.model.adm.User;

import java.util.Map;

/**
 * @author Jandos Iskakov
 */

public class MailMessageDto {
    private Long statusId;
    private User receiver;
    private String mailTemplate;
    private Map<String, String> params;

    public MailMessageDto() {
        super();
    }

    public Long getStatusId() {
        return statusId;
    }

    public void setStatusId(Long statusId) {
        this.statusId = statusId;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public String getMailTemplate() {
        return mailTemplate;
    }

    public void setMailTemplate(String mailTemplate) {
        this.mailTemplate = mailTemplate;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "MailMessageDto{" +
                "statusId=" + statusId +
                ", receiver=" + receiver +
                ", mailTemplate='" + mailTemplate + '\'' +
                ", params=" + params +
                '}';
    }

}
