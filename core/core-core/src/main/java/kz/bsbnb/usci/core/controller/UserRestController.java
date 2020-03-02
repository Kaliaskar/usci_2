package kz.bsbnb.usci.core.controller;

import kz.bsbnb.usci.core.service.UserService;
import kz.bsbnb.usci.eav.meta.controller.ProductController;
import kz.bsbnb.usci.eav.model.meta.json.ProductJson;
import kz.bsbnb.usci.model.adm.User;
import kz.bsbnb.usci.model.respondent.Respondent;
import kz.bsbnb.usci.util.json.ext.ExtJsJson;
import kz.bsbnb.usci.util.json.ext.ExtJsList;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jandos Iskakov
 */

@RestController
@RequestMapping(value = "/user")
public class UserRestController {
    private final UserService userService;

    public UserRestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(value = "getUserRespondentList")
    public List<Respondent> getUserRespondentList(@RequestParam(name = "userId") long userId) {
        return userService.getUserRespondentList(userId);
    }

    @GetMapping(value = "getUser")
    public User getUser(@RequestParam long userId) {
        return userService.getUser(userId);
    }

    @GetMapping(value = "getUsers")
    public List<User> getUsers() {
        return userService.getUserList();
    }

    @GetMapping(value = "getUserList")
    public ExtJsList getUserList() {
        return new ExtJsList(userService.getUserList());
    }

    @GetMapping(value = "getNationalBankUsers")
    public List<User> getNationalBankUsers(@RequestParam long respondentId) {
        return userService.getNationalBankUsers(respondentId);
    }

    @GetMapping(value = "getUserProductJsonList")
    public List<ProductJson> getUserProductJsonList(@RequestParam(name = "userId") long userId) {
        return userService.getUserProductList(userId).stream()
                .map(ProductController::convertProductToJson)
                .collect(Collectors.toList());
    }

    @PutMapping(value = "addUserProduct")
    public ExtJsJson addUserProduct(@RequestParam(name = "userId") Long userId,
                                    @RequestParam(name = "productIds") List<Long> productIds) {
        userService.addUserProduct(userId, productIds);
        return new ExtJsJson(true);
    }

    @PostMapping(value = "delUserProduct")
    public ExtJsJson delUserProduct(@RequestParam(name = "userId") Long userId,
                                     @RequestParam(name = "productIds") List<Long> productIds) {
        userService.deleteUserProduct(userId, productIds);
        return new ExtJsJson(true);
    }

    @PostMapping(value = "syncUsers")
    public void syncUsers(@RequestBody List<User> users) {
        userService.addMailTemplatesToNewUser(users);
        userService.synchronize(users);
    }

}
