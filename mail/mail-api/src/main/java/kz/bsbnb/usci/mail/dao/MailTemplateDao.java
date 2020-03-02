package kz.bsbnb.usci.mail.dao;

import kz.bsbnb.usci.mail.model.MailTemplate;
import kz.bsbnb.usci.mail.model.UserMailTemplate;

import java.util.List;

/**
 * @author Jandos Iskakov
 * @author Ernur Bakash
 */

public interface MailTemplateDao {

    MailTemplate getTemplate(String code);

    boolean isTemplateEnabledForUser(Long templateId, long userId);

    List<UserMailTemplate> getUserMailTemplateList(Long userId);

    void saveUserMailTemplateList(List<UserMailTemplate> userMailTemplateList);

}
