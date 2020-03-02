package kz.bsbnb.usci.mail.client;

import kz.bsbnb.usci.mail.model.dto.MailMessageDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author Jandos Iskakov
 */

@FeignClient(name = "mail")
public interface MailClient {

    @PutMapping(value = "/mail/sendMail")
    void sendMail(@RequestBody MailMessageDto mailMessageDto);

}
