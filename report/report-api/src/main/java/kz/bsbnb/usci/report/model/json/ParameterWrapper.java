package kz.bsbnb.usci.report.model.json;

import java.io.Serializable;
import java.util.List;

public class ParameterWrapper implements Serializable {
    private List<InputParametersJson> parameters;

    public List<InputParametersJson> getParameters() {
        return parameters;
    }

    public void setParameters(List<InputParametersJson> parameters) {
        this.parameters = parameters;
    }
}
