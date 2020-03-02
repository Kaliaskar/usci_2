package kz.bsbnb.usci.receiver.service;

import kz.bsbnb.usci.core.client.UserClient;
import kz.bsbnb.usci.mail.client.MailClient;
import kz.bsbnb.usci.mail.model.MailTemplate;
import kz.bsbnb.usci.mail.model.dto.MailMessageDto;
import kz.bsbnb.usci.model.Constants;
import kz.bsbnb.usci.model.adm.User;
import kz.bsbnb.usci.receiver.model.Batch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Baurzhan Makhanbetov
 * @author Jandos Iskakov
 */

@Service
public class MailServiceImpl implements MailService {
    private static final Logger logger = LoggerFactory.getLogger(MailServiceImpl.class);

    private final UserClient userClient;
    private final MailClient mailClient;

    public MailServiceImpl(UserClient userClient, MailClient mailClient) {
        this.userClient = userClient;
        this.mailClient = mailClient;
    }

    @Override
    public void notifyBatchProcessCompleted(Batch batch) {
        logger.info("Отправка сообщения по батчу о завершений обработки батча {}", batch);

        Map<String, String> params = new HashMap<>();
        params.put("FILENAME", batch.getFormattedFileName());

        MailMessageDto mailMessageDto = new MailMessageDto();
        mailMessageDto.setReceiver(userClient.getUser(batch.getUserId()));
        mailMessageDto.setMailTemplate(MailTemplate.FILE_PROCESSING_COMPLETED);
        mailMessageDto.setParams(params);

        try {
            //TODO: временно отключен в рамках тестирования
            //mailClient.sendMail(mailMessageDto);
        } catch (Exception e) {
            logger.error(String.format("Ошибка отправки email %s", mailMessageDto), e);
        }
    }

    @Override
    @Deprecated
    public void notifyRegulatorMaintenance(Batch batch) {
        List<User> nbUsers = userClient.getNationalBankUsers(batch.getRespondentId());
        nbUsers.removeIf(nbUser -> !nbUser.isActive());

        for (User user : nbUsers) {
            Map<String, String> params = new HashMap<>();
            params.put("REPORT_DATE", batch.getReportDate().format(Constants.DATE_FORMATTER_ISO));
            params.put("ORG", batch.getRespondent().getName());
            params.put("FILE_NAME", batch.getFormattedFileName());

            MailMessageDto mailMessageDto = new MailMessageDto();
            mailMessageDto.setReceiver(user);
            mailMessageDto.setMailTemplate(MailTemplate.MAINTENANCE_REQUEST);
            mailMessageDto.setParams(params);

            try {
                //TODO: временно отключен в рамках тестирования
                //mailClient.sendMail(mailMessageDto);
            } catch (Exception e) {
                logger.error("Ошибка отправки email {}", mailMessageDto);
            }
        }
    }

}
