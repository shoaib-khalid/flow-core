package com.kalsym.flowcore.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalsym.flowcore.utils.LogUtil;
import org.springframework.stereotype.Service;

/**
 *
 * @author Sarosh
 */
@Service
public class MessageDecoder {

    public ReceivedEvent getReceivedEvent(String json) {
        String logprefix = "";
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();

        ReceivedEvent receivedEvent = new ReceivedEvent();

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            receivedEvent = objectMapper.readValue(json, ReceivedEvent.class);
        } catch (JsonProcessingException e) {
            LogUtil.error(logprefix, logLocation, "Error processing JSON", "", e);
        }

        return receivedEvent;
    }
}
