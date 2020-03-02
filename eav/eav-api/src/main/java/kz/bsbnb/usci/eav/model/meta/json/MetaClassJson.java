package kz.bsbnb.usci.eav.model.meta.json;

import kz.bsbnb.usci.model.ui.UiConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jandos Iskakov
 */

public class MetaClassJson {
    private Long id;
    private String name;
    private String title;
    private boolean isDictionary;
    private boolean isOperational;
    private boolean isDeleted;
    private boolean isSync;
    private Long periodTypeId;
    private UiConfig uiConfig;
    private Short hashSize;
    private List<MetaAttributeJson> attributes = new ArrayList<>();

    public MetaClassJson() {
        super();
    }

    public MetaClassJson(Long id, String name, String title) {
        this.id = id;
        this.name = name;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isDictionary() {
        return isDictionary;
    }

    public void setDictionary(boolean dictionary) {
        isDictionary = dictionary;
    }

    public boolean isOperational() {
        return isOperational;
    }

    public void setOperational(boolean operational) {
        isOperational = operational;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public List<MetaAttributeJson> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<MetaAttributeJson> attributes) {
        this.attributes = attributes;
    }

    public Long getPeriodTypeId() {
        return periodTypeId;
    }

    public void setPeriodTypeId(Long periodTypeId) {
        this.periodTypeId = periodTypeId;
    }

    public UiConfig getUiConfig() {
        return uiConfig;
    }

    public void setUiConfig(UiConfig uiConfig) {
        this.uiConfig = uiConfig;
    }

    public boolean isSync() {
        return isSync;
    }

    public void setSync(boolean sync) {
        isSync = sync;
    }

    public Short getHashSize() {
        return hashSize;
    }

    public void setHashSize(Short hashSize) {
        this.hashSize = hashSize;
    }

}
