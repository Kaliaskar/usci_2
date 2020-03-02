package kz.bsbnb.usci.wsclient.model.currency;

import java.time.LocalDate;

public class CurrencyEntityCustom {
    private Long CurrId;
    private String CurrCode;
    private LocalDate CourseDate;
    private Long CourseKind;
    private Double Course;
    private Long Corellation;
    private Long WdKind;

    public Long getCurrId() {
        return CurrId;
    }

    public void setCurrId(Long currId) {
        CurrId = currId;
    }

    public String getCurrCode() {
        return CurrCode;
    }

    public void setCurrCode(String currCode) {
        CurrCode = currCode;
    }

    public LocalDate getCourseDate() {
        return CourseDate;
    }

    public void setCourseDate(LocalDate courseDate) {
        CourseDate = courseDate;
    }

    public Long getCourseKind() {
        return CourseKind;
    }

    public void setCourseKind(Long courseKind) {
        CourseKind = courseKind;
    }

    public Double getCourse() {
        return Course;
    }

    public void setCourse(Double course) {
        Course = course;
    }

    public Long getCorellation() {
        return Corellation;
    }

    public void setCorellation(Long corellation) {
        Corellation = corellation;
    }

    public Long getWdKind() {
        return WdKind;
    }

    public void setWdKind(Long wdKind) {
        WdKind = wdKind;
    }
}
