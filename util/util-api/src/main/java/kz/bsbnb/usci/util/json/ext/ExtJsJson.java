package kz.bsbnb.usci.util.json.ext;

/**
 * @author Jandos Iskakov
 */

public class ExtJsJson {
    private boolean success = true;
    private Object data;
    private String errorMessage;

    public ExtJsJson() {
        // Пустой конструктор
    }

    public ExtJsJson(Object data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}
