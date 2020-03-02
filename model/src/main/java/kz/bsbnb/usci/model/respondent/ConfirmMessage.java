package kz.bsbnb.usci.model.respondent;

import kz.bsbnb.usci.model.persistence.Persistable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olzhas Kaliaskar
 */

public class ConfirmMessage extends Persistable {
    private Confirm confirm;
    private Long confirmId;
    private Long userId;
    private LocalDateTime sendDate;
    private String text;
    private List<ConfirmMessageFile> files = new ArrayList<>();

    public Confirm getConfirm() {
        return confirm;
    }

    public void setConfirm(Confirm confirm) {
        this.confirm = confirm;
    }

    public Long getConfirmId() {
        return confirmId;
    }

    public void setConfirmId(Long confirmId) {
        this.confirmId = confirmId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public List<ConfirmMessageFile> getFiles() {
        return files;
    }

    public void setFiles(List<ConfirmMessageFile> files) {
        this.files = files;
    }
}
