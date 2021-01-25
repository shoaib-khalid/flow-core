package com.kalsym.flowcore.services;

import com.kalsym.flowcore.VersionHolder;
import com.kalsym.flowcore.models.pushmessages.*;
import com.kalsym.flowcore.utils.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Sarosh
 */
@Service
public class MessageSender {

    public String sendMessage(PushMessage message, String url, String refId, boolean isGuest) throws Exception {
        String logprefix = refId;
        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "url: " + url);

        message.setGuest(isGuest);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(url, message, String.class);
        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "response: " + response.getBody());
        return response.getBody();
    }
}
