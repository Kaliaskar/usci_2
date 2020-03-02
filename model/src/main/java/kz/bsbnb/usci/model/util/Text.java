package kz.bsbnb.usci.model.util;

import kz.bsbnb.usci.model.persistence.Persistable;

/**
 * @author Zhanar Akhmetova
 */

public class Text extends Persistable {
    private static final long serialVersionUID = 1L;

    private String type;
    private String code;
    private String nameRu;
    private String nameKz;

    public Text() {
        super();
    }

    public Text(Long id, String type, String code, String nameRu, String nameKz) {
        super(id);
        this.type = type;
        this.code = code;
        this.nameRu = nameRu;
        this.nameKz = nameKz;
    }

    @Override
    public Text setId(Long id) {
        super.setId(id);
        return this;
    }

    public String getType() {
        return type;
    }

    public Text setType(String type) {
        this.type = type;
        return this;
    }

    public String getCode() {
        return code;
    }

    public Text setCode(String code) {
        this.code = code;
        return this;
    }

    public String getNameRu() {
        return nameRu;
    }

    public Text setNameRu(String nameRu) {
        this.nameRu = nameRu;
        return this;
    }

    public String getNameKz() {
        return nameKz;
    }

    public Text setNameKz(String nameKz) {
        this.nameKz = nameKz;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Text text = (Text) o;

        if (!getType().equals(text.getType())) return false;
        if (!getCode().equals(text.getCode())) return false;

        return true;

    }

    @Override
    public int hashCode() {
        int result = getType().hashCode();
        result = 31 * result + getCode().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Constant{" +
                "type='" + type + '\'' +
                ", code='" + code + '\'' +
                ", nameRu='" + nameRu + '\'' +
                ", nameKz='" + nameKz + '\'' +
                '}';
    }

}
