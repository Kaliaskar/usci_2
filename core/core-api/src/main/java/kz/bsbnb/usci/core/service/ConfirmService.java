package kz.bsbnb.usci.core.service;

import kz.bsbnb.usci.model.respondent.*;
import kz.bsbnb.usci.util.json.ext.ExtJsList;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @author Jandos Iskakov
 * @author Olzhas Kaliaskar
 */

public interface ConfirmService {

    ConfirmJson getConfirmJson(long confirmId);

    Map<String, Object> updateConfirm(long respondentId, LocalDate repDate, long productId, long userId);

    ExtJsList getConfirmJsonList(long userId, Boolean isNb, List<Long> respondentIds,
                                 LocalDate reportDate, Integer pageIndex, Integer pageSize);

    void approve(long userId, long confirmId, String documentHash, String signature, String userName);

    byte[] createConfirmDocument(long confirmId, long userId, String userName);

    String getConfirmDocumentHash(long confirmId, long userId, String userName);

    List<ConfirmStageJson> getConfirmStageJsonList(long confirmId);

    void addConfirmMessage(ConfirmMessage message);

    List<ConfirmMessageJson> getMessagesByConfirmId(long confirmId);

    List<ConfirmMsgFileJson> getFilesByMessageId(long messageId);

    byte[] getConfirmDocument(long id);

    byte[] getMessageFileContent(long fileId);

}

