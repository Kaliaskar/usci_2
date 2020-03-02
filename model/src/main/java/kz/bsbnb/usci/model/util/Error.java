package kz.bsbnb.usci.model.util;

import kz.bsbnb.usci.model.persistence.Persistable;

/**
 * @author Zhanar Akhmetova
 */

public class Error extends Persistable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String nameRu;
    private String descriptionRu;
    private String nameKz;
    private String descriptionKz;

    public Error() {
        super();
    }

    public String getCode() {return code; }

    public void setCode(String code) {this.code = code;}

    public String getNameRu() {return nameRu;}

    public void setNameRu(String nameRu) {this.nameRu = nameRu;}

    public String getDescriptionRu() {return descriptionRu;}

    public void setDescriptionRu(String descriptionRu) {this.descriptionRu = descriptionRu;}

    public String getNameKz() {return nameKz;}

    public void setNameKz(String nameKz) {this.nameKz = nameKz;}

    public String getDescriptionKz() {return descriptionKz;}

    public void setDescriptionKz(String descriptionKz) {this.descriptionKz = descriptionKz;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Error error = (Error) o;

        if (!getCode().equals(error.getCode())) return false;
        if (!getNameRu().equals(error.getNameRu())) return false;
        return getDescriptionRu().equals(error.getDescriptionRu());

    }

    @Override
    public int hashCode() {
        int result = getCode().hashCode();
        result = 31 * result + getNameRu().hashCode();
        result = 31 * result + getDescriptionRu().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Error{" +
                "code='" + code + '\'' +
                ", nameRu='" + nameRu + '\'' +
                ", descriptionRu='" + descriptionRu + '\'' +
                ", nameKz='" + nameKz + '\'' +
                ", descriptionKz='" + descriptionKz + '\'' +
                '}';
    }

}
