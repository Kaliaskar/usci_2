package kz.bsbnb.usci.model.respondent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * author Jandos Iskakov
 */

public class ConfirmMessageJson {
    private Long id;
    private Long confirmId;
    private LocalDateTime sendDate;
    private String text;
    private Long userId;
    private String userName;
    private List<ConfirmMsgFileJson> files = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConfirmId() {
        return confirmId;
    }

    public void setConfirmId(Long confirmId) {
        this.confirmId = confirmId;
    }

    public LocalDateTime getSendDate() {
        return sendDate;
    }

    public void setSendDate(LocalDateTime sendDate) {
        this.sendDate = sendDate;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public List<ConfirmMsgFileJson> getFiles() {
        return files;
    }

    public void setFiles(List<ConfirmMsgFileJson> files) {
        this.files = files;
    }

}
