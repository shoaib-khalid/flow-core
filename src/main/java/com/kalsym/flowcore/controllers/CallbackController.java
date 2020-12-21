package com.kalsym.flowcore.controllers;

import com.kalsym.flowcore.daos.models.*;
import com.kalsym.flowcore.models.pushmessages.*;
import com.kalsym.flowcore.daos.models.*;
import com.kalsym.flowcore.daos.models.vertexsubmodels.*;
import com.kalsym.flowcore.daos.repositories.ConversationsRepostiory;
import com.kalsym.flowcore.daos.repositories.VerticesRepostiory;
import com.kalsym.flowcore.models.*;
import com.kalsym.flowcore.utils.LogUtil;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.kalsym.flowcore.daos.repositories.FlowsRepostiory;
import com.kalsym.flowcore.models.enums.VertexType;
import com.kalsym.flowcore.services.ConversationHandler;
import com.kalsym.flowcore.services.MessageSender;
import com.kalsym.flowcore.services.VerticesHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Sarosh
 */
@RestController()
@RequestMapping("/callback")
public class CallbackController {

    @Autowired
    private VerticesRepostiory verticesRepostiory;

    @Autowired
    private FlowsRepostiory flowsRepostiory;

    @Autowired
    private ConversationsRepostiory conversationsRepostiory;

    @Autowired
    private ConversationHandler conversationHandler;

    @Autowired
    private VerticesHandler verticesHandler;

    @Autowired
    private MessageSender messageSender;

    /**
     * Postback receives id targetId in the payload as data.
     *
     * @param request for logging
     * @param senderId
     * @param refrenceId is botId or flowId
     * @param requestBody
     * @return
     */
    @PostMapping(path = {"/postaback/", "/postaback"}, name = "callback-postback-post")
    public ResponseEntity<HttpReponse> postback(HttpServletRequest request,
            @RequestParam(name = "senderId", required = true) String senderId,
            @RequestParam(name = "refrenceId", required = true) String refrenceId,
            @RequestBody(required = true) RequestPayload requestBody) {
        String logprefix = request.getRequestURI() + " ";
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

        LogUtil.info(logprefix, logLocation, "queryString: " + request.getQueryString(), "");

        try {
            Conversation conversation = conversationHandler.getConversation(senderId, refrenceId);
            LogUtil.info(logprefix, logLocation, "conversationId: " + conversation.getId(), "");

            Vertex nextVertex = conversationHandler.getNextVertex(conversation, requestBody.getData());

            List<String> recipients = new ArrayList<>();
            recipients.add(senderId);
            PushMessage pushMessage = verticesHandler.getPushMessage(nextVertex, recipients, senderId);
            response.setData(pushMessage);

            String url = "";
            if (VertexType.MENU_MESSAGE == nextVertex.getInfo().getType()) {
                url = requestBody.getCallbackUrl() + "callback/pushMenuMessage";
            }

            if (VertexType.TEXT_MESSAGE == nextVertex.getInfo().getType()) {
                url = requestBody.getCallbackUrl() + "callback/pushSimpleMessage";
            }
            messageSender.sendMessage(pushMessage, url);
            conversation.setLatestVertexId(nextVertex.getId());
            conversationHandler.shiftVertex(conversation, nextVertex);
        } catch (Exception e) {
            LogUtil.error(logprefix, logLocation, "Error processing request params", "", e);
        }

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Message received as plain string in payload as data.
     *
     * @param request for logging
     * @param senderId
     * @param refrenceId is botId or flowId
     * @param requestBody
     * @return
     */
    @PostMapping(path = {"/message/", "/message"}, name = "callback-message-post")
    public ResponseEntity<HttpReponse> message(HttpServletRequest request,
            @RequestParam(name = "senderId", required = true) String senderId,
            @RequestParam(name = "refrenceId", required = true) String refrenceId,
            @RequestBody(required = true) RequestPayload requestBody) {
        String logprefix = request.getRequestURI() + " ";
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());
        LogUtil.info(logprefix, logLocation, "queryString: " + request.getQueryString(), "");

        try {
            Conversation conversation = conversationHandler.getConversation(senderId, refrenceId);
            LogUtil.info(logprefix, logLocation, "conversationId: " + conversation.getId(), "");

            Vertex nextVertex = conversationHandler.getNextVertex(conversation, requestBody.getData());
            String url = "";

            List<String> recipients = new ArrayList<>();
            recipients.add(senderId);
            PushMessage pushMessage = verticesHandler.getPushMessage(nextVertex, recipients, senderId);
            response.setData(pushMessage);
            if (VertexType.MENU_MESSAGE == nextVertex.getInfo().getType()) {
                url = requestBody.getCallbackUrl() + "callback/pushMenuMessage";
            }

            if (VertexType.TEXT_MESSAGE == nextVertex.getInfo().getType()) {
                url = requestBody.getCallbackUrl() + "callback/pushSimpleMessage";
            }
            messageSender.sendMessage(pushMessage, url);
            conversation.setLatestVertexId(nextVertex.getId());
            conversationHandler.shiftVertex(conversation, nextVertex);
        } catch (Exception e) {
            LogUtil.error(logprefix, logLocation, "Error processing request params", "", e);
        }

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
