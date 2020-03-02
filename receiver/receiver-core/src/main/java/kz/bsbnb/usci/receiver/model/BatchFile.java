package kz.bsbnb.usci.receiver.model;

/**
 * @author Jandos Iskakov
 * DTO обьект передаваемый в метод для обработки батчей
 */

public class BatchFile {
    private String fileName;
    private Long respondentId;
    private byte[] fileContent;
    private Long userId;
    private Boolean isNb;
    private String filePath;
    private Long batchEntryId;

    public BatchFile() {
    }

    public BatchFile(Long userId, Boolean isNb, String filePath) {
        this.userId = userId;
        this.isNb = isNb;
        this.filePath = filePath;
    }

    public Boolean getNb() {
        return isNb;
    }

    public void setNb(Boolean nb) {
        isNb = nb;
    }

    public Long getUserId() {
        return userId;
    }

    public BatchFile setUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public BatchFile setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public BatchFile setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public Long getRespondentId() {
        return respondentId;
    }

    public BatchFile setRespondentId(Long respondentId) {
        this.respondentId = respondentId;
        return this;
    }

    public String getFilePath() {
        return filePath;
    }

    public BatchFile setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public Long getBatchEntryId() {
        return batchEntryId;
    }

    public BatchFile setBatchEntryId(Long batchEntryId) {
        this.batchEntryId = batchEntryId;
        return this;
    }

    @Override
    public String toString() {
        return "BatchFile{" +
                "fileName='" + fileName + '\'' +
                ", respondentId=" + respondentId +
                ", userId=" + userId +
                ", isNb=" + isNb +
                ", filePath='" + filePath + '\'' +
                '}';
    }

}
