package kz.bsbnb.usci.core.client;

import kz.bsbnb.usci.model.adm.User;
import kz.bsbnb.usci.model.respondent.Respondent;
import kz.bsbnb.usci.util.json.ext.ExtJsList;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author Jandos Iskakov
 */

@FeignClient(name = "core")
public interface UserClient {

    @GetMapping(value = "user/getUserRespondentList")
    List<Respondent> getUserRespondentList(@RequestParam(name = "userId") long userId);

    @GetMapping(value = "user/getUser")
    User getUser(@RequestParam(name = "userId") long userId);

    @GetMapping(value = "user/getNationalBankUsers")
    List<User> getNationalBankUsers(@RequestParam(name = "respondentId") long respondentId);

    @GetMapping(value = "user/getUsers")
    List<User> getUsers();

}
