package kz.bsbnb.usci.mail.service.impl;

import kz.bsbnb.usci.mail.config.MailConfig;
import kz.bsbnb.usci.mail.dao.MailMessageDao;
import kz.bsbnb.usci.mail.dao.MailTemplateDao;
import kz.bsbnb.usci.mail.model.*;
import kz.bsbnb.usci.mail.model.dto.MailMessageDto;
import kz.bsbnb.usci.mail.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Aidar Mirzahanov
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

@Service
public class MailServiceImpl implements MailService {
    private static final Logger logger = LoggerFactory.getLogger(MailServiceImpl.class);

    private final MailTemplateDao mailTemplateDao;
    private final MailConfig mailConfig;
    private final MailMessageDao mailMessageDao;
    private final JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();

    public MailServiceImpl(MailTemplateDao mailTemplateDao,
                           MailConfig mailConfig,
                           MailMessageDao mailMessageDao) {
        this.mailTemplateDao = mailTemplateDao;
        this.mailConfig = mailConfig;
        this.mailMessageDao = mailMessageDao;
    }

    @PostConstruct
    private void init() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", mailConfig.getHost());
        properties.put("mail.smtp.user", mailConfig.getEmail());

        javaMailSender.setJavaMailProperties(properties);
        javaMailSender.setHost(properties.getProperty("mail.smtp.host"));
        javaMailSender.setUsername(properties.getProperty("mail.smtp.user"));
    }

    private boolean isTemplateEnabledForUser(MailTemplate mailTemplate, Long receiverUserId) {
        // данный шаблон не настраивается пользователем, а высылается в обязательном порядке
        if (mailTemplate.getTypeId() == MailConfigurationTypes.OBLIGATORY) {
            return true;
        }

        // если шаблон настраивается пользователем, проверяются настройки
        return mailTemplateDao.isTemplateEnabledForUser(mailTemplate.getId(), receiverUserId);
    }

    @Async
    @Override
    public void sendMail(MailMessageDto mailMessageDto) {
        MailMessage mailMessage = new MailMessage();
        mailMessage.setParams(mailMessageDto.getParams());
        mailMessage.setReceiver(mailMessageDto.getReceiver());
        mailMessage.setCreationDate(LocalDateTime.now());
        mailMessage.setMailTemplate(mailTemplateDao.getTemplate(mailMessageDto.getMailTemplate()));
        mailMessage.setStatus(MailMessageStatus.PROCESSING);

        logger.info("Обработка сообщения к email {}", mailMessage);

        mailMessageDao.insertMailMessage(mailMessage);

        boolean isSending = isTemplateEnabledForUser(mailMessage.getMailTemplate(), mailMessage.getReceiver().getUserId());

        if (!mailMessage.getReceiver().isActive()) {
            isSending = false;
        }

        if (isSending) {
            sendMail(prepareMail(mailMessage));
            mailMessage.setStatus(MailMessageStatus.SENT);
        } else {
            mailMessage.setStatus(MailMessageStatus.REJECTED_BY_USER_SETTINGS);
        }

        mailMessage.setSendingDate(LocalDateTime.now());

        mailMessageDao.updateMailMessage(mailMessage);
    }

    @Override
    public List<UserMailTemplate> getUserMailTemplateList(Long userId) {
        return mailTemplateDao.getUserMailTemplateList(userId);
    }

    @Override
    public void saveUserMailTemplateList(List<UserMailTemplate> userMailTemplateList) {
        mailTemplateDao.saveUserMailTemplateList(userMailTemplateList);
    }

    private Mail prepareMail(MailMessage mailMessage) {
        MailTemplate mailTemplate = mailMessage.getMailTemplate();

        return new Mail()
            .setSenderEmail(mailConfig.getEmail())
            .setReceiverEmail(mailMessage.getReceiver().getEmailAddress())
            .setSubject(replaceParamsWithValues(mailTemplate.getSubject(), mailMessage.getParams()))
            .setText(replaceParamsWithValues(mailTemplate.getText(), mailMessage.getParams()));
    }

    private String replaceParamsWithValues(String templateText, Map<String, String> params) {
        for (String paramKey : params.keySet())
            templateText = templateText.replace("${" + paramKey + "}", params.get(paramKey));

        return templateText;
    }

    private void sendMail(Mail mail) {
        if (mail.getReceiverEmail().toLowerCase().endsWith("liferay.com") ||
            mail.getReceiverEmail().toLowerCase().endsWith("oldbank.kz")) {
            return;
        }

        logger.info("Отправка сообщения по email {}", mail);

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            mimeMessage.setFrom(new InternetAddress(mail.getSenderEmail()));
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(mail.getReceiverEmail()));

            mimeMessage.setSubject(mail.getSubject());
            mimeMessage.setText(mail.getText(), "utf-8", "html");

            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            logger.error("Ошибка отправки письма по email", e);
        }
    }

}
