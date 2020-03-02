package kz.bsbnb.usci.mail.controller;

import kz.bsbnb.usci.mail.model.UserMailTemplate;
import kz.bsbnb.usci.mail.model.UserMailTemplateList;
import kz.bsbnb.usci.mail.model.dto.MailMessageDto;
import kz.bsbnb.usci.mail.service.MailService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Jandos Iskakov
 */

@RestController
@RequestMapping(value = "mail")
public class MailController {
    private final MailService mailService;

    public MailController(MailService mailService) {
        this.mailService = mailService;
    }

    @PutMapping(value = "sendMail")
    public void sendMail(@RequestBody MailMessageDto mailMessageDto) {
        mailService.sendMail(mailMessageDto);
    }

    @GetMapping(value = "getUserMailTemplateList")
    public List<UserMailTemplate> getUserMailTemplateList(@RequestParam(name = "userId") Long userId) {
        return mailService.getUserMailTemplateList(userId);
    }

    @PostMapping(value = "saveUserMailTemplateList")
    public void saveUserMailTemplateList(@RequestBody UserMailTemplateList userMailTemplateList) {
        mailService.saveUserMailTemplateList(userMailTemplateList.getUserMailTemplateList());
    }

}
