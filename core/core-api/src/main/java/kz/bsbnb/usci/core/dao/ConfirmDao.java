package kz.bsbnb.usci.core.dao;

import kz.bsbnb.usci.model.respondent.Confirm;
import kz.bsbnb.usci.model.respondent.ConfirmMessage;
import kz.bsbnb.usci.model.respondent.ConfirmMessageJson;
import kz.bsbnb.usci.model.respondent.ConfirmMsgFileJson;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * @author Jandos Iskakov
 */

public interface ConfirmDao {

    long insertConfirm(Confirm confirm);

    void updateConfirm(Confirm confirm);

    Optional<Confirm> getConfirm(long respondentId, LocalDate reportDate, long productId);

    Confirm getConfirm(long confirmId);

    void addConfirmMessage(ConfirmMessage message);

    List<ConfirmMessageJson> getMessagesByConfirmId(long confirmId);

    List<ConfirmMsgFileJson> getFilesByMessage(long confirmId);

    byte[] getMessageFileContent(long fileId);

}
