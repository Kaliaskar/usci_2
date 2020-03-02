package kz.bsbnb.usci.mail.model;

/**
 * @author Aidar Myrzahanov
 */

public class Mail {
    private String senderEmail;
    private String receiverEmail;
    private String subject;
    private String text;

    public String getSenderEmail() {
        return senderEmail;
    }

    public Mail setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
        return this;
    }

    public String getReceiverEmail() {
        return receiverEmail;
    }

    public Mail setReceiverEmail(String receiverEmail) {
        this.receiverEmail = receiverEmail;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public Mail setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getText() {
        return text;
    }

    public Mail setText(String text) {
        this.text = text;
        return this;
    }

    @Override
    public String toString() {
        return "Mail{" +
                "senderEmail='" + senderEmail + '\'' +
                ", receiverEmail='" + receiverEmail + '\'' +
                ", subject='" + subject + '\'' +
                ", text='" + text + '\'' +
                '}';
    }

}
