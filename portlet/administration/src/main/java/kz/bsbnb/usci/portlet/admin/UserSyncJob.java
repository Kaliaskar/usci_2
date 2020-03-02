package kz.bsbnb.usci.portlet.admin;

import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageListener;
import com.liferay.portal.kernel.messaging.MessageListenerException;
import com.liferay.portal.service.UserLocalServiceUtil;
import kz.bsbnb.usci.model.adm.User;
import kz.bsbnb.usci.model.exception.UsciException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserSyncJob implements MessageListener {
    private final Logger logger = LoggerFactory.getLogger(UserSyncJob.class);

    @Override
    public void receive(Message message) throws MessageListenerException {
        try {
            List<com.liferay.portal.model.User> portalUsers = UserLocalServiceUtil.getUsers(0, UserLocalServiceUtil.getUsersCount());
            List<User> users = new ArrayList<>(portalUsers.size());
            for (com.liferay.portal.model.User user : portalUsers) {
                users.add(convert(user));
            }

            boolean https = true;
            if (https) {
                String url = "https://10.8.1.134:28765/api/usci/core/user/syncUsers";

                TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
                    @Override
                    public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        return true;
                    }
                };

                SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                        .loadTrustMaterial(null, acceptingTrustStrategy)
                        .build();

                SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);


                CloseableHttpClient httpClient = HttpClients.custom()
                        .setSSLSocketFactory(csf)
                        .build();
                HttpComponentsClientHttpRequestFactory requestFactory
                        = new HttpComponentsClientHttpRequestFactory();
                requestFactory.setHttpClient(httpClient);

                RestTemplate restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(requestFactory));
                restTemplate.setInterceptors(Collections.<ClientHttpRequestInterceptor>singletonList(new RequestResponseLoggingInterceptor()));
                ResponseEntity<String> response = restTemplate.postForEntity(url, users, String.class);
            } else {
                String url = "http://localhost:28765/api/usci/core/user/syncUsers";
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.setInterceptors(Collections.<ClientHttpRequestInterceptor>singletonList(new RequestResponseLoggingInterceptor()));
                ResponseEntity<String> response = restTemplate.postForEntity(url, users, String.class);
            }
        } catch (Exception e) {
            logger.error("Failed to synchronize portal users with database", e);
        }
    }

    private User convert(com.liferay.portal.model.User user) throws IllegalArgumentException {
        if (user == null)
            throw new UsciException("Liferay пользователя не может быть пустым. Параметр <Liferay пользователя> не может быть null");

        User portalUser = new User();
        portalUser.setUserId(user.getUserId());
        portalUser.setEmailAddress(user.getEmailAddress());
        portalUser.setModifiedDate(null);//SqlJdbcConverter.convertToLocalDate(liferayUser.getModifiedDate()));
        portalUser.setFirstName(user.getFirstName());
        portalUser.setLastName(user.getLastName());
        portalUser.setMiddleName(user.getMiddleName());
        portalUser.setScreenName(user.getScreenName());
        portalUser.setActive(user.isActive());

        return portalUser;
    }
}
