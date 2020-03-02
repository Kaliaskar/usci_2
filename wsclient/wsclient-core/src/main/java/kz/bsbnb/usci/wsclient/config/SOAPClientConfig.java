package kz.bsbnb.usci.wsclient.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

@Configuration
public class SOAPClientConfig {

    @Value("${wsclient.kgdUrl}")
    private String kgdUrl;

    @Bean
    Jaxb2Marshaller nsiJaxb2Marshaller() {
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setContextPath("kz.bsbnb.usci.wsclient.jaxb.nsi");

        return jaxb2Marshaller;
    }

    @Bean
    Jaxb2Marshaller kgdJaxb2Marshaller() {
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setContextPath("kz.bsbnb.usci.wsclient.jaxb.kgd");

        return jaxb2Marshaller;
    }

    @Bean
    public WebServiceTemplate nsiWebServiceTemplate() {
        WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
        webServiceTemplate.setMarshaller(nsiJaxb2Marshaller());
        webServiceTemplate.setUnmarshaller(nsiJaxb2Marshaller());
        webServiceTemplate.setDefaultUri("https://nbportal.nationalbank.kz:443/WebService/NSI_NBRK");
        webServiceTemplate.setMessageSender(httpComponentsMessageSender());

        return webServiceTemplate;
    }

    @Bean
    public WebServiceTemplate kgdWebServiceTemplate() {
        WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
        webServiceTemplate.setMarshaller(kgdJaxb2Marshaller());
        webServiceTemplate.setUnmarshaller(kgdJaxb2Marshaller());
        webServiceTemplate.setDefaultUri("http://"+kgdUrl+"/EiccToKgdUniversal");
        webServiceTemplate.setMessageSender(httpComponentsMessageSender());

        ClientInterceptor[] interceptors =
                new ClientInterceptor[] {new LogClientInterceptor()};
        webServiceTemplate.setInterceptors(interceptors);

        return webServiceTemplate;
    }

    @Bean
    public HttpComponentsMessageSender httpComponentsMessageSender() {
        HttpComponentsMessageSender httpComponentsMessageSender = new HttpComponentsMessageSender();
        httpComponentsMessageSender.setReadTimeout(3000000);
        httpComponentsMessageSender.setConnectionTimeout(3000000);

        return httpComponentsMessageSender;
    }

    @Bean
    public HttpComponentsMessageSender httpProxyComponentsMessageSender() {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope("10.10.32.14",3128), new UsernamePasswordCredentials("bakash.yernur","3791364Ss"));
        HttpClient client = HttpClients.custom()
                .addInterceptorFirst(new HttpComponentsMessageSender.RemoveSoapHeadersInterceptor())
                .setDefaultCredentialsProvider(credentialsProvider)
                .setProxy(new HttpHost("10.10.32.14", 3128, "http"))
                .build();

        HttpComponentsMessageSender httpComponentsMessageSender = new HttpComponentsMessageSender(client);
        //httpComponentsMessageSender.setReadTimeout(3000000);
//        httpComponentsMessageSender.setConnectionTimeout(3000000);

        return httpComponentsMessageSender;
    }

}
