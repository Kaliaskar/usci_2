package kz.bsbnb.usci.model.adm;

import kz.bsbnb.usci.model.persistence.Persistable;

/**
 * @author Jandos Iskakov
 * Справочник должностей
 */

public class Position extends Persistable {
    private String nameRu;
    private String nameKz;
    private String shortNameRu;
    private String shortNameKz;
    private Long level;

    @Override
    public Position setId(Long id) {
        this.id = id;
        return this;
    }

    public String getNameRu() {
        return nameRu;
    }

    public Position setNameRu(String nameRu) {
        this.nameRu = nameRu;
        return this;
    }

    public String getNameKz() {
        return nameKz;
    }

    public Position setNameKz(String nameKz) {
        this.nameKz = nameKz;
        return this;
    }

    public Long getLevel() {
        return level;
    }

    public Position setLevel(Long level) {
        this.level = level;
        return this;
    }

    public String getShortNameRu() {
        return shortNameRu;
    }

    public Position setShortNameRu(String shortNameRu) {
        this.shortNameRu = shortNameRu;
        return this;
    }

    public String getShortNameKz() {
        return shortNameKz;
    }

    public Position setShortNameKz(String shortNameKz) {
        this.shortNameKz = shortNameKz;
        return this;
    }

}
