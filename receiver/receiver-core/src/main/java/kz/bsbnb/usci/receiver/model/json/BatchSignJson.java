package kz.bsbnb.usci.receiver.model.json;

import java.time.LocalDateTime;

public class BatchSignJson {
    private Long id;
    private String fileName;
    private LocalDateTime receiverDate;
    private String hash;
    private String signature;
    private String information;
    private LocalDateTime signingTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getReceiverDate() {
        return receiverDate;
    }

    public void setReceiverDate(LocalDateTime receiverDate) {
        this.receiverDate = receiverDate;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getInformation() {
        return information;
    }

    public void setInformation(String information) {
        this.information = information;
    }

    public LocalDateTime getSigningTime() {
        return signingTime;
    }

    public void setSigningTime(LocalDateTime signingTime) {
        this.signingTime = signingTime;
    }
}
