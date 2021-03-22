package com.kalsym.flowcore.services;

import com.kalsym.flowcore.VersionHolder;
import com.kalsym.flowcore.models.pushmessages.*;
import com.kalsym.flowcore.utils.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Sarosh
 */
@Service
public class MessageSender {

    public String sendMessage(PushMessage message, String url, String refId, boolean isGuest) {
        String res = null;
        String logprefix = refId;
        try {

            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "url: " + url);

            message.setGuest(isGuest);
            RestTemplate restTemplate = new RestTemplate();
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
