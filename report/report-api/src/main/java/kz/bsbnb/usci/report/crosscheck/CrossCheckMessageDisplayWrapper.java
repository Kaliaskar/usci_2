package kz.bsbnb.usci.report.crosscheck;

public class CrossCheckMessageDisplayWrapper {

    private CrossCheckMessage crossCheckMessage;
    private String description;
    private String help;

    public CrossCheckMessageDisplayWrapper(CrossCheckMessage message) {
        this.crossCheckMessage = message;
        if (message.getDescription() != null) {
            String text = message.getDescription();
            int pos = text.indexOf("|");
            if (pos < 0) {
                description = text;
            } else {
                description = text.substring(0, pos);
                help = "<h2>" + text.substring(pos + 1) + "</h2>";
            }
        }
        else {
            description = message.getMessage().getNameRu();
        }

    }

    public String getDescription() {
        if (crossCheckMessage.getMessage() != null) {
            Message message = crossCheckMessage.getMessage();
            return  message.getNameRu();
        }

        return description;
    }

    public String getHelp() {
        if (crossCheckMessage.getMessage() != null)
            return crossCheckMessage.getMessage().getNote();

        return help;
    }

    public String getInnerValue() {
        return crossCheckMessage.getInnerValue();
    }

    public String getOuterValue() {
        return crossCheckMessage.getOuterValue();
    }

    public String getDifference() {
        return crossCheckMessage.getDiff();
    }

    public int getIsError() {
        return crossCheckMessage.getIsError() ? 1 : 0;
    }
    public int getNonCritical() {
        return crossCheckMessage.getIsNonCriticalError()?1:0;
    }
}
