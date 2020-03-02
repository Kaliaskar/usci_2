package kz.bsbnb.usci.core.dao;

import kz.bsbnb.usci.core.service.RespondentServiceImpl;
import kz.bsbnb.usci.eav.meta.dao.ProductDaoImpl;
import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.eav.repository.MetaClassRepository;
import kz.bsbnb.usci.eav.service.BaseEntityLoadService;
import kz.bsbnb.usci.model.Constants;
import kz.bsbnb.usci.model.adm.User;
import kz.bsbnb.usci.model.batch.Product;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.respondent.Respondent;
import kz.bsbnb.usci.util.SetUtils;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author Artur Tkachenko
 * @author Yernur Bakash
 * @author Jandos Iskakov
 */

@Repository
public class UserDaoImpl implements UserDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDaoImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final MetaClassRepository metaClassRepository;
    private final BaseEntityLoadService baseEntityLoadService;
    private final SimpleJdbcInsert respondentUserInsert;
    private final SimpleJdbcInsert userProductInsert;

    @Autowired
    public UserDaoImpl(JdbcTemplate jdbcTemplate,
                       NamedParameterJdbcTemplate npJdbcTemplate,
                       MetaClassRepository metaClassRepository,
                       BaseEntityLoadService baseEntityLoadService) {
        this.jdbcTemplate = jdbcTemplate;
        this.npJdbcTemplate = npJdbcTemplate;
        this.metaClassRepository = metaClassRepository;
        this.baseEntityLoadService = baseEntityLoadService;

        this.respondentUserInsert = new SimpleJdbcInsert(jdbcTemplate)
            .withSchemaName("USCI_ADM")
            .withTableName("CREDITOR_USER")
            .usingColumns("USER_ID", "CREDITOR_ID")
            .usingGeneratedKeyColumns("ID");

        this.userProductInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName("USCI_ADM")
                .withTableName("USER_PRODUCT")
                .usingColumns("USER_ID", "PRODUCT_ID")
                .usingGeneratedKeyColumns("ID");
    }

    @Override
    public List<User> getUserList() {
        List<Map<String, Object>> rows =  jdbcTemplate.queryForList("select * from USCI_ADM.USERS");

        List<User> users = new ArrayList<>();
        for (Map<String, Object> row : rows)
            users.add(getUserFromJdbcMap(row, false));

        return users;
    }

    @Override
    public List<User> getRespondentUserList(long respondentId) {
        List<Map<String, Object>> rows =  jdbcTemplate.queryForList("select u.*\n" +
                "from USCI_ADM.CREDITOR_USER cu,\n" +
                "     USCI_ADM.USERS u\n" +
                "where cu.CREDITOR_ID = ?\n" +
                "  and cu.USER_ID = u.USER_ID", respondentId);

        List<User> users = new ArrayList<>();
        for (Map<String, Object> row : rows)
            users.add(getUserFromJdbcMap(row, false));

        return users;
    }

    @Override
    public List<Respondent> getUserRespondentList(long userId) {
        MetaClass metaClass = metaClassRepository.getMetaClass("ref_respondent");

        String query = "select CREDITOR_ID from USCI_ADM.CREDITOR_USER where USER_ID = :userId";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);

        List<Respondent> respondents = new ArrayList<>();

        List<Long> rows = npJdbcTemplate.queryForList(query, params, Long.class);

        if (rows.size() < 1)
            return Collections.emptyList();

        for (Long respondentId : rows) {
            BaseEntity beRespondent = baseEntityLoadService.loadBaseEntity(new BaseEntity(respondentId, metaClass, Constants.NBK_AS_RESPONDENT_ID));

            Respondent respondent = RespondentServiceImpl.getRespondentFromEntity(beRespondent);
            respondents.add(respondent);
        }

        return respondents;
    }

    @Override
    public void addUserRespondent(Long userId, List<Long> respondentIds) {
        List<MapSqlParameterSource> params = new ArrayList<>();

        for (Long respondentId : respondentIds) {
            params.add(new MapSqlParameterSource("USER_ID", userId)
                    .addValue("CREDITOR_ID", respondentId));
        }

        if (respondentIds.size() > 1) {
            int counts[] = respondentUserInsert.executeBatch(params.toArray(new SqlParameterSource[0]));
            if (Arrays.stream(counts).anyMatch(value -> value != 1))
                throw new UsciException("Ошибка insert записей в таблицу USCI_ADM.CREDITOR_USER");
        }
        else {
            int count = respondentUserInsert.execute(params.get(0));
            if (count != 1)
                throw new UsciException("Ошибка insert записей в таблицу USCI_ADM.CREDITOR_USER");
        }

    }

    @Override
    public void deleteUserRespondent(Long userId, List<Long> respondentIds) {
        List<MapSqlParameterSource> params = new ArrayList<>();

        for (Long respondentId : respondentIds) {
            params.add(new MapSqlParameterSource("USER_ID", userId)
                    .addValue("RESPONDENT_ID", respondentId));
        }

        String delete = "delete from USCI_ADM.CREDITOR_USER\n" +
                        "  where USER_ID = :USER_ID\n" +
                        "    and CREDITOR_ID = :RESPONDENT_ID";

        if (respondentIds.size() > 1) {
            int counts[] = npJdbcTemplate.batchUpdate(delete, params.toArray(new SqlParameterSource[0]));
            if (Arrays.stream(counts).anyMatch(value -> value != 1))
                throw new UsciException("Ошибка delete записей из таблицы USCI_ADM.CREDITOR_USER");
        } else {
            int count = npJdbcTemplate.update(delete, params.get(0));
            if (count != 1)
                throw new UsciException("Ошибка delete записей из таблицы USCI_ADM.CREDITOR_USER");
        }
    }

    @Override
    public void addUserProduct(Long userId, List<Long> productIds) {
        List<MapSqlParameterSource> params = new ArrayList<>();

        for (Long productId : productIds) {
            params.add(new MapSqlParameterSource("USER_ID", userId)
                    .addValue("PRODUCT_ID", productId));
        }

        if (productIds.size() > 1) {
            int counts[] = userProductInsert.executeBatch(params.toArray(new SqlParameterSource[0]));
            if (Arrays.stream(counts).anyMatch(value -> value != 1))
                throw new UsciException("Ошибка insert записей в таблицу USCI_ADM.USER_PRODUCT");
        }
        else {
            int count = userProductInsert.execute(params.get(0));
            if (count != 1)
                throw new UsciException("Ошибка insert записей в таблицу USCI_ADM.USER_PRODUCT");
        }
    }

    @Override
    public void deleteUserProduct(Long userId, List<Long> productIds) {
        List<MapSqlParameterSource> params = new ArrayList<>();

        for (Long productId : productIds) {
            params.add(new MapSqlParameterSource("USER_ID", userId)
                    .addValue("PRODUCT_ID", productId));
        }

        String delete = "delete from USCI_ADM.USER_PRODUCT\n" +
                "  where USER_ID = :USER_ID\n" +
                "    and PRODUCT_ID = :PRODUCT_ID";

        if (productIds.size() > 1) {
            int counts[] = npJdbcTemplate.batchUpdate(delete, params.toArray(new SqlParameterSource[0]));
            if (Arrays.stream(counts).anyMatch(value -> value != 1))
                throw new UsciException("Ошибка delete записей из таблицы USCI_ADM.USER_PRODUCT");
        } else {
            int count = npJdbcTemplate.update(delete, params.get(0));
            if (count != 1)
                throw new UsciException("Ошибка delete записей из таблицы USCI_ADM.USER_PRODUCT");
        }
    }

    @Override
    public List<Product> getUserProductList(long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select p.*\n" +
                "  from USCI_ADM.USER_PRODUCT up, \n" +
                "       EAV_CORE.EAV_M_PRODUCT p\n" +
                " where up.USER_ID = ?\n" +
                "   and up.PRODUCT_ID = p.ID", userId);

        return ProductDaoImpl.getProductFromJdbcMap(rows);
    }

    //TODO: отрефакторить
    @Override
    public void synchronize(List<User> users) {
        HashMap<Long, User> usersFromDB = new HashMap<>();
        HashMap<Long, User> usersFromPortal = new HashMap<>();

        String querySelect = "select ID, USER_ID, FIRST_NAME, LAST_NAME, MIDDLE_NAME, SCREEN_NAME, EMAIL, MODIFIED_DATE, IS_ACTIVE from USCI_ADM.USERS";

        List<Map<String, Object>> rows =  jdbcTemplate.queryForList(querySelect);

        for (Map<String, Object> row : rows) {
            User user = getUserFromJdbcMap(row, false);
            usersFromDB.put(user.getUserId(), user);
        }

        for (User user : users) {
            usersFromPortal.put(user.getUserId(), user);
        }
        //TODO:временно убрал
        //Set<Long> toDelete = SetUtils.difference(usersFromDB.keySet(), usersFromPortal.keySet());
        Set<Long> toAdd = SetUtils.difference(usersFromPortal.keySet(), usersFromDB.keySet());
        Set<Long> toUpdate = SetUtils.intersection(usersFromPortal.keySet(), usersFromDB.keySet());

        //TODO:временно убрал

        /*for (Long id : toDelete) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("userId", id);

            String queryDelete = "delete from USCI_ADM.CREDITOR_USER\n" +
                                 "  where USER_ID = :userId";

            int count = npJdbcTemplate.update(queryDelete, params);

            if (count > 1 )
                throw new UsciException("Ошибка delete записей из таблицы USCI_ADM.CREDITOR_USER");

            queryDelete = "delete from USCI_ADM.USERS\n" +
                          "  where USER_ID = :userId";

            count = npJdbcTemplate.update(queryDelete, params);
            if (count != 1)
                throw new UsciException("Ошибка delete записей из таблицы USCI_ADM.USERS");
        }*/

        for (Long id : toAdd) {
            User user = usersFromPortal.get(id);

            SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                    .withSchemaName("USCI_ADM")
                    .withTableName("USERS")
                    .usingGeneratedKeyColumns("ID")
                    .usingColumns("USER_ID","FIRST_NAME","LAST_NAME","MIDDLE_NAME","SCREEN_NAME","EMAIL","MODIFIED_DATE");;

            int count = simpleJdbcInsert.execute(new MapSqlParameterSource()
                    .addValue("USER_ID", user.getUserId())
                    .addValue("FIRST_NAME", user.getFirstName())
                    .addValue("LAST_NAME", user.getLastName())
                    .addValue("MIDDLE_NAME", user.getMiddleName())
                    .addValue("SCREEN_NAME", user.getScreenName())
                    .addValue("EMAIL", user.getEmailAddress())
                    .addValue("MODIFIED_DATE", SqlJdbcConverter.convertToSqlDate(user.getModifiedDate())));
            if (count != 1)
                throw new UsciException("Ошибка insert записей в таблицу USCI_ADM.USERS");
        }

        for (Long id : toUpdate) {
            User user = usersFromPortal.get(id);
            User dbUser = usersFromDB.get(id);
            if (!user.getFirstName().equals(dbUser.getFirstName()) ||
                    !user.getLastName().equals(dbUser.getLastName()) ||
                    !user.getMiddleName().equals(dbUser.getMiddleName()) ||
                    !user.getScreenName().equals(dbUser.getScreenName()) ||
                    !user.getEmailAddress().equals(dbUser.getEmailAddress()) ||
                    !user.isActive().equals(dbUser.isActive()) ) {
                int count = npJdbcTemplate.update("UPDATE USCI_ADM.USERS\n" +
                                "   SET USER_ID = :userId ,\n" +
                                "   FIRST_NAME = :firstName ,\n" +
                                "   LAST_NAME = :lastName ,\n" +
                                "   MIDDLE_NAME = :middleName ,\n" +
                                "   SCREEN_NAME = :screenName ,\n" +
                                "   EMAIL = :emailAddress ,\n" +
                                "   MODIFIED_DATE = :modifiedDate ,\n" +
                                "   IS_ACTIVE = :isActive\n" +
                                "   WHERE USER_ID = :id ",
                        new MapSqlParameterSource("id", id)
                                .addValue("userId", user.getUserId())
                                .addValue("firstName", user.getFirstName())
                                .addValue("lastName", user.getLastName())
                                .addValue("middleName", user.getMiddleName())
                                .addValue("screenName", user.getScreenName())
                                .addValue("emailAddress", user.getEmailAddress())
                                .addValue("modifiedDate", SqlJdbcConverter.convertToSqlDate(user.getModifiedDate()))
                                .addValue("isActive", SqlJdbcConverter.convertToByte(user.isActive())));
                if (count != 1)
                    throw new RuntimeException("Ошибка update записи в таблице USCI_ADM.USERS");
            }
        }
    }

    //TODO: отрефакторить
    @Override
    public void addMailTemplatesToNewUser(List<User> users) {
        String select = "select USER_ID from USCI_ADM.USERS";

        List<Long> rows = jdbcTemplate.queryForList(select, Long.class);

        Set<Long> dbUsers = new HashSet<>();
        Set<Long> liferayUsers = new HashSet<>();

        for (Long userId : rows) {
            dbUsers.add(userId);
        }

        for (User user : users) {
            liferayUsers.add(user.getUserId());
        }

        Set<Long> toAdd = SetUtils.difference(liferayUsers, dbUsers);

        if(toAdd.size() < 1)
            return;

        Set<Long> userConfigurableTemplates = new HashSet<>();

        String templateSelect = "select ID from USCI_MAIL.MAIL_TEMPLATE where CONFIG_TYPE_ID = 137";

        List<Long> templateRows = jdbcTemplate.queryForList(templateSelect, Long.class);

        for (Long templateId : templateRows) {
            userConfigurableTemplates.add(templateId);
        }

        for (Long userId : toAdd) {
            for (Long templateId : userConfigurableTemplates) {
                SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                        .withSchemaName("USCI_MAIL")
                        .withTableName("MAIL_USER_MAIL_TEMPLATE")
                        .usingGeneratedKeyColumns("ID");

                int count = simpleJdbcInsert.execute(new MapSqlParameterSource()
                        .addValue("PORTAL_USER_ID", userId)
                        .addValue("MAIL_TEMPLATE_ID", templateId)
                        .addValue("ENABLED", 0L));
                if (count != 1)
                    throw new UsciException("Ошибка insert записей в таблицу USCI_MAIL.MAIL_USER_MAIL_TEMPLATE");
            }
        }
    }

    @Override
    public User getUser(long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from USCI_ADM.USERS where USER_ID = ?", userId);
        if (rows == null || rows.isEmpty())
            return null;

        Map<String, Object> row = rows.get(0);

        return getUserFromJdbcMap(row, true);
    }

    @Override
    public List<User> getNationalBankUsers(long respondentId) {
        List<Map<String, Object>> rows =  jdbcTemplate.queryForList("select *\n" +
                "  from USCI_ADM.USERS u\n" +
                " where EMAIL like '%nationalbank.kz'\n" +
                "   and exists (select cu.USER_ID\n" +
                "                 from USCI_ADM.CREDITOR_USER cu\n" +
                "                where cu.USER_ID = u.USER_ID\n" +
                "                  and cu.CREDITOR_ID = ?)", new Object[]{respondentId});

        List<User> users = new ArrayList<>();
        for (Map<String, Object> row : rows)
            users.add(getUserFromJdbcMap(row, false));

        return users;
    }

    public Optional<Set<Long>> getUserProductPositionIds(Long userId, Long productId) {
        try {
            return Optional.of(new HashSet<>(jdbcTemplate.queryForList("select POS_ID\n" +
                    " from USCI_ADM.USER_PROD_POS\n" +
                    "where USER_ID = ?\n" +
                    "  and PRODUCT_ID = ?", new Object[]{userId, productId}, Long.class)));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private User getUserFromJdbcMap(Map<String, Object> row, boolean fillRespondents) {
        User user = new User();

        user.setId(BigInteger.valueOf(SqlJdbcConverter.convertToLong(row.get("ID"))));
        user.setUserId(SqlJdbcConverter.convertToLong(row.get("USER_ID")));
        user.setScreenName(SqlJdbcConverter.convertObjectToString(row.get("SCREEN_NAME")));
        user.setEmailAddress(SqlJdbcConverter.convertObjectToString(row.get("EMAIL")));
        user.setFirstName(SqlJdbcConverter.convertObjectToString(row.get("FIRST_NAME")));
        user.setLastName(SqlJdbcConverter.convertObjectToString(row.get("LAST_NAME")));
        user.setMiddleName(SqlJdbcConverter.convertObjectToString(row.get("MIDDLE_NAME")));
        user.setModifiedDate(SqlJdbcConverter.convertToLocalDate((Timestamp) row.get("MODIFIED_DATE")));
        user.setActive(SqlJdbcConverter.convertToBoolean(row.get("IS_ACTIVE")));

        if (fillRespondents)
            user.setRespondents(getUserRespondentList(user.getUserId()));

        return user;
    }

}
