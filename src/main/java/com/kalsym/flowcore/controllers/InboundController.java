package com.kalsym.flowcore.controllers;

import com.kalsym.flowcore.VersionHolder;
import com.kalsym.flowcore.daos.models.*;
import com.kalsym.flowcore.daos.repositories.ConversationsRepostiory;
import com.kalsym.flowcore.models.*;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.kalsym.flowcore.services.ConversationHandler;
import com.kalsym.flowcore.utils.Logger;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author Sarosh
 */
@RestController()
@RequestMapping("/inbound")
public class InboundController {

    @Autowired
    private ConversationHandler conversationHandler;
    
    @Autowired
    private ConversationsRepostiory conversationsRepostiory;



    /**
     * Postback receives id targetId in the payload as data.
     *
     * @param request for logging
     * @param senderId
     * @param refrenceId is botId or flowId
     * @param requestBody
     * @return
     */
    @PostMapping(path = {"/"}, name = "callback-postback-post")
    public ResponseEntity<HttpReponse> postback(HttpServletRequest request,
            @RequestParam(name = "senderId", required = true) String senderId,
            @RequestParam(name = "refrenceId", required = true) String refrenceId,
            @RequestBody(required = true) RequestPayload requestBody) throws Exception {
        String logprefix = senderId;
        HttpReponse response = new HttpReponse(request.getRequestURI());

        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "queryString: " + request.getQueryString());
        if (null != requestBody) {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "body: " + requestBody.toString());

        }

        Conversation conversation = null;

        try {
            conversation = conversationHandler.getConversation(senderId, refrenceId);

            

            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "conversationId: " + conversation.getId());

        } catch (Exception e) {
            Logger.application.error("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "Error retrieving conversation", e);
            response.setSuccessStatus(HttpStatus.OK, e.getMessage());
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }

        try {
            conversation = conversationHandler.processConversastion(conversation, requestBody);
            response.setSuccessStatus(HttpStatus.ACCEPTED);
        } catch (InterruptedException e) {
            Logger.application.error("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "Error processiong conversation", e);
            response.setSuccessStatus(HttpStatus.OK, e.getMessage());
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }

        response.setSuccessStatus(HttpStatus.OK);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
