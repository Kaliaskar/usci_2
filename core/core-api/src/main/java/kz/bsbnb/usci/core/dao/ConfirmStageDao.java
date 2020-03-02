package kz.bsbnb.usci.core.dao;

import kz.bsbnb.usci.model.respondent.ConfirmStage;

/**
 * @author Jandos Iskakov
 */

public interface ConfirmStageDao {

    void insertConfirmStage(ConfirmStage confirmStage);

    ConfirmStage getConfirmStage(Long id);

    ConfirmStage getLastConfirmStage(Long confirmId);

}
