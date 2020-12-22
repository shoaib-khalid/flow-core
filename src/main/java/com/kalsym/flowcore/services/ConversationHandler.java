package com.kalsym.flowcore.services;

import com.kalsym.flowcore.daos.models.*;
import com.kalsym.flowcore.daos.models.conversationsubmodels.*;
import com.kalsym.flowcore.daos.repositories.ConversationsRepostiory;
import com.kalsym.flowcore.daos.repositories.FlowsRepostiory;
import com.kalsym.flowcore.daos.repositories.VerticesRepostiory;
import com.kalsym.flowcore.utils.Logger;
import java.util.List;
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

        String logprefix = "";
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
        String logprefix = "";
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
     * Returns next vertex based on conversation flow and inputData. Returns
     * flow top Vertex in case conversation does not have latestVertexId.
     *
     * @param conversation
     * @param inputData
     * @return Next vertex.
     */
    public Vertex getNextVertex(Conversation conversation, String inputData) {

        String logprefix = "";
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
        Vertex vertex = null;
        Vertex nextVertex = null;
        if (null != conversation.getData() && null != conversation.getData().getCurrentVertexId()) {
            vertex = verticesRepostiory.findById(conversation.getData().getCurrentVertexId()).get();
            nextVertex = verticesHandler.getNextVertex(conversation, vertex, inputData);
        } else {
            //if conversation does not have latestVertexId consider it a new 
            //conversation and attach flow's topVertexId to conversation's 
            //latestVertexId
            Flow flow = flowsRepostiory.findById(conversation.getRefrenceId()).get();
            nextVertex = vertex = verticesRepostiory.findById(flow.getTopVertexId()).get();
        }
        Logger.info(logprefix, logLocation, "vertex: " + vertex.getId(), "");
        Logger.info(logprefix, logLocation, "nextVertexId: " + nextVertex.getId(), "");


        return nextVertex;
    }

}
