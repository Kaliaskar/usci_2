package kz.bsbnb.usci.mail.dao;

import com.google.gson.Gson;
import kz.bsbnb.usci.mail.model.MailMessage;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

/**
 * @author Baurzhan Makhanbetov
 * @author Jandos Iskakov
 */

@Repository
public class MailMessageDaoImpl implements MailMessageDao {
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private SimpleJdbcInsert mailMessageInsert;
    private static final Gson gson = new Gson();

    public MailMessageDaoImpl(NamedParameterJdbcTemplate npJdbcTemplate) {
        this.npJdbcTemplate = npJdbcTemplate;

        this.mailMessageInsert = new SimpleJdbcInsert(npJdbcTemplate.getJdbcTemplate())
                .withSchemaName("USCI_MAIL")
                .withTableName("MAIL_MESSAGE")
                .usingGeneratedKeyColumns("ID");
    }

    @Override
    public void updateMailMessage(MailMessage mailMessage) {
        int count = npJdbcTemplate.update("update USCI_MAIL.MAIL_MESSAGE\n" +
                        "   set STATUS_ID = :STATUS_ID, SENDING_DATE = :SENDING_DATE\n" +
                        " where ID = :ID",
                new MapSqlParameterSource("ID", mailMessage.getId())
                        .addValue("STATUS_ID", mailMessage.getStatus().getId())
                        .addValue("SENDING_DATE", SqlJdbcConverter.convertToSqlTimestamp(mailMessage.getSendingDate())));

        if (count != 1)
            throw new UsciException("Ошибка update записи в таблице USCI_MAIL.MAIL_MESSAGE");
    }

    @Override
    public MailMessage insertMailMessage(MailMessage mailMessage) {
        Number id = mailMessageInsert.executeAndReturnKey(new MapSqlParameterSource()
                .addValue("RECEIVER_USER_ID", mailMessage.getReceiver().getUserId())
                .addValue("STATUS_ID", mailMessage.getStatus().getId())
                .addValue("MAIL_TEMPLATE_ID", mailMessage.getMailTemplate().getId())
                .addValue("CREATION_DATE", SqlJdbcConverter.convertToSqlTimestamp(mailMessage.getCreationDate()))
                .addValue("SENDING_DATE", SqlJdbcConverter.convertToSqlTimestamp(mailMessage.getSendingDate()))
                .addValue("PARAMS", gson.toJson(mailMessage.getParams())));

        mailMessage.setId(id.longValue());

        return mailMessage;
    }

}
