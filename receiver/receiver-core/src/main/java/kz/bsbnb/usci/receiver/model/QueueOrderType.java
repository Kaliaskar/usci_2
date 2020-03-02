package kz.bsbnb.usci.receiver.model;

public enum QueueOrderType {
    CHRONOLOGICAL,
    CREDITOR_CYCLE,
    MINIMUM_WEIGHT;

    public String code() {
        return name();
    }

}
