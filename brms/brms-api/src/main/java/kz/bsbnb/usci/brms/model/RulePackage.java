package kz.bsbnb.usci.brms.model;

import kz.bsbnb.usci.model.persistence.Persistable;

import java.io.Serializable;

/**
 * @author Artur Tkachenko
 */

public class RulePackage extends Persistable implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String description;

    public RulePackage() {
        super();
    }

    public RulePackage(String name) {
        this.name = name;
    }

    public RulePackage(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "RulePackage{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

}
