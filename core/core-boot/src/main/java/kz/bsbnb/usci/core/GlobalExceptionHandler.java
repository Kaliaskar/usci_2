package kz.bsbnb.usci.core;

import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.util.json.ext.ExtJsJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * @author Raziya Azhgireyeva
 * Перехватчик ошибок для ExtJs
 */

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String JSON_V1 = "application/vnd.captech-v1.0+json";
    private static final String JSON_V2 = "application/vnd.captech-v2.0+json";

    @RequestMapping(produces = {JSON_V1, JSON_V2})
    @ExceptionHandler(UsciException.class)
    @ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
    @ResponseBody
    public ExtJsJson handleUncaughtException(UsciException ex) {
        logger.error("Ошибка", ex);
        return buildError(ex);
    }

    private ExtJsJson buildError(Throwable ex) {
        ExtJsJson error = new ExtJsJson();
        error.setSuccess(false);
        error.setErrorMessage(ex.getMessage());

        return error;
    }

}
