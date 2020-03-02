package kz.bsbnb.usci.brms.model;

import kz.bsbnb.usci.model.persistence.Persistable;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * @author Aibek Bukabaev
 */

public class Rule extends Persistable implements Serializable {
    private static final long serialVersionUID = 1L;

    private String rule;
    private String title;
    private boolean isActive;
    private LocalDate openDate;
    private LocalDate closeDate;

    public Rule() {
        super();
    }

    public Rule(long ruleId, LocalDate date) {
        this.id = ruleId;
        this.openDate = date;
    }

    public Rule(String title, String rule) {
        this.title = title;
        this.rule = rule;
    }

    public Rule(String title, String rule, LocalDate openDate) {
        this.title = title;
        this.rule = rule;
        this.openDate = openDate;
    }

    public Rule(String rule) {
        this.rule = rule;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDate getOpenDate() {
        return openDate;
    }

    public void setOpenDate(LocalDate openDate) {
        this.openDate = openDate;
    }

    public LocalDate getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(LocalDate closeDate) {
        this.closeDate = closeDate;
    }
}
