package kz.bsbnb.usci.receiver.model;

import kz.bsbnb.usci.model.persistence.Persistable;

import java.time.LocalDateTime;

/**
 * @author Maksat Nussipzhan
 */

public class BatchStatus extends Persistable {
    private static final long serialVersionUID = 1L;

    private Long batchId;
    private String text;
    private LocalDateTime receiptDate;
    private BatchStatusType status;
    private String exceptionTrace;

    public BatchStatus() {
        super();
    }

    public BatchStatus(Long batchId, BatchStatusType status) {
        this.batchId = batchId;
        this.receiptDate = LocalDateTime.now();
        this.status = status;
    }

    public Long getBatchId() {
        return batchId;
    }

    public BatchStatus setBatchId(Long batchId) {
        this.batchId = batchId;
        return this;
    }

    public String getText() {
        return text;
    }

    public BatchStatus setText(String text) {
        if (text != null) {
            if (text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 512) {
                this.text = text;
            } else {
                this.text = text.substring(0, 256);
            }
        } else {
            this.text = null;
        }

        return this;
    }

    public String getExceptionTrace() {
        return exceptionTrace;
    }

    public BatchStatus setExceptionTrace(String exceptionTrace) {
        this.exceptionTrace = exceptionTrace;
        return this;
    }

    public LocalDateTime getReceiptDate() {
        return receiptDate;
    }

    public BatchStatus setReceiptDate(LocalDateTime receiptDate) {
        this.receiptDate = receiptDate;
        return this;
    }

    public BatchStatusType getStatus() {
        return status;
    }

    public BatchStatus setStatus(BatchStatusType status) {
        this.status = status;
        return this;
    }

}
