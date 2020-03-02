package kz.bsbnb.usci.model.respondent;

/**
 * @author Jandos Iskakov
 */

public class RespondentJson {
    private Long id;
    private String name;
    private String shortName;
    private String code;

    public RespondentJson() {
        super();
    }

    public RespondentJson(Long id) {
        this.id = id;
    }

    public RespondentJson(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public RespondentJson(Respondent respondent) {
        this.id = respondent.getId();
        this.name = respondent.getName();
    }

    public Long getId() {
        return id;
    }

    public RespondentJson setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public RespondentJson setName(String name) {
        this.name = name;
        return this;
    }

    public String getShortName() {
        return shortName;
    }

    public RespondentJson setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    public String getCode() {
        return code;
    }

    public RespondentJson setCode(String code) {
        this.code = code;
        return this;
    }

}
