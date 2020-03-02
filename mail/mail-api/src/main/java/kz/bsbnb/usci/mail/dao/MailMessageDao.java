package kz.bsbnb.usci.mail.dao;

import kz.bsbnb.usci.mail.model.MailMessage;

/**
 * @author Baurzhan Makhanbetov
 * @author Jandos Iskakov
 */

public interface MailMessageDao {

    void updateMailMessage(MailMessage mailMessage);

    MailMessage insertMailMessage(MailMessage mailMessage);

}
