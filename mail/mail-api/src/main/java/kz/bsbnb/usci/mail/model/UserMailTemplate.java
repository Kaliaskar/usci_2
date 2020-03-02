package kz.bsbnb.usci.mail.model;

import kz.bsbnb.usci.model.persistence.Persistable;

/**
 * @author Ernur Bakash
 */

public class UserMailTemplate extends Persistable {
    private Long userId;
    private MailTemplate mailTemplate;
    private boolean enabled = false;

    public UserMailTemplate() {
        super();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public MailTemplate getMailTemplate() {
        return mailTemplate;
    }

    public void setMailTemplate(MailTemplate mailTemplate) {
        this.mailTemplate = mailTemplate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "UserMailTemplate{" +
                "id=" + id +
                '}';
    }

}
