package kz.bsbnb.usci.mail.model;

import java.util.ArrayList;
import java.util.List;

public class UserMailTemplateList {
    private List<UserMailTemplate> userMailTemplateList = new ArrayList<>();

    public List<UserMailTemplate> getUserMailTemplateList() {
        return userMailTemplateList;
    }

    public void setUserMailTemplateList(List<UserMailTemplate> userMailTemplateList) {
        this.userMailTemplateList = userMailTemplateList;
    }

}
