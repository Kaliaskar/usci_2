package kz.bsbnb.usci.model.respondent;

import kz.bsbnb.usci.model.persistence.Persistable;

/**
 * @author Olzhas Kaliaskar
 */

public class ConfirmMessageFile extends Persistable {
    private Long messageId;
    private String fileName;
    private byte[] content;

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
    
}
