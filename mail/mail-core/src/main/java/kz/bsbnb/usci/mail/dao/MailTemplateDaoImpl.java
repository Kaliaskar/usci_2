package kz.bsbnb.usci.mail.dao;

import kz.bsbnb.usci.mail.model.MailTemplate;
import kz.bsbnb.usci.mail.model.UserMailTemplate;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Repository
public class MailTemplateDaoImpl implements MailTemplateDao {
    private final MailTemplateMapper mailTemplateMapper = new MailTemplateMapper();
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate npJdbcTemplate;

    public MailTemplateDaoImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate npJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.npJdbcTemplate = npJdbcTemplate;
    }

    public MailTemplate getTemplate(String code) {
        return jdbcTemplate.queryForObject("select * from USCI_MAIL.MAIL_TEMPLATE where CODE = ?", new Object[]{code}, mailTemplateMapper);
    }

    @Override
    public boolean isTemplateEnabledForUser(Long templateId, long userId) {
        int count = jdbcTemplate.queryForObject("select count(ID)\n" +
                        "  from USCI_MAIL.MAIL_USER_MAIL_TEMPLATE\n" +
                        " where PORTAL_USER_ID = ?\n" +
                        "   and MAIL_TEMPLATE_ID = ?" +
                        "   and ENABLED = 1",
                new Object[]{userId, templateId}, Integer.class);

        return count > 0;
    }

    @Override
    public List<UserMailTemplate> getUserMailTemplateList(Long userId) {
        String query = "select mt.ID,\n" +
                "       mt.ENABLED,\n" +
                "       m.ID as MAIL_TEMPLATE_ID,\n" +
                "       m.CODE,\n" +
                "       m.NAME_RU,\n" +
                "       m.NAME_KZ,\n" +
                "       m.CONFIG_TYPE_ID,\n" +
                "       m.SUBJECT,\n" +
                "       m.TEXT\n" +
                "from USCI_MAIL.MAIL_USER_MAIL_TEMPLATE mt, USCI_MAIL.MAIL_TEMPLATE m\n" +
                "where mt.MAIL_TEMPLATE_ID = m.ID\n" +
                "  and mt.PORTAL_USER_ID = :userId";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);

        List<Map<String, Object>> rows = npJdbcTemplate.queryForList(query, params);

        List<UserMailTemplate> userMailTemplateList = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            UserMailTemplate userMailTemplate = new UserMailTemplate();
            userMailTemplate.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
            userMailTemplate.setEnabled(SqlJdbcConverter.convertToBoolean(row.get("ENABLED")));
            userMailTemplate.setUserId(userId);
            userMailTemplate.setMailTemplate(mapRowToMailTemplate(row));
            userMailTemplateList.add(userMailTemplate);
        }

        return userMailTemplateList;
    }

    @Override
    public void saveUserMailTemplateList(List<UserMailTemplate> userMailTemplateList) {
        List<MapSqlParameterSource> params = new ArrayList<>();
        for (UserMailTemplate userMailTemplate : userMailTemplateList) {
            params.add(new MapSqlParameterSource("enabled", SqlJdbcConverter.convertToByte(userMailTemplate.isEnabled()))
                    .addValue("id", userMailTemplate.getId()));
        }

        String query = "update USCI_MAIL.MAIL_USER_MAIL_TEMPLATE\n" +
                        "set ENABLED = :enabled\n" +
                        "where ID = :id";

        if (userMailTemplateList.size() > 1) {
            int counts[] = npJdbcTemplate.batchUpdate(query, params.toArray(new SqlParameterSource[0]));
            if (Arrays.stream(counts).anyMatch(value -> value != 1))
                throw new UsciException("Ошибка update записи в таблице USCI_MAIL.MAIL_USER_MAIL_TEMPLATE");
        } else {
            int count = npJdbcTemplate.update(query, params.get(0));
            if (count != 1)
                throw new UsciException("Ошибка update записи в таблице USCI_MAIL.MAIL_USER_MAIL_TEMPLATE");
        }
    }

    private MailTemplate mapRowToMailTemplate(Map<String, Object> row) {
        MailTemplate mailTemplate = new MailTemplate();
        mailTemplate.setId(SqlJdbcConverter.convertToLong(row.get("ID")));
        mailTemplate.setCode(String.valueOf(row.get("CODE")));
        mailTemplate.setText(String.valueOf(row.get("TEXT")));
        mailTemplate.setSubject(String.valueOf(row.get("SUBJECT")));
        mailTemplate.setNameRu(String.valueOf(row.get("NAME_RU")));
        mailTemplate.setNameKz(String.valueOf(row.get("NAME_KZ")));
        mailTemplate.setTypeId(SqlJdbcConverter.convertToLong(row.get("CONFIG_TYPE_ID")));
        return mailTemplate;
    }

    private static class MailTemplateMapper implements RowMapper<MailTemplate> {

        @Override
        public MailTemplate mapRow(ResultSet rs, int rowNum) throws SQLException {
            MailTemplate mailTemplate = new MailTemplate();
            mailTemplate.setId(rs.getLong("ID"));
            mailTemplate.setCode(rs.getString("CODE"));
            mailTemplate.setText(rs.getString("TEXT"));
            mailTemplate.setSubject(rs.getString("SUBJECT"));
            mailTemplate.setNameRu(rs.getString("NAME_RU"));
            mailTemplate.setNameKz(rs.getString("NAME_KZ"));
            mailTemplate.setTypeId(rs.getLong("CONFIG_TYPE_ID"));

            return mailTemplate;
        }
    }

}
