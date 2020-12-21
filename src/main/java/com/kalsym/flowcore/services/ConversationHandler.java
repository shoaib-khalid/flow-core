package com.kalsym.flowcore.services;

import com.kalsym.flowcore.daos.models.*;
import com.kalsym.flowcore.daos.repositories.ConversationsRepostiory;
import com.kalsym.flowcore.daos.repositories.FlowsRepostiory;
import com.kalsym.flowcore.daos.repositories.VerticesRepostiory;
import com.kalsym.flowcore.models.enums.VertexType;
import com.kalsym.flowcore.utils.LogUtil;
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
            LogUtil.info(logprefix, logLocation, "conversation found", "");
            return conversationList.get(0);
        }
        LogUtil.info(logprefix, logLocation, "conversation  not found", "");

        Conversation newConversation = new Conversation();

        newConversation.setSenderId(senderId);
        newConversation.setRefrenceId(refrenceId);
        newConversation.setFlowId(refrenceId);
        LogUtil.info(logprefix, logLocation, "created conversation", "");

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

        if (null != conversation.getLatestVertexId()) {
            LogUtil.info(logprefix, logLocation, "latest vertex found", "");
            vertexId = conversation.getLatestVertexId();
        } else {
            LogUtil.info(logprefix, logLocation, "latest vertex not found", "");

            Optional<Flow> flowOpt = flowsRepostiory.findById(conversation.getFlowId());
            if (!flowOpt.isPresent()) {
                LogUtil.info(logprefix, logLocation, "flow not found with id: " + conversation.getFlowId(), "");
                throw new NotFoundException();
            } else {
                vertexId = flowOpt.get().getTopVertexId();
                LogUtil.info(logprefix, logLocation, "flow found with id: " + conversation.getFlowId(), "");
                conversation.setLatestVertexId(vertexId);
                conversationsRepostiory.save(conversation);
                LogUtil.info(logprefix, logLocation, "updated latestVertexId: " + vertexId, "");

            }
        }

        Optional<Vertex> vertexOpt = verticesRepostiory.findById(vertexId);

        if (!vertexOpt.isPresent()) {
            LogUtil.info(logprefix, logLocation, "vertex not found with id: " + vertexId, "");
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
        if (null != conversation.getLatestVertexId()) {
            vertex = verticesRepostiory.findById(conversation.getLatestVertexId()).get();
            nextVertex = verticesHandler.getNextVertex(conversation, vertex, inputData);
        } else {
            //if conversation does not have latestVertexId consider it a new 
            //conversation and attach flow's topVertexId to conversation's 
            //latestVertexId
            Flow flow = flowsRepostiory.findById(conversation.getRefrenceId()).get();
            nextVertex = vertex = verticesRepostiory.findById(flow.getTopVertexId()).get();
        }
        LogUtil.info(logprefix, logLocation, "vertex: " + vertex.getId(), "");
        LogUtil.info(logprefix, logLocation, "nextVertexId: " + nextVertex.getId(), "");

//        if (vertex.getId().equals(nextVertex.getId())) {
//
//            if (VertexType.TEXT_MESSAGE == vertex.getInfo().getType()) {
//                int currentVertexSendCount = conversation.getData().getCurrentVertexSendCount();
//                LogUtil.info(logprefix, logLocation, "vertex already sent " + currentVertexSendCount + " times", "");
//                if (currentVertexSendCount >= vertex.getValidation().getRetry().getCount()) {
//                    LogUtil.info(logprefix, logLocation, "vertex sen limit reached", "");
//                    nextVertex = verticesRepostiory.findById(vertex.getValidation().getRetry().getFailureStep().getTargetId()).get();
//                    LogUtil.info(logprefix, logLocation, "nextVertexId: " + nextVertex.getId(), "");
//
//                } else {
//                    conversation.getData().setCurrentVertexSendCount(conversation.getData().getCurrentVertexSendCount() + 1);
//
//                }
//            }
//
//        }
        return nextVertex;
    }

    /**
     * Returns next vertex based on conversation flow and inputData.Returns flow
     * top Vertex in case conversation does not have latestVertexId.
     *
     * @param conversation
     * @param nextVertex
     * @return Next vertex.
     */
    public Conversation shiftVertex(Conversation conversation, Vertex nextVertex) {

        String logprefix = "";
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();

        if (null != conversation.getLatestVertexId()) {
            conversation.setFlowId(nextVertex.getFlowId());
            conversation.setLatestVertexId(nextVertex.getId());
        } else {
            if (!nextVertex.getFlowId().equals(conversation.getFlowId())) {
                conversation.setFlowId(nextVertex.getFlowId());
                LogUtil.info(logprefix, logLocation, "flow id changed to: " + nextVertex.getFlowId(), "");

            }
        }

        return conversationsRepostiory.save(conversation);

    }

}
