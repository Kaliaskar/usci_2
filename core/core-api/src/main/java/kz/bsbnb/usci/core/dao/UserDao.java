package kz.bsbnb.usci.core.dao;

import kz.bsbnb.usci.model.adm.User;
import kz.bsbnb.usci.model.batch.Product;
import kz.bsbnb.usci.model.respondent.Respondent;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Artur Tkachenko
 * @author Yernur Bakash
 * @author Jandos Iskakov
 */

public interface UserDao {

    List<User> getUserList();

    List<Respondent> getUserRespondentList(long userId);

    List<User> getRespondentUserList(long respondentId);

    User getUser(long userId);

    List<User> getNationalBankUsers(long respondentId);

    void addUserRespondent(Long userId, List<Long> respondentIds);

    void deleteUserRespondent(Long userId, List<Long> respondentIds);

    void addUserProduct(Long userId, List<Long> productIds);

    void deleteUserProduct(Long userId, List<Long> productIds);

    List<Product> getUserProductList(long userId);

    void synchronize(List<User> users);

    Optional<Set<Long>> getUserProductPositionIds(Long userId, Long productId);

    void addMailTemplatesToNewUser(List<User> users);

}
