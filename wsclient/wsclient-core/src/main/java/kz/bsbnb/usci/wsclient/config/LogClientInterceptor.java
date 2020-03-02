package kz.bsbnb.usci.wsclient.config;

import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;

public class LogClientInterceptor implements ClientInterceptor {

    @Override
    public void afterCompletion(MessageContext arg0, Exception arg1)
            throws WebServiceClientException {
        //HttpLoggingUtils.showTheMessageINeed("Some text!!!");
    }

    @Override
    public boolean handleFault(MessageContext messageContext) throws WebServiceClientException {
        //HttpLoggingUtils.logMessage("Fault Message", messageContext.getResponse());
        return true;
    }

    @Override
    public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
        //HttpLoggingUtils.logMessage("Client Request Message", messageContext.getRequest());

        return true;
    }

    @Override
    public boolean handleResponse(MessageContext messageContext) throws WebServiceClientException {
        //HttpLoggingUtils.logMessage("Client Response Message", messageContext.getResponse());

        return true;
    }
}
