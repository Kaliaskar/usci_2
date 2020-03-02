package kz.bsbnb.usci.core.service;

import kz.bsbnb.usci.core.dao.UserDao;
import kz.bsbnb.usci.model.adm.User;
import kz.bsbnb.usci.model.batch.Product;
import kz.bsbnb.usci.model.respondent.Respondent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Jandos Iskakov
 */

@Service
public class UserServiceImpl implements UserService {
    private final UserDao userDao;

    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public List<User> getUserList() {
        return userDao.getUserList();
    }

    @Override
    public List<Respondent> getUserRespondentList(long userId) {
        return userDao.getUserRespondentList(userId);
    }

    @Override
    public List<User> getRespondentUserList(long respondentId) {
        return userDao.getRespondentUserList(respondentId);
    }

    @Override
    public User getUser(long userId) {
        return userDao.getUser(userId);
    }

    @Override
    public void addUserProduct(Long userId, List<Long> productIds) {
        userDao.addUserProduct(userId, productIds);
    }

    @Override
    public void deleteUserProduct(Long userId, List<Long> productIds) {
        userDao.deleteUserProduct(userId, productIds);
    }

    @Override
    public List<Product> getUserProductList(long userId) {
        return userDao.getUserProductList(userId);
    }

    @Override
    public List<User> getNationalBankUsers(long respondentId) {
        return userDao.getNationalBankUsers(respondentId);
    }

    @Override
    public void synchronize(List<User> users) {
        userDao.synchronize(users);
    }

    @Override
    public void addMailTemplatesToNewUser(List<User> users) {
        userDao.addMailTemplatesToNewUser(users);
    }

    @Override
    public void addUserRespondent(Long userId, List<Long> respondentIds) {
        userDao.addUserRespondent(userId, respondentIds);
    }

    @Override
    public void deleteUserRespondent(Long userId, List<Long> respondentIds) {
        userDao.deleteUserRespondent(userId, respondentIds);
    }

    @Override
    public Optional<Set<Long>> getUserProductPositionIds(Long userId, Long productId) {
        return userDao.getUserProductPositionIds(userId, productId);
    }

}
