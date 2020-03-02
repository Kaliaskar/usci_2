package kz.bsbnb.usci.mail.service;

import kz.bsbnb.usci.mail.model.UserMailTemplate;
import kz.bsbnb.usci.mail.model.dto.MailMessageDto;

import java.util.List;

public interface MailService {

    void sendMail(MailMessageDto mailMessageDto);

    List<UserMailTemplate> getUserMailTemplateList(Long userId);

    void saveUserMailTemplateList(List<UserMailTemplate> userMailTemplateList);

}
