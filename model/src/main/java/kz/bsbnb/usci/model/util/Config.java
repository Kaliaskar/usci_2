package kz.bsbnb.usci.model.util;

import kz.bsbnb.usci.model.persistence.Persistable;

public class Config extends Persistable {
    private static final long serialVersionUID = 1L;

    private String module;
    private String code;
    private String value;
    private String description;

    public Config() {
        super();
    }

    public Config(Long id, String module, String code, String value, String description) {
        this.id = id;
        this.module = module;
        this.code = code;
        this.value = value;
        this.description = description;
    }

    public Config(String module, String code, String value){
        this.module = module;
        this.code = code;
        this.value = value;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Config config = (Config) o;

        if (!getModule().equals(config.getModule())) return false;
        if (!getCode().equals(config.getCode())) return false;
        return getValue().equals(config.getValue());

    }

    @Override
    public int hashCode() {
        int result = getModule().hashCode();
        result = 31 * result + getCode().hashCode();
        result = 31 * result + getValue().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Constant{" +
                "module='" + module + '\'' +
                ", code='" + code + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

}
