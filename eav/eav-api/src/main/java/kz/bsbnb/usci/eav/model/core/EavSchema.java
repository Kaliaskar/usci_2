package kz.bsbnb.usci.eav.model.core;

/**
 * @author Jandos Iskakov
 */

public enum EavSchema {
    EAV_DATA("EAV_DATA"),
    EAV_XML("EAV_XML");

    private final String code;

    EavSchema(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return name();
    }
}
