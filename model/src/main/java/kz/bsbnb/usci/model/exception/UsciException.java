package kz.bsbnb.usci.model.exception;

import java.util.*;

/**
 * @author Jandos Iskakov
 */

public class UsciException extends RuntimeException {
    private String errorCode;
    private Collection<String> errorMessages = new ArrayList<>();
    protected Map<String, Object> params = new HashMap<>();

    public UsciException() {
        super();
    }

    public UsciException(Throwable cause) {
        super(cause);
    }

    public UsciException(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public UsciException(Set<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public UsciException(String message) {
        super(message);
        this.errorMessages.add(message);
    }

    public UsciException(String message, Throwable cause) {
        super(message, cause);
        this.errorMessages.add(message);
    }

    @Override
    public String getMessage() {
        // так как в классе RuntimeException нет setMessage текста ошибки храню в переменной errorMessage
        // используется в ClosedBaseEntityException, UndefinedBaseEntityException
        if (errorMessages != null && errorMessages.size() == 1)
            return errorMessages.stream().findFirst().get();
        else if (errorMessages != null)
            return errorMessages.toString();

        return super.getMessage();
    }

    public Collection<String> getErrorMessages() {
        return errorMessages;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessages.clear();
        this.errorMessages.add(errorMessage);
    }

}
