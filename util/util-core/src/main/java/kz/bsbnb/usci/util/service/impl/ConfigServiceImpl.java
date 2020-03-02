package kz.bsbnb.usci.util.service.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import kz.bsbnb.usci.model.json.LocalDateGsonAdapter;
import kz.bsbnb.usci.model.util.Config;
import kz.bsbnb.usci.util.dao.ConfigDao;
import kz.bsbnb.usci.util.service.ConfigService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static kz.bsbnb.usci.util.Constants.DIGITAL_SIGNING_ORG_IDS_CONFIG_CODE;

/**
 * @author Jandos Iskakov
 */

@Service
public class ConfigServiceImpl implements ConfigService {
    private Gson gson;
    private final ConfigDao configDao;

    @PostConstruct
    public void init() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, new LocalDateGsonAdapter())
                .create();
    }

    public ConfigServiceImpl(ConfigDao configDao) {
        this.configDao = configDao;
    }

    @Override
    public Config getConfig(String module, String code) {
        return configDao.get(module, code);
    }

    @Override
    public Config getConfig(Long id) {
        return configDao.get(id);
    }

    @Override
    public void updateConfig(Config constant) {
        configDao.update(constant);
    }

    @Override
    public Set<Long> getDigitalSigningOrgIds() {
        String value = getConfig("USCI", DIGITAL_SIGNING_ORG_IDS_CONFIG_CODE).getValue();

        Type type = new TypeToken<HashSet<Long>>(){}.getType();
        return gson.fromJson(value, type);
    }

    @Override
    public Set<Long> getPriorityRespondentIds() {
        String value = getConfig("RECEIVER", "PRIORITY_CREDITOR_IDS").getValue();

        Type type = new TypeToken<HashSet<Long>>(){}.getType();
        return gson.fromJson(value, type);
    }

    @Override
    public void setPriorityRespondentIds(List<Long> priorityRespondentIds) {
        Config config = getConfig("RECEIVER", "PRIORITY_CREDITOR_IDS");
        config.setValue(gson.toJson(priorityRespondentIds));
        updateConfig(config);
    }

    @Override
    public void setDigitalSigningOrgIds(List<Long> digitalSigningOrgIds) {
        Config config = getConfig("USCI", DIGITAL_SIGNING_ORG_IDS_CONFIG_CODE);
        config.setValue(gson.toJson(digitalSigningOrgIds));
        updateConfig(config);
    }

    @Override
    public void setQueueAlgorithm(String queueAlgorithm) {
        Config config = getConfig("RECEIVER", "QUEUE_ALGO");
        config.setValue(queueAlgorithm);
        updateConfig(config);
    }

    @Override
    public byte[] getManifestXsd() {
        Config config = getConfig("RECEIVER", "MANIFEST_XSD");
        return config.getValue().getBytes();
    }

    @Override
    public String getConfirmText() {
        Config config = getConfig("USCI", "CONFIRM_TEXT");
        return config.getValue();
    }

}
