package kz.bsbnb.usci.model.respondent;

import kz.bsbnb.usci.model.persistence.Persistable;

import java.time.LocalDateTime;

/**
 * @author Jandos Iskakov
 */

public class ConfirmStage extends Persistable {
    private Long confirmId;
    private ConfirmStatus status;
    private LocalDateTime stageDate;
    private Long userId;
    private String signature;
    private byte[] document;
    private Long userPosId;
    private String docHash;
    private String signInfo;

    public ConfirmStage() {
        super();
    }

    @Override
    public ConfirmStage setId(Long id) {
        super.setId(id);
        return this;
    }

    public Long getConfirmId() {
        return confirmId;
    }

    public ConfirmStage setConfirmId(Long confirmId) {
        this.confirmId = confirmId;
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public ConfirmStage setUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public ConfirmStatus getStatus() {
        return status;
    }

    public ConfirmStage setStatus(ConfirmStatus status) {
        this.status = status;
        return this;
    }

    public LocalDateTime getStageDate() {
        return stageDate;
    }

    public ConfirmStage setStageDate(LocalDateTime stageDate) {
        this.stageDate = stageDate;
        return this;
    }

    public String getSignature() {
        return signature;
    }

    public ConfirmStage setSignature(String signature) {
        this.signature = signature;
        return this;
    }

    public byte[] getDocument() {
        return document;
    }

    public ConfirmStage setDocument(byte[] document) {
        this.document = document;
        return this;
    }

    public Long getUserPosId() {
        return userPosId;
    }

    public ConfirmStage setUserPosId(Long userPosId) {
        this.userPosId = userPosId;
        return this;
    }

    public String getDocHash() {
        return docHash;
    }

    public ConfirmStage setDocHash(String docHash) {
        this.docHash = docHash;
        return this;
    }

    public String getSignInfo() {
        return signInfo;
    }

    public ConfirmStage setSignInfo(String signInfo) {
        this.signInfo = signInfo;
        return this;
    }

}
