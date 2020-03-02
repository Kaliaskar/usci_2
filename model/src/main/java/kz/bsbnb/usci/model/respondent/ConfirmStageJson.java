package kz.bsbnb.usci.model.respondent;

import java.time.LocalDateTime;

/**
 * @author Jandos Iskakov
 */

public class ConfirmStageJson {
    private Long id;
    private Long confirmId;
    private Long statusId;
    private String statusName;
    private LocalDateTime stageDate;
    private Long userId;
    private String userName;
    private String signature;
    private byte[] document;
    private Long userPosId;
    private String userPosName;

    public ConfirmStageJson() {
        super();
    }

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

    public Long getStatusId() {
        return statusId;
    }

    public void setStatusId(Long statusId) {
        this.statusId = statusId;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public LocalDateTime getStageDate() {
        return stageDate;
    }

    public void setStageDate(LocalDateTime stageDate) {
        this.stageDate = stageDate;
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

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public byte[] getDocument() {
        return document;
    }

    public void setDocument(byte[] document) {
        this.document = document;
    }

    public Long getUserPosId() {
        return userPosId;
    }

    public void setUserPosId(Long userPosId) {
        this.userPosId = userPosId;
    }

    public String getUserPosName() {
        return userPosName;
    }

    public void setUserPosName(String userPosName) {
        this.userPosName = userPosName;
    }

}
