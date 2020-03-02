package kz.bsbnb.usci.core.controller;

import kz.bsbnb.usci.core.service.RespondentService;
import kz.bsbnb.usci.core.service.UserService;
import kz.bsbnb.usci.model.adm.User;
import kz.bsbnb.usci.model.respondent.Respondent;
import kz.bsbnb.usci.model.respondent.RespondentJson;
import kz.bsbnb.usci.util.json.ext.ExtJsJson;
import kz.bsbnb.usci.util.json.ext.ExtJsList;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Jandos Iskakov
 */

@RestController
@RequestMapping(value = "/respondent")
public class RespondentRestController {
    private final RespondentService respondentService;
    private final UserService userService;

    public RespondentRestController(RespondentService respondentService, UserService userService) {
        this.respondentService = respondentService;
        this.userService = userService;
    }

    @GetMapping(value = "getRespondentJsonList")
    public ExtJsList getRespondentJsonList() {
        return new ExtJsList(respondentService.getRespondentList());
    }

    @GetMapping(value = "getApproveRespondentJsonList")
    public List<RespondentJson> getApproveRespondentJsonList() {
        return respondentService.getApproveRespondentList();
    }

    @GetMapping(value = "getRespondentList")
    public List<Respondent> getRespondentList() {
        return respondentService.getRespondentList();
    }

    @GetMapping(value = "getRespondentById")
    public Respondent getRespondentById(@RequestParam(name = "respondentId") Long respondentId) {
        return respondentService.getRespondent(respondentId);
    }

    @GetMapping(value = "getUserRespondentList")
    public ExtJsList getUserRespondentList(@RequestParam(name = "userId") Long userId) {
        return new ExtJsList(userService.getUserRespondentList(userId));
    }

    @PutMapping(value = "addUserRespondent")
    public ExtJsJson addUserRespondent(@RequestParam(name = "userId") Long userId,
                                       @RequestParam(name = "respondentIds") List<Long> respondentIds
                                       ) {
        userService.addUserRespondent(userId, respondentIds);
        return new ExtJsJson(true);
    }

    @PostMapping(value = "delUserRespondent")
    public ExtJsJson deleteUserRespondent(@RequestParam(name = "userId") Long userId,
                                          @RequestParam(name = "respondentIds") List<Long> respondentIds) {
        userService.deleteUserRespondent(userId, respondentIds);
        return new ExtJsJson(true);
    }

    @PostMapping(value = "getRespondentByUser")
    public Respondent getRespondentByUser(@RequestBody User user) {
        return respondentService.getRespondentByUser(user);
    }

    @GetMapping(value = "getRespondentByUserId")
    public Respondent getRespondentByUserId(@RequestParam(name = "userId") Long userId,
                                            @RequestParam(name = "isNb") Boolean isNb) {
        return respondentService.getRespondentByUserId(userId, isNb);
    }

}
