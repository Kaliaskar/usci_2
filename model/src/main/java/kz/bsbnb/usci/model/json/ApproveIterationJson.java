package kz.bsbnb.usci.model.json;

public class ApproveIterationJson {
    private Long iterationNumber;
    private Long month;
    private Long day;
    private Long hour;
    private Long min;
    private boolean dayTransfer;

    public ApproveIterationJson() { super();
    }

    public Long getIterationNumber() {
        return iterationNumber;
    }

    public void setIterationNumber(Long iterationNumber) {
        this.iterationNumber = iterationNumber;
    }

    public Long getMonth() {
        return month;
    }

    public void setMonth(Long month) {
        this.month = month;
    }

    public Long getDay() {
        return day;
    }

    public void setDay(Long day) {
        this.day = day;
    }

    public Long getHour() {
        return hour;
    }

    public void setHour(Long hour) {
        this.hour = hour;
    }

    public Long getMin() {
        return min;
    }

    public void setMin(Long min) {
        this.min = min;
    }

    public boolean isDayTransfer() {
        return dayTransfer;
    }

    public void setDayTransfer(boolean dayTransfer) {
        this.dayTransfer = dayTransfer;
    }
}
