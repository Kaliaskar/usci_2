package kz.bsbnb.usci.model.util;

/**
 * @author Jandos Iskakov
 */

public class TextJson {
    private Long id;
    private String code;
    private String nameRu;
    private String nameKz;

    public TextJson() {
        super();
    }

    public TextJson(Text text) {
        this.id = text.getId();
        this.code = text.getCode();
        this.nameKz = text.getNameKz();
        this.nameRu = text.getNameRu();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNameRu() {
        return nameRu;
    }

    public void setNameRu(String nameRu) {
        this.nameRu = nameRu;
    }

    public String getNameKz() {
        return nameKz;
    }

    public void setNameKz(String nameKz) {
        this.nameKz = nameKz;
    }
}
