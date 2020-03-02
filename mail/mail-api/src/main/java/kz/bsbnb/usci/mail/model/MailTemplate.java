package kz.bsbnb.usci.mail.model;

import kz.bsbnb.usci.model.persistence.Persistable;

/**
 * @author Aidar Myrzahanov
 */

public class MailTemplate extends Persistable {
    public static final String FILE_PROCESSING_COMPLETED = "FILE_PROCESSING_COMPLETED";
    public static final String MAINTENANCE_REQUEST = "MAINTENANCE_REQUEST";

    private String subject;
    private String text;
    private String code;
    private String nameRu;
    private String nameKz;
    private Long typeId;

    public MailTemplate() {
        super();
    }

    public MailTemplate(Long id) {
        super(id);
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNameRu() {
        return nameRu;
    }

    public void setNameRu(String nameRu) {
        this.nameRu = nameRu;
    }

    public String getNameKz() {
        return nameKz;
    }

    public void setNameKz(String nameKz) {
        this.nameKz = nameKz;
    }

    public Long getTypeId() {
        return typeId;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }

    @Override
    public String toString() {
        return "MailTemplate{" +
                "subject='" + (subject != null? subject.substring(0, 16): null) + '\'' +
                ", text='" + (text != null? text.substring(0, 16): null) + '\'' +
                ", code='" + code + '\'' +
                ", nameRu='" + nameRu + '\'' +
                ", nameKz='" + nameKz + '\'' +
                ", typeId=" + typeId +
                '}';
    }

}
