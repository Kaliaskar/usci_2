package kz.bsbnb.usci.core.controller;

import kz.bsbnb.usci.core.service.ConfirmService;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.respondent.*;
import kz.bsbnb.usci.util.json.ext.ExtJsList;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author Jandos Iskakov
 * @author Olzhas Kaliaskar
 */

@RestController
@RequestMapping(value = "/confirm")
public class ConfirmRestController {
    private final ConfirmService confirmService;

    public ConfirmRestController(ConfirmService confirmService) {
        this.confirmService = confirmService;
    }

    @GetMapping(value = "/updateConfirm")
    public Map<String, Object> updateConfirm(@RequestParam(name = "respondentId") Long respondentId,
                                             @RequestParam(name = "reportDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
                                             @RequestParam(name = "productId") Long productId,
                                             @RequestParam(name = "userId") Long userId) {
        return confirmService.updateConfirm(respondentId, reportDate, productId, userId);
    }

    @GetMapping(value = "/getConfirmList")
    public ExtJsList getConfirmJson(@RequestParam(required = false, name = "userId") Long userId,
                                    @RequestParam(required = false, name = "isNb") Boolean isNb,
                                    @RequestParam(required = false, name = "respondentIds") List<Long> respondentIds,
                                    @RequestParam(required = false, name = "reportDate")
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
                                    @RequestParam(name = "page") Integer pageIndex,
                                    @RequestParam(name = "limit") Integer pageSize) {
        return confirmService.getConfirmJsonList(userId, isNb, respondentIds, reportDate, pageIndex, pageSize);
    }

    @GetMapping(value = "createConfirmDocument")
    public byte[] createConfirmDocument(@RequestParam(name = "confirmId") long confirmId,
                                        @RequestParam(name = "userId") long userId,
                                        @RequestParam(name = "userName") String userName) {
        return confirmService.createConfirmDocument(confirmId, userId, userName);
    }

    @GetMapping(value = "getConfirmDocument")
    public byte[] getConfirmDocument(@RequestParam(name = "id") long id) {
        return confirmService.getConfirmDocument(id);
    }

    @GetMapping(value = "/getConfirmJson")
    public ConfirmJson getConfirmJson(@RequestParam Long confirmId) {
        return confirmService.getConfirmJson(confirmId);
    }

    @GetMapping(value = "getConfirmStageJsonList")
    public List<ConfirmStageJson> getConfirmStageJsonList(@RequestParam(name = "confirmId") Long confirmId) {
        return confirmService.getConfirmStageJsonList(confirmId);
    }

    @GetMapping(value = "getConfirmDocumentHash")
    public String getConfirmDocumentHash(@RequestParam(name = "confirmId") Long confirmId,
                                         @RequestParam(name = "userId") Long userId,
                                         @RequestParam(name = "userName") String userName) {
        return confirmService.getConfirmDocumentHash(confirmId, userId, userName);
    }

    @PostMapping(value = "approve")
    public void approve(@RequestParam(name = "userId") Long userId,
                        @RequestParam(name = "confirmId") Long confirmId,
                        @RequestParam(name = "documentHash") String documentHash,
                        @RequestParam(name = "signature") String signature,
                        @RequestParam(name = "userName") String userName) {
        confirmService.approve(userId, confirmId, documentHash, signature, userName);
    }

    @PostMapping(value = "addConfirmMessage")
    public void addConfirmMessage(@RequestParam(name = "confirmId") Long confirmId,
                                  @RequestParam(name = "userId") Long userId,
                                  @RequestParam(name = "text") String text,
                                  @RequestParam("file") MultipartFile[] files) {
        ConfirmMessage confirmMessage = new ConfirmMessage();
        confirmMessage.setConfirmId(confirmId);
        confirmMessage.setUserId(userId);
        confirmMessage.setSendDate(LocalDateTime.now());
        confirmMessage.setText(text);

        for (MultipartFile uploadedFile : files) {
            ConfirmMessageFile messageFile = new ConfirmMessageFile();

            try {
                messageFile.setContent(uploadedFile.getBytes());
                messageFile.setFileName(uploadedFile.getOriginalFilename());
            } catch (IOException e) {
                throw new UsciException("Ошибка загрузки прикрепленного документа сообщения", e);
            }

            confirmMessage.getFiles().add(messageFile);
        }

        confirmService.addConfirmMessage(confirmMessage);
    }

    @GetMapping(value = "/getMessagesByConfirmId")
    public List<ConfirmMessageJson> getMessagesByConfirmId(@RequestParam(name = "confirmId") Long confirmId) {
        return confirmService.getMessagesByConfirmId(confirmId);
    }

    @GetMapping(value = "getMessageFileContent")
    public byte[] getMessageFileContent(@RequestParam(name = "fileId") Long fileId) {
        return confirmService.getMessageFileContent(fileId);
    }

    @GetMapping(value = "getFilesByMessageId")
    public List<ConfirmMsgFileJson> getFilesByMessageId(@RequestParam(name = "messageId") Long messageId) {
        return confirmService.getFilesByMessageId(messageId);
    }

}
