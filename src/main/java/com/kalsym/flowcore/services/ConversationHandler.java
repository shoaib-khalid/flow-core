package com.kalsym.flowcore.services;

import com.kalsym.flowcore.daos.models.*;
import com.kalsym.flowcore.daos.models.conversationsubmodels.*;
import com.kalsym.flowcore.daos.repositories.ConversationsRepostiory;
import com.kalsym.flowcore.daos.repositories.FlowsRepostiory;
import com.kalsym.flowcore.daos.repositories.VerticesRepostiory;
import com.kalsym.flowcore.models.RequestPayload;
import com.kalsym.flowcore.models.enums.VertexType;
import com.kalsym.flowcore.models.pushmessages.PushMessage;
import com.kalsym.flowcore.utils.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
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
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();

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
            Logger.info(logprefix, logLocation, "conversation found", "");
            return conversationList.get(0);
        }
        Logger.info(logprefix, logLocation, "conversation  not found", "");

        Conversation newConversation = new Conversation();

        newConversation.setSenderId(senderId);
        newConversation.setRefrenceId(refrenceId);
        newConversation.setFlowId(refrenceId);
        Logger.info(logprefix, logLocation, "created conversation", "");

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
            Logger.info(logprefix, logLocation, "current vertex found", "");
            vertexId = conversation.getData().getCurrentVertexId();
        } else {
            Logger.info(logprefix, logLocation, "latest vertex not found", "");
            Optional<Flow> flowOpt = flowsRepostiory.findById(conversation.getFlowId());
            vertexId = flowOpt.get().getTopVertexId();
            Logger.info(logprefix, logLocation, "flow found with id: " + conversation.getFlowId(), "");
            conversation.shiftVertex(vertexId);
            conversationsRepostiory.save(conversation);
            Logger.info(logprefix, logLocation, "updated currentVertexId: " + vertexId, "");
        }

        Optional<Vertex> vertexOpt = verticesRepostiory.findById(vertexId);

        if (!vertexOpt.isPresent()) {
            Logger.info(logprefix, logLocation, "vertex not found with id: " + vertexId, "");
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
     */
    public Conversation processConversastion(Conversation conversation, RequestPayload requestBody) {
        String inputData = requestBody.getData();
        String logprefix = conversation.getSenderId();
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
        Vertex vertex = null;
        Vertex nextVertex = null;
        if (null != conversation.getData() && null != conversation.getData().getCurrentVertexId()) {
            Logger.info(logprefix, logLocation, "currentVertexId: " + conversation.getData().getCurrentVertexId(), "");

            vertex = verticesRepostiory.findById(conversation.getData().getCurrentVertexId()).get();
            nextVertex = verticesHandler.processVertex(conversation, vertex, inputData);
        } else {
            //if conversation does not have latestVertexId consider it a new 
            //conversation and attach flow's topVertexId to conversation's 
            //latestVertexId
            Logger.info(logprefix, logLocation, "currentVertexId no present", "");

            Flow flow = flowsRepostiory.findById(conversation.getRefrenceId()).get();
            nextVertex = vertex = verticesRepostiory.findById(flow.getTopVertexId()).get();
            Logger.info(logprefix, logLocation, "assigned currentVertexId: " + vertex.getId(), "");

        }

        if (VertexType.MENU_MESSAGE == nextVertex.getInfo().getType()
                || VertexType.TEXT_MESSAGE == nextVertex.getInfo().getType()
                || VertexType.HANDOVER == nextVertex.getInfo().getType()
                || VertexType.IMMEDIATE_TEXT_MESSAGE == nextVertex.getInfo().getType()
                || VertexType.IMMEDIATE_MENU_MESSAGE == nextVertex.getInfo().getType()) {
            List<String> recipients = new ArrayList<>();
            recipients.add(conversation.getSenderId());
            PushMessage pushMessage = nextVertex.getPushMessage(conversation.getData(), recipients, logprefix);

            String url = "";
            if (VertexType.MENU_MESSAGE == nextVertex.getInfo().getType()
                    || VertexType.IMMEDIATE_MENU_MESSAGE == nextVertex.getInfo().getType()) {
                url = requestBody.getCallbackUrl() + "callback/pushMenuMessage";
            }

            if (VertexType.TEXT_MESSAGE == nextVertex.getInfo().getType() 
                    || VertexType.IMMEDIATE_TEXT_MESSAGE == nextVertex.getInfo().getType()) {
                url = requestBody.getCallbackUrl() + "callback/pushSimpleMessage";
            }

            if (VertexType.HANDOVER == nextVertex.getInfo().getType()) {
                url = requestBody.getCallbackUrl() + "callback/passConversationToCustomerService";

            }
            try {
                messageSender.sendMessage(pushMessage, url, conversation.getSenderId(), requestBody.getIsGuest());
                conversation.shiftVertex(nextVertex.getId());
                conversationsRepostiory.save(conversation);
            } catch (Exception e) {
                Logger.error(logprefix, logLocation, "Error sending message", "", e);
            }

        }

        if (VertexType.ACTION == nextVertex.getInfo().getType()
                || VertexType.CONDITION == nextVertex.getInfo().getType()
                || VertexType.IMMEDIATE_TEXT_MESSAGE == nextVertex.getInfo().getType()
                || VertexType.IMMEDIATE_MENU_MESSAGE == nextVertex.getInfo().getType()) {
            Logger.info(logprefix, logLocation, "recursing through " + nextVertex.getInfo().getType() + " type", "");
            conversation.shiftVertex(nextVertex.getId());
            conversationsRepostiory.save(conversation);
            conversation = processConversastion(conversation, requestBody);
        }

        return conversation;
    }

}
