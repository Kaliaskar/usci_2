package kz.bsbnb.usci.core.service;

import kz.bsbnb.usci.model.creditor.AuditEvent;
import kz.bsbnb.usci.model.json.AuditJson;

import java.util.List;


public interface AuditService {

     void insertAuditEvent(AuditEvent auditEvent);

     List<AuditEvent> getAudit(long userId);

     List <AuditJson> getAuditJson(long userId);

}
