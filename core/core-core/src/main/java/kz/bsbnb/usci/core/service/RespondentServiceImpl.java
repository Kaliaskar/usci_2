package kz.bsbnb.usci.core.service;

import kz.bsbnb.usci.core.dao.UserDao;
import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.eav.model.base.BaseSet;
import kz.bsbnb.usci.eav.model.base.BaseValue;
import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.eav.repository.MetaClassRepository;
import kz.bsbnb.usci.eav.service.BaseEntityLoadService;
import kz.bsbnb.usci.model.Constants;
import kz.bsbnb.usci.model.adm.User;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.respondent.Respondent;
import kz.bsbnb.usci.model.respondent.RespondentJson;
import kz.bsbnb.usci.model.respondent.SubjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static kz.bsbnb.usci.model.Constants.NBRK_BIN;

/**
 * @author Jandos Iskakov
 * @author Baurzhan Makhambetov
 */

@Service
public class RespondentServiceImpl implements RespondentService {
    private final BaseEntityLoadService baseEntityLoadService;
    private final MetaClassRepository metaClassRepository;
    private final UserDao userDao;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public RespondentServiceImpl(BaseEntityLoadService baseEntityLoadService,
                                 MetaClassRepository metaClassRepository,
                                 UserDao userDao, JdbcTemplate jdbcTemplate) {
        this.baseEntityLoadService = baseEntityLoadService;
        this.metaClassRepository = metaClassRepository;
        this.userDao = userDao;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * TODO: оптимизировать выгрузку всех сущностей
     * есть похожий функционал для справочников
     */
    @Override
    public List<Respondent> getRespondentList() {
        List<BaseEntity> entities = baseEntityLoadService.loadBaseEntitiesByMetaClass(metaClassRepository.getMetaClass("ref_respondent"));

        List<Respondent> respondents = new ArrayList<>();
        for (BaseEntity entity : entities)
            respondents.add(getRespondentFromEntity(entity));

        return respondents;
    }

    @Override
    public List<RespondentJson> getApproveRespondentList() {
        MetaClass metaClass = metaClassRepository.getMetaClass("ref_respondent");

        String query = "select /*+parallel(16)*/ distinct CREDITOR_ID \n" +
                "        from EAV_XML.EAV_ENTITY_MAINTENANCE \n" +
                "        where APPROVED = 0 and DECLINED = 0";

        List<Long> rows = jdbcTemplate.queryForList(query, Long.class);

        if (rows.size() < 1)
            return Collections.emptyList();

        List<RespondentJson> respondents = new ArrayList<>();

        for (Long respondentId : rows) {
            BaseEntity beRespondent = baseEntityLoadService.loadBaseEntity(new BaseEntity(respondentId, metaClass, Constants.NBK_AS_RESPONDENT_ID));

            Respondent respondent = getRespondentFromEntity(beRespondent);
            RespondentJson respondentJson = new RespondentJson(respondent);
            respondents.add(respondentJson);
        }

        return respondents;
    }

    /**
     * замечание!!! при вызове batch.getRespondentId будет возвращать 0,
     * но при вызове getCreditor.getId будет возвращать 46
     */
    /*@Override
    public Respondent getRespondent(long id) {
        if (id == NBK_AS_RESPONDENT_ID) {
            return getRespondentList().stream()
                    .filter(respondent -> respondent.getShortName().equals("НБРК"))
                    .findFirst()
                    .get();
        }
        else {
            return getRespondentList().stream()
                    .filter(respondent -> respondent.getId().equals(id))
                    .findFirst()
                    .get();
        }
    }*/

    @Override
    public Respondent getRespondent(long id) {
        MetaClass metaClass = metaClassRepository.getMetaClass("ref_respondent");
        BaseEntity beRespondent = baseEntityLoadService.loadBaseEntity(new BaseEntity(id, metaClass, Constants.NBK_AS_RESPONDENT_ID));
        return  getRespondentFromEntity(beRespondent);
    }

    @Override
    public Respondent getRespondentByUser(User user) {
        List<Respondent> respondents = userDao.getUserRespondentList(user.getUserId());

        if (respondents.isEmpty()) {
            throw new UsciException("Пользователь не привязан к респондентам");
        }

        if (user.isNb()) {
            for (Respondent respondent : respondents) {
                if (respondent.getBin() != null && respondent.getBin().equals(NBRK_BIN)) {
                    return respondent;
                }
            }
        }

        if (respondents.size() != 1)
            throw new UsciException("Пользователь привязан к нескольким респондентам");

        return respondents.get(0);
    }

    @Override
    public Respondent getRespondentByUserId(long userId, boolean isNb) {
        return getRespondentByUser(new User(userId, isNb));
    }

    public static Respondent getRespondentFromEntity(BaseEntity beRespondent) {
        Respondent respondent = new Respondent();
        respondent.setId(beRespondent.getId());

        BaseValue bvName = beRespondent.getBaseValue("name");
        respondent.setName(bvName != null? (String) bvName.getValue(): "none");

        BaseValue bvShortName = beRespondent.getBaseValue("short_name");
        respondent.setShortName(bvShortName != null ? (String) bvShortName.getValue(): "none");

        BaseValue bvCode = beRespondent.getBaseValue("code");
        respondent.setCode(bvCode != null ? (String) bvCode.getValue(): "none");

        respondent.setBin("");

        BaseValue bvDocs = beRespondent.getBaseValue("docs");
        if (bvDocs != null && bvDocs.getValue() != null) {
            BaseSet docs = (BaseSet) bvDocs.getValue();

            for (BaseValue bvDoc : docs.getValues()) {
                BaseEntity beDoc = (BaseEntity) bvDoc.getValue();

                if (beDoc != null) {
                    String docType = (String) beDoc.getEl("ref_doc_type.code");

                    if (docType == null)
                        docType = Constants.DOC_TYPE_BIN;

                    String docNo = (String) beDoc.getEl("no");

                    switch (docType) {
                        case Constants.DOC_TYPE_BIN:
                            respondent.setBin(docNo);
                            break;
                        case Constants.DOC_TYPE_RNN:
                            respondent.setRnn(docNo);
                            break;
                        case Constants.DOC_TYPE_BIK:
                            respondent.setBik(docNo);
                            break;
                    }
                }
            }
        }

        SubjectType subjectType = new SubjectType();
        BaseValue bvSubjectType = beRespondent.getBaseValue("ref_respondent_type");
        BaseEntity beSubjectType = bvSubjectType == null ? null : (BaseEntity) bvSubjectType.getValue();

        if (beSubjectType != null) {
            subjectType.setId(beSubjectType.getId());

            beSubjectType.getAttributes().forEach(atr -> {
                Object obj = beSubjectType.getBaseValue(atr.getName()).getValue();

                if (obj == null)
                    return;
                switch (atr.getName()) {
                    case "code":
                        subjectType.setCode((String) obj);
                        break;
                    case "name_ru":
                        subjectType.setNameRu((String) obj);
                        break;
                    case "name_kz":
                        subjectType.setNameKz((String) obj);
                        break;
                    case "kind_id":
                        break;
                }
            });
        } else {
            subjectType.setCode("NONE");
            subjectType.setNameKz("NONE");
            subjectType.setNameRu("NONE");
        }

        respondent.setSubjectType(subjectType);

        return respondent;
    }

}
