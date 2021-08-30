package com.kalsym.flowcore.services;

import com.kalsym.flowcore.VersionHolder;
import com.kalsym.flowcore.daos.models.*;
import com.kalsym.flowcore.models.*;
import com.kalsym.flowcore.daos.repositories.ConversationsRepostiory;
import com.kalsym.flowcore.daos.repositories.FlowsRepostiory;
import com.kalsym.flowcore.daos.repositories.VerticesRepostiory;
import com.kalsym.flowcore.models.RequestPayload;
import com.kalsym.flowcore.models.enums.VertexType;
import com.kalsym.flowcore.models.pushmessages.PushMessage;
import com.kalsym.flowcore.utils.Logger;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.swagger.models.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Handles conversation based on sender and refrenceId.
 *
 * @author Sarosh
 */
@Service
public class ConversationHandler {

    @Autowired
    private ConversationsRepostiory conversationsRepostiory;

    @Autowired
    private FlowsRepostiory flowsRepostiory;

    @Autowired
    private VerticesRepostiory verticesRepostiory;

    @Autowired
    private VerticesHandler verticesHandler;

    @Autowired
    private MessageSender messageSender;

    @Value("${default.message:Chatbot is not configured yet.}")
    private String defaultMessage;

    @Value("${conversation.expiry:3600}")
    int conversationExpiry;

    @Value("${product.service.url:https://api.symplified.biz/product-service/v1/}")
    String PRODUCT_SERVICE_URL;
    /**
     * Return conversation of sender.If conversation does not exist returns a
     * new conversation.
     *
     * @param senderId
     * @param refrenceId
     * @return Latest conversation.
     * @throws java.lang.Exception
     */
    public Conversation getConversation(String senderId, String refrenceId) throws Exception {

        String logprefix = senderId;

        Conversation searchConvo = new Conversation();
        searchConvo.setSenderId(senderId);
        searchConvo.setRefrenceId(refrenceId);

        Example convoExample = Example.of(searchConvo,
                ExampleMatcher.matching()
                        .withIgnoreNullValues()
                        .withIgnorePaths("id")
                        .withStringMatcher(StringMatcher.EXACT)
                        .withIgnoreCase());

        List<Conversation> conversationList = conversationsRepostiory.findAll(convoExample, Sort.by("lastModifiedDate").descending());

        if (conversationList.size() > 0) {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "conversation found");

            Conversation conversation = conversationList.get(0);

            long currentTime = (new Date()).getTime();

            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "currentTime: " + currentTime);

