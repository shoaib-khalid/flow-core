package com.kalsym.flowcore.services;

import com.kalsym.flowcore.models.pushmessages.*;
import com.kalsym.flowcore.utils.LogUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Sarosh
 */
@Service
public class MessageSender {

    public String sendMessage(PushMessage message, String url) throws Exception {
        String logprefix = "";
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
        LogUtil.info(logprefix, logLocation, "url: " + url, "");

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(url, message, String.class);
        LogUtil.info(logprefix, logLocation, "response: " + response.getBody(), "");
        return response.getBody();
    }
}
