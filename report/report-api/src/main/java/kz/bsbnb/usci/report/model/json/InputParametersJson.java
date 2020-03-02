package kz.bsbnb.usci.report.model.json;


public class InputParametersJson {

    private  Long order;
    private  String name;
    private  String display;
    private  Object value;
    private  String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }




    public Long getOrder() {
        return order;
    }

    public void setOrder(Long order) {
        this.order = order;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

}
