package kz.bsbnb.usci.core.client;

import kz.bsbnb.usci.model.adm.User;
import kz.bsbnb.usci.model.respondent.Respondent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "core")
public interface RespondentClient {

    @PostMapping(value = "/respondent/getRespondentByUser")
    Respondent getRespondentByUser(@RequestBody User user);

    @GetMapping(value = "/respondent/getRespondentByUserId")
    Respondent getRespondentByUserId(@RequestParam(name = "userId") Long userId,
                                     @RequestParam(name = "isNb") boolean isNb);

    @GetMapping(value = "/respondent/getRespondentList")
    List<Respondent> getRespondentList();

    @GetMapping(value = "/respondent/getRespondentById")
    Respondent getRespondentById(@RequestParam(name = "respondentId") Long respondentId);

}
