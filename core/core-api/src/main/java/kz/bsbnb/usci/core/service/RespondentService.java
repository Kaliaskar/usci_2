package kz.bsbnb.usci.core.service;

import kz.bsbnb.usci.model.adm.User;
import kz.bsbnb.usci.model.respondent.Respondent;
import kz.bsbnb.usci.model.respondent.RespondentJson;

import java.util.List;

/**
 * @author Jandos Iskakov
 */

public interface RespondentService {

    Respondent getRespondent(long id);

    List<Respondent> getRespondentList();

    List<RespondentJson> getApproveRespondentList();

    Respondent getRespondentByUser(User user);

    Respondent getRespondentByUserId(long userId, boolean isNb);

}
