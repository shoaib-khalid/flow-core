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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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
        //searchConvo.setRefrenceId(refrenceId);

        Example convoExample = Example.of(searchConvo,
                ExampleMatcher.matching()
                        .withIgnoreNullValues()
                        .withIgnorePaths("id")
                        .withStringMatcher(StringMatcher.CONTAINING)
                        .withIgnoreCase());

        List<Conversation> conversationList = conversationsRepostiory.findAll(convoExample, Sort.by("lastModifiedDate").descending());

        if (conversationList.size() > 0) {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "conversation found");
            return conversationList.get(0);
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
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
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

        Optional<Vertex> vertexOpt = verticesRepostiory.findById(vertexId);

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
    public Conversation processConversastion(Conversation conversation, RequestPayload requestBody) throws InterruptedException {
        String inputData = requestBody.getData();
        String logprefix = conversation.getSenderId();
        Vertex vertex = null;
        //Vertex nextVertex = null;
        Dispatch dispatch;
        if (null != conversation.getData() && null != conversation.getData().getCurrentVertexId()) {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "currentVertexId: " + conversation.getData().getCurrentVertexId());

            vertex = verticesRepostiory.findById(conversation.getData().getCurrentVertexId()).get();
            dispatch = verticesHandler.processVertex(conversation, vertex, inputData);

        } else {
            //if conversation does not have latestVertexId consider it a new 
            //conversation and attach flow's topVertexId to conversation's 
            //latestVertexId
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "currentVertexId no present");


            String[] botIds = {conversation.getRefrenceId()};
            List<Flow> flows = flowsRepostiory.findByBotIds(conversation.getRefrenceId());

            if (flows.isEmpty()) {
                Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "no flow with botId: " + conversation.getRefrenceId());
                return conversation;
            }
            Flow flow = flows.get(0);

            if (null == flow) {
                Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "no flow with botId: " + conversation.getRefrenceId());
                return conversation;
            }

            if (null == flow.getTopVertexId()) {
                Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "flow does not have topVertexId");
                return conversation;
            }

            conversation.setFlowId(flow.getId());
            Optional<Vertex> optVertex = verticesRepostiory.findById(flow.getTopVertexId());

            if (!optVertex.isPresent()) {
                Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertex not found with Id: " + flow.getTopVertexId());
                return conversation;
            }
            vertex = optVertex.get();

            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "flow topVertexId: " + flow.getTopVertexId());

            dispatch = new Dispatch(vertex, conversation.getData(), logprefix);
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "assigned currentVertexId: " + vertex.getId());

            conversation.setIsGuest(requestBody.getIsGuest());
        }

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

            try {
                String url = "";
                if (VertexType.MENU_MESSAGE == dispatch.getType()) {
                    url = requestBody.getCallbackUrl() + "callback/menumessage/push/";
                    messageSender.sendMessage(pushMessage, url, conversation.getSenderId(), conversation.getIsGuest(Boolean.TRUE));
                    conversation.shiftVertex(dispatch.getStepId());
                    conversationsRepostiory.save(conversation);
                }

                if (VertexType.TEXT_MESSAGE == dispatch.getType()
                        || VertexType.IMMEDIATE_TEXT_MESSAGE == dispatch.getType()
                        || VertexType.HANDOVER == dispatch.getType()) {
                    url = requestBody.getCallbackUrl() + "callback/textmessage/push/";
                    messageSender.sendMessage(pushMessage, url, conversation.getSenderId(), conversation.getIsGuest(Boolean.TRUE));
                    conversation.shiftVertex(dispatch.getStepId());
                    conversationsRepostiory.save(conversation);
                }

                if (VertexType.HANDOVER == dispatch.getType()) {
                    url = requestBody.getCallbackUrl() + "callback/conversation/pass/";
                    messageSender.sendMessage(pushMessage, url, conversation.getSenderId(), conversation.getIsGuest(Boolean.TRUE));
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
            conversation.shiftVertex(dispatch.getStepId());
            conversationsRepostiory.save(conversation);
            conversation = processConversastion(conversation, requestBody);
        }

        return conversation;
    }

}
