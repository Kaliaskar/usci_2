package kz.bsbnb.usci.util.service;

import kz.bsbnb.usci.model.util.Config;

import java.util.List;
import java.util.Set;

public interface ConfigService {

    Config getConfig(String module, String code);

    Config getConfig(Long id);

    void updateConfig(Config config);

    Set<Long> getDigitalSigningOrgIds();

    Set<Long> getPriorityRespondentIds();

    void setPriorityRespondentIds(List<Long> priorityRespondentIds);

    void setDigitalSigningOrgIds(List<Long> digitalSigningOrgIds);

    void setQueueAlgorithm(String queueAlgorithm);

    byte[] getManifestXsd();

    String getConfirmText();

}
