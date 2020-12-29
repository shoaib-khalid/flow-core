package com.kalsym.flowcore.services;

import com.kalsym.flowcore.models.pushmessages.*;
import com.kalsym.flowcore.utils.Logger;
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
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
        Logger.info(logprefix, logLocation, "url: " + url, "");
        message.setGuest(isGuest);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(url, message, String.class);
        Logger.info(logprefix, logLocation, "response: " + response.getBody(), "");
        return response.getBody();
    }
}
