package kz.bsbnb.usci.receiver.validator;

import kz.bsbnb.usci.model.respondent.Respondent;
import kz.bsbnb.usci.receiver.model.Batch;

import java.util.List;

public interface BatchValidator {

    void validateUserRespondent(Batch batch, List<Respondent> userRespondents);

}
