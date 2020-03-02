package kz.bsbnb.usci.model.adm;

import kz.bsbnb.usci.model.respondent.Respondent;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Artur Tkachenko
 */

public class User implements Serializable {
    private static final long serialVersionUID = 8626348715892412242L;

    private BigInteger id;
    private Long userId;
    private String screenName;
    private String emailAddress;
    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate modifiedDate;
    private Boolean isActive;
    private List<Respondent> respondents;
    private boolean isNb;

    public User() {
        super();
    }

    public User(Long userId) {
        this.userId = userId;
    }

    public User(Long userId, boolean isNb) {
        this.userId = userId;
        this.isNb = isNb;
    }

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String ScreenName) {
        this.screenName = ScreenName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public LocalDate getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(LocalDate modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public List<Respondent> getRespondents() {
        return respondents;
    }

    public void setRespondents(List<Respondent> respondents) {
        this.respondents = respondents;
    }

    public Boolean isActive() {
        return isActive;
    }

    public void setActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isNb() {
        return isNb;
    }

    public String getFullName() {
        return firstName + (lastName != null? " ".concat(lastName) : "") + (middleName != null? " ".concat(middleName): "");
    }

    public void setNb(boolean nb) {
        isNb = nb;
    }

    @Override
    public String toString() {
        return "LiferayUser{" +
                "id=" + id +
                ", userId=" + userId +
                ", screenName='" + screenName + '\'' +
                ", emailAddress='" + emailAddress + '\'' +
                '}';
    }

}