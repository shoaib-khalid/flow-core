package com.kalsym.flowcore.services;

import com.kalsym.flowcore.VersionHolder;
import com.kalsym.flowcore.models.pushmessages.*;
import com.kalsym.flowcore.utils.Logger;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Sarosh
 */
@Service
public class MessageSender {

    public String sendMessage(PushMessage message, String url, String refId, boolean isGuest) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        String res = null;
        String logprefix = refId;
        try {

            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "url: " + url);

            message.setGuest(isGuest);

            TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

            SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                    .loadTrustMaterial(null, acceptingTrustStrategy)
                    .build();

            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(csf)
                    .build();

            HttpComponentsClientHttpRequestFactory requestFactory
                    = new HttpComponentsClientHttpRequestFactory();

            requestFactory.setHttpClient(httpClient);

            RestTemplate restTemplate = new RestTemplate(requestFactory);

            ResponseEntity<String> response = restTemplate.postForEntity(url, message, String.class);
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "response: " + response.getBody());
            res = response.getBody();

        } catch (RestClientException e) {
            Logger.application.error("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "response: " + e.getMessage(), e);
            res = e.getMessage();
        }
        return res;
    }
}