            long lastModifiedTime = conversation.getLastModifiedDate().getTime();
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "lastModifiedTime: " + lastModifiedTime);

            long secondsSinceLastUpdate = (currentTime - lastModifiedTime) / 1000;

            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "secondsSinceLastUpdate: " + secondsSinceLastUpdate);
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "conversationExpiry: " + conversationExpiry);

            if (conversationExpiry < secondsSinceLastUpdate) {
                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "conversation expired");

                conversationsRepostiory.delete(conversation);
            } else {
                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "conversation not expired");

                return conversationList.get(0);
            }

        }
        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "conversation  not found");

        Conversation newConversation = new Conversation();

        newConversation.setSenderId(senderId);
        newConversation.setRefrenceId(refrenceId);



        //newConversation.setFlowId(refrenceId);
        Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "created conversation");

        return conversationsRepostiory.save(newConversation);

    }

    /**
     * Returns the latest vertex for a conversation. If vertex is not found it
     * it assigns the topVertexId of the flow to conversation.
     *
     * @param conversation
     * @return Latest Vertex for the conversation.
     * @throws Exception
     */
    public Vertex getLatestVertex(Conversation conversation) throws Exception {
        String logprefix = conversation.getSenderId();
        String vertexId = null;

        if (null != conversation.getData() && null != conversation.getData().getCurrentVertexId()) {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "current vertex found");

            vertexId = conversation.getData().getCurrentVertexId();
        } else {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "latest vertex not found");

            Optional<Flow> flowOpt = flowsRepostiory.findById(conversation.getFlowId());
            vertexId = flowOpt.get().getTopVertexId();
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "flow found with id: " + conversation.getFlowId());

            conversation.shiftVertex(vertexId);
            conversationsRepostiory.save(conversation);
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "updated currentVertexId: " + vertexId);
        }

        Optional<Vertex> vertexOpt = verticesRepostiory.findByFlowIdAndMxGraphId(conversation.getFlowId(),vertexId);

        if (!vertexOpt.isPresent()) {
            Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertex not found with id: " + vertexId);
            throw new NotFoundException();
        } else {
            return vertexOpt.get();
        }

    }

    /**
     * Returns next vertex based on conversation flow and inputData.Returns flow
     * top Vertex in case conversation does not have latestVertexId.
     *
     * @param conversation
     * @param requestBody
     * @return Next vertex.
     * @throws java.lang.InterruptedException
     */
    public Conversation processConversastion(Conversation conversation, RequestPayload requestBody) throws Exception {
        String inputData = requestBody.getData();
        String logprefix = conversation.getSenderId();
        Vertex vertex = null;
        Dispatch dispatch = null;


        try {

            if (null != conversation.getData() && null != conversation.getData().getCurrentVertexId()) {
                //existing conversation
                Optional<Vertex> optVertex = getCurrentVertex(conversation);
                if (!optVertex.isPresent()) {
                    Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertex not found");
                    sendDefaultMessage(conversation, requestBody.getCallbackUrl());
                    return conversation;
                }
                vertex = optVertex.get();
                if (vertex.getIsLastVertex() != null && vertex.getIsLastVertex() == 1) {
                    conversation.getData().setCurrentVertexId(null);
                    Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "cleared currentVertex from conversation");
                    return processConversastion(conversation, requestBody);
                }
                dispatch = verticesHandler.processVertex(conversation, vertex, inputData);
            } else {
                //new conversation
                Optional<Vertex> optVertex = getCurrentVertex(conversation);
                if (!optVertex.isPresent()) {
                    Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertex not found");
                    sendDefaultMessage(conversation, requestBody.getCallbackUrl());
                    return conversation;
                }
                vertex = optVertex.get();

                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, "vertex info : ", vertex.toString());

                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, "flow ID : ", vertex.getFlowId());

                Optional<Flow> optionalFlow = flowsRepostiory.findById(vertex.getFlowId());
                if(optionalFlow.isPresent()){
                    Flow testFlow = optionalFlow.get();
                    Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, "flow details : ", testFlow.toString());

                    String storeId = testFlow.storeId;

                    Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, "Store id : ", storeId);
                    String STORE_ASSET_URL = PRODUCT_SERVICE_URL+"stores/"+storeId+"/assets";
                    RestTemplate getStoreDetailsRequest = new RestTemplate();
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Authorization", "Bearer accessToken");
                    HttpEntity getAssetsRequestBody = new HttpEntity(headers);

                    try{
                        ResponseEntity<String> getAssetsResponse = getStoreDetailsRequest.exchange(STORE_ASSET_URL, HttpMethod.GET, getAssetsRequestBody, String.class);
                        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, "data response  : ", getAssetsResponse.getBody().toString());

                        JSONObject assetData = new JSONObject(getAssetsResponse.getBody()).getJSONObject("data");
                        //set logo url
                        conversation.setVariableValue("logoUrl", assetData.getString("logoUrl"));

                        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, "data : ", assetData);
                    }catch (Exception e){
                        Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, "COULD NOT FETCH STORE ASSETS", "SETTING DEFAULT ASSET VALUES");
                        conversation.setVariableValue("logoUrl", "https://www.techopedia.com/images/uploads/6e13a6b3-28b6-454a-bef3-92d3d5529007.jpeg");
                    }finally {
                        conversation.setVariableValue("urlType", "IMAGE");
                    }



                }



                dispatch = new Dispatch(vertex, conversation.getData(), logprefix, conversation.getRefrenceId());

            }

        } catch (Exception e) {
            Logger.application.error("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "could not get current vertex ", e);
            sendDefaultMessage(conversation, requestBody.getCallbackUrl());
            return conversation;
        }

        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "requestBody.getIsGuest(): " + requestBody.getIsGuest());
        conversation.setIsGuest(requestBody.getIsGuest());

        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "isLastVeretex: " + vertex.getIsLastVertex());

        if (vertex.getIsLastVertex() != null && vertex.getIsLastVertex() == 1) {
            conversation.getData().setCurrentVertexId(null);
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "cleared currentVertex from conversation");
            return processConversastion(conversation, requestBody);
        }

        /*
        //Vertex nextVertex = null;
        if (null != conversation.getData() && null != conversation.getData().getCurrentVertexId()) {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "currentVertexId: " + conversation.getData().getCurrentVertexId());

            Optional<Vertex> optVertex = verticesRepostiory.findById(conversation.getData().getCurrentVertexId());

            //if last vertex
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "isLastVeretex: " + vertex.getIsLastVertex());

            
            if (vertex != null && vertex.getIsLastVertex() != null && vertex.getIsLastVertex() == 1) {
                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "currentVertex is last vertex");
                if (conversation.getFlowId() != null) {
                    Optional<Flow> optFlow = flowsRepostiory.findById(conversation.getFlowId());
                    //TODO: add default reply
                    if (!optFlow.isPresent()) {
                        Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "no flow with id: " + conversation.getFlowId());
                        sendDefaultMessage(conversation, requestBody.getCallbackUrl());
                        return conversation;
                    }

                    Flow flow = optFlow.get();


                    vertex = optVertex.get();
                    Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "flow topVertexId: " + flow.getTopVertexId());

                    dispatch = new Dispatch(vertex, conversation.getData(), logprefix, conversation.getRefrenceId());
                    Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "assigned currentVertexId: " + vertex.getId());
                }
            } else {
                //if vertex is not last vertex
                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "currentVertex is not last vertex");

                //check if vertez is present
                if (!optVertex.isPresent()) {
                    Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertex not found with Id: " + conversation.getData().getCurrentVertexId());
                    //If vertex not preset shift to top vertex again
                    Optional<Flow> optFlow = flowsRepostiory.findById(conversation.getFlowId());
                    //TODO: add default reply
                    if (!optFlow.isPresent()) {
                        Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "no flow with idd: " + conversation.getFlowId());
                        sendDefaultMessage(conversation, requestBody.getCallbackUrl());
                        return conversation;
                    }

                    Flow flow = optFlow.get();
                    Optional<Vertex> optTopVertex = verticesRepostiory.findById(flow.getTopVertexId());
                    if (!optTopVertex.isPresent()) {
                        Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "top vertex not found with Id: " + flow.getTopVertexId());
                        sendDefaultMessage(conversation, requestBody.getCallbackUrl());
                        return conversation;
                    }
                    Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "shifted comversation to top vertex with id: " + flow.getTopVertexId());

                    vertex = optTopVertex.get();
                    dispatch = new Dispatch(vertex, conversation.getData(), logprefix, conversation.getRefrenceId());

                } else {
                    vertex = optVertex.get();
                    dispatch = verticesHandler.processVertex(conversation, vertex, inputData);
                }

            }

        } else {
            //if conversation does not have latestVertexId consider it a new 
            //conversation and attach flow's topVertexId to conversation's 
            //latestVertexId
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "currentVertexId not present");

            String[] botIds = {conversation.getRefrenceId()};
            List<Flow> flows = flowsRepostiory.findByBotIds(conversation.getRefrenceId());

            //TODO: add default reply
            if (flows.isEmpty()) {
                Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "no flow with botId: " + conversation.getRefrenceId());
                sendDefaultMessage(conversation, requestBody.getCallbackUrl());
                return conversation;
            }
            Flow flow = flows.get(0);
            Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "flow found with id: " + flow.getId());

            if (null == flow) {
                Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "no flow with botId: " + conversation.getRefrenceId());
                sendDefaultMessage(conversation, requestBody.getCallbackUrl());
                return conversation;
            }

            //TODO: add default reply
            if (null == flow.getTopVertexId()) {

                Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "flow does not have topVertexId");
                sendDefaultMessage(conversation, requestBody.getCallbackUrl());
                return conversation;
            }

            conversation.setFlowId(flow.getId());
            Optional<Vertex> optVertex = verticesRepostiory.findById(flow.getTopVertexId());

            //TODO: add default reply
            if (!optVertex.isPresent()) {
                Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "top vertex not found with Id: " + flow.getTopVertexId());
                sendDefaultMessage(conversation, requestBody.getCallbackUrl());
                return conversation;
            }
            Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "top vertex found with id: " + flow.getTopVertexId());

            vertex = optVertex.get();

            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "flow topVertexId: " + flow.getTopVertexId());

            dispatch = new Dispatch(vertex, conversation.getData(), logprefix, conversation.getRefrenceId());
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "assigned currentVertexId: " + vertex.getId());

            conversation.setIsGuest(requestBody.getIsGuest());
        }
        
         */
        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "dispatch.getType(): " + dispatch.getType());

        if (null != dispatch.getVariables()) {
            for (Map.Entry<String, String> mapElement : dispatch.getVariables().entrySet()) {
                String key = mapElement.getKey();
                String value = mapElement.getValue();
                conversation.setVariableValue(key, value);
                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "saved " + key + ": " + value);
            }
        }
        if (VertexType.MENU_MESSAGE == dispatch.getType()
                || VertexType.TEXT_MESSAGE == dispatch.getType()
                || VertexType.HANDOVER == dispatch.getType()
                || VertexType.IMMEDIATE_TEXT_MESSAGE == dispatch.getType()) {
            List<String> recipients = new ArrayList<>();
            recipients.add(conversation.getSenderId());
            PushMessage pushMessage = dispatch.generatePushMessage(conversation.getData(), logprefix);
            pushMessage.setRecipientIds(recipients);
            pushMessage.setReferenceId(conversation.getRefrenceId());

            try {
                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "pushMessage " + pushMessage);

                String url = "";
                if (VertexType.MENU_MESSAGE == dispatch.getType()) {
                    url = requestBody.getCallbackUrl() + "callback/menumessage/push/";
                    messageSender.sendMessage(pushMessage, url, conversation.getSenderId(), conversation.getIsGuest());
                    conversation.shiftVertex(dispatch.getStepId());
                    conversationsRepostiory.save(conversation);
                }

                if (VertexType.TEXT_MESSAGE == dispatch.getType()
                        || VertexType.IMMEDIATE_TEXT_MESSAGE == dispatch.getType()
                        || VertexType.HANDOVER == dispatch.getType()) {
                    url = requestBody.getCallbackUrl() + "callback/textmessage/push/";

                    Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "conversation: " + conversation);
                    Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "conversation.getSenderId(): " + conversation.getSenderId());
                    Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "conversation.getIsGuest(): " + conversation.getIsGuest());

                    messageSender.sendMessage(pushMessage, url, conversation.getSenderId(), conversation.getIsGuest());
                    conversation.shiftVertex(dispatch.getStepId());
                    conversationsRepostiory.save(conversation);
                }

                if (VertexType.HANDOVER == dispatch.getType()) {
                    url = requestBody.getCallbackUrl() + "callback/conversation/pass/";
                    messageSender.sendMessage(pushMessage, url, conversation.getSenderId(), conversation.getIsGuest());
                    conversation.shiftVertex(dispatch.getStepId());
                    conversationsRepostiory.save(conversation);

                }

            } catch (Exception e) {
                Logger.application.error("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "Error sending message", e);

            }

        }

        if (VertexType.ACTION == dispatch.getType()
                || VertexType.CONDITION == dispatch.getType()
                || VertexType.IMMEDIATE_TEXT_MESSAGE == dispatch.getType()) {

            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "recursing through " + dispatch.getType() + " type");

            Optional<Vertex> optNextVertex = verticesRepostiory.findByFlowIdAndMxGraphId(conversation.getFlowId(), dispatch.getStepId());

            if (optNextVertex.isPresent()) {
                Vertex nextVertex = optNextVertex.get();
                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "nextVertexId: " + nextVertex.getId() + " nextVertexId.getIsLastVertex(): " + nextVertex.getIsLastVertex());
                if (nextVertex.getIsLastVertex() != null && nextVertex.getIsLastVertex() == 1) {
                    conversation.getData().setCurrentVertexId(null);
                    conversationsRepostiory.save(conversation);
                    Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "cleared currentVertex from conversation");
                    return conversation;
                }
            }

            conversation.shiftVertex(dispatch.getStepId());
            conversationsRepostiory.save(conversation);
            conversation = processConversastion(conversation, requestBody);
        }

        return conversation;
    }

    private Optional<Vertex> getCurrentVertex(Conversation conversation) throws NotFoundException {

        String logprefix = conversation.getSenderId();
        String vertexId = null;

        if (null != conversation.getData() && null != conversation.getData().getCurrentVertexId()) {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "current vertex found");
            vertexId = conversation.getData().getCurrentVertexId();
        }
        else {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "latest vertex not found");

            List<Flow> flows = flowsRepostiory.findByBotIds(conversation.getRefrenceId());
            Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "bot: " +conversation.getRefrenceId());

            Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "flows for bot: " + flows.size());

            if (flows != null && flows.isEmpty()) {
                throw new NotFoundException();
            }

            Flow flow = flows.get(0);
            Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "flow found with id: " + flow.getId());

            vertexId = flow.getTopVertexId();
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "topVertexId: " + vertexId);
            conversationsRepostiory.save(conversation);
        }

        Optional<Vertex> vertexOpt = verticesRepostiory.findByFlowIdAndMxGraphId(conversation.getFlowId(), vertexId);

        if (!vertexOpt.isPresent()) {
            Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertex not found with id: " + vertexId);
//            sendDefaultMessage(conversation,);
//            conversationsRepostiory.delete(conversation);
            throw new NotFoundException();
        } else {
            return vertexOpt;
        }

    }

    private void sendDefaultMessage(Conversation conversation, String callBackUrl) throws Exception {
        String logprefix = conversation.getSenderId();
        List<String> recipients = new ArrayList<>();
        recipients.add(conversation.getSenderId());
        PushMessage pushMessage = new PushMessage();
        pushMessage.setTitle("Default");
        pushMessage.setSubTitle(defaultMessage);
        pushMessage.setMessage(defaultMessage);
        pushMessage.setRecipientIds(recipients);
        pushMessage.setReferenceId(conversation.getRefrenceId());
        String url = callBackUrl + "callback/textmessage/push/";
        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, " essembled  default pushMessage: " + pushMessage);
        messageSender.sendMessage(pushMessage, url, conversation.getSenderId(), conversation.getIsGuest());
        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, " sent  default pushMessage: " + pushMessage);

    }

}
