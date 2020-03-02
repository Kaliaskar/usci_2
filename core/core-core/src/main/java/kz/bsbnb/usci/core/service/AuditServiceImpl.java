package kz.bsbnb.usci.core.service;

import kz.bsbnb.usci.core.dao.AuditDao;

import kz.bsbnb.usci.model.creditor.AuditEvent;
import kz.bsbnb.usci.model.json.AuditJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditServiceImpl implements AuditService {
    private final AuditDao auditDao;
    @Autowired
    public AuditServiceImpl(AuditDao auditDao) {
        this.auditDao = auditDao;
    }

    @Override
    public void insertAuditEvent(AuditEvent auditEvent) {
         auditDao.insertAuditEvent(auditEvent);
    }
    @Override
    public List<AuditEvent> getAudit(long userId) {
        return  auditDao.getAudit(userId);
    }
    @Override
    public List<AuditJson> getAuditJson(long userId) {
        return  auditDao.getAuditJson(userId);
    }
}
