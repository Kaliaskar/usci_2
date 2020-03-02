package kz.bsbnb.usci.model.respondent;

import kz.bsbnb.usci.model.persistence.Persistable;

import java.io.Serializable;

/**
 * @author Artur Tkachenko
 */

public class Respondent extends Persistable implements Serializable {
    private String name;
    private String shortName;
    private String code;
    private String bin;
    private String rnn;
    private String bik;
    private SubjectType subjectType;

    public Respondent() {
        super();
    }

    public Respondent(Long id) {
        super(id);
    }

    public Long getId() {
        return id;
    }

    @Override
    public Respondent setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    public String getRnn() {
        return rnn;
    }

    public void setRnn(String rnn) {
        this.rnn = rnn;
    }

    public String getBik() {
        return bik;
    }

    public void setBik(String bik) {
        this.bik = bik;
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(SubjectType subjectType) {
        this.subjectType = subjectType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Respondent respondent = (Respondent) o;

        return getId().equals(respondent.getId());

    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
