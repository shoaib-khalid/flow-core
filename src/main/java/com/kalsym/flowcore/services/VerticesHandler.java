package com.kalsym.flowcore.services;

import com.kalsym.flowcore.models.pushmessages.*;
import com.kalsym.flowcore.daos.models.*;
import com.kalsym.flowcore.daos.models.vertexsubmodels.*;
import com.kalsym.flowcore.daos.models.conversationsubmodels.*;
import com.kalsym.flowcore.daos.repositories.ConversationsRepostiory;
import com.kalsym.flowcore.daos.repositories.FlowsRepostiory;
import com.kalsym.flowcore.daos.repositories.VerticesRepostiory;
import com.kalsym.flowcore.models.enums.ValidationInputType;
import com.kalsym.flowcore.models.enums.VertexType;
import com.kalsym.flowcore.utils.LogUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Sarosh
 */
@Service
public class VerticesHandler {

    @Autowired
    private ConversationsRepostiory conversationsRepostiory;

    @Autowired
    private FlowsRepostiory flowsRepostiory;

    @Autowired
    private VerticesRepostiory verticesRepostiory;

    /**
     * Returns next Vertex after processing inputData through the provided
     * vertex.
     *
     * @param conversation
     * @param vertex
     * @param inputData Data to be processed. Can be
     * @return Next vertex.
     */
    public Vertex getNextVertex(Conversation conversation, Vertex vertex, String inputData) {

        String logprefix = "";
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
        LogUtil.info(logprefix, logLocation, "vertexType: " + vertex.getInfo().getType(), "");

        Vertex nextVertex = null;

        if (VertexType.ACTION == vertex.getInfo().getType()) {
        }

        if (VertexType.MENU_MESSAGE == vertex.getInfo().getType()) {
            nextVertex = getNextVertexByMenu(inputData);
        }

        if (VertexType.TEXT_MESSAGE == vertex.getInfo().getType()) {
            Step step = getNextVertexByText(conversation, vertex, inputData);
            LogUtil.info(logprefix, logLocation, "nextVertex Id: " + step.getTargetId(), "");

            Optional<Vertex> vertexOpt = verticesRepostiory.findById(step.getTargetId());
            nextVertex = vertexOpt.get();
        }

        if (VertexType.CONDITION == vertex.getInfo().getType()) {
            Step step = vertex.matchConditions(conversation.getData());
            LogUtil.info(logprefix, logLocation, "nextVertex Id: " + step.getTargetId(), "");

            Optional<Vertex> vertexOpt = verticesRepostiory.findById(step.getTargetId());
            nextVertex = vertexOpt.get();
        }

        if (VertexType.ACTION == nextVertex.getInfo().getType()
                || VertexType.CONDITION == nextVertex.getInfo().getType()) {
            nextVertex = getNextVertex(conversation, nextVertex, inputData);
        }

        return nextVertex;
    }

    /**
     * Returns the next vertex considering that inputData is id of next vertex;
     *
     * @param targetId Id of next vertex
     * @return Next Vertex.
     */
    private Vertex getNextVertexByMenu(String targetId) {
        Optional<Vertex> vertexOpt = verticesRepostiory.findById(targetId);
        return vertexOpt.get();
    }

    /**
     * Returns the next vertex after processing text input.
     *
     * @param conversation
     * @param vertex
     * @param text
     * @return Next vertex
     */
    private Step getNextVertexByText(Conversation conversation, Vertex vertex, String text) {
        Validation validation = vertex.getValidation();

        if (ValidationInputType.TEXT == validation.getInputType()) {
            Pattern p = Pattern.compile(validation.getRegex());
            Matcher m = p.matcher(text);

            if (m.matches()) {

                Data data = conversation.getData();

                if (null == data) {
                    data = new Data();
                }

                HashMap<String, String> variables = data.getVariables();
                if (null == variables) {
                    variables = new HashMap<>();
                }

                variables.put(vertex.getCustomVariableName(), text);
                data.setVariables(variables);
                conversation.setData(data);
                conversationsRepostiory.save(conversation);
                return vertex.getStep();
            }
        }

        Step step = new Step();
        step.setTargetId(vertex.getId());
        step.setActionType("vertex");

        return step;
    }

    private Vertex getNextVertexByCondition(Conversation conversation, Vertex vertex, String text) {
        return null;
    }

    /**
     * Compiles and returns the push message based on Vertex/
     *
     * @param vertex
     * @param recipients
     * @param refId
     *
     * @return PushMessage
     */
    public PushMessage getPushMessage(Vertex vertex, List<String> recipients, String refId) {
        String logprefix = "";
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
        LogUtil.info(logprefix, logLocation, "vertexType: " + vertex.getInfo().getType(), "");

        PushMessage pushMessage = null;

        if (VertexType.ACTION == vertex.getInfo().getType()) {
        }

        if (VertexType.MENU_MESSAGE == vertex.getInfo().getType()) {
            pushMessage = processMenu(vertex);
        }

        if (VertexType.TEXT_MESSAGE == vertex.getInfo().getType()) {
            pushMessage = processTextMessage(vertex);
        }

        if (VertexType.CONDITION == vertex.getInfo().getType()) {

        }

        pushMessage.setRecipientIds(recipients);
        pushMessage.setRefId(refId);

        return pushMessage;
    }

    private PushMessage processMenu(Vertex vertex) {
        String logprefix = "";
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();

        PushMessage pushMessage = new PushMessage();

        pushMessage.setTitle(vertex.getInfo().getTitle());
        pushMessage.setSubTitle(vertex.getInfo().getText());

        List<MenuItem> menuItems = new ArrayList<>();
        for (Option option : vertex.getOptions()) {
            MenuItem menuItem = new MenuItem();
            menuItem.setTitle(option.getText());
            menuItem.setType("postback");
            menuItem.setPayload(option.getStep().getTargetId());
            menuItems.add(menuItem);
        }

        if (null == vertex.getOptions() || vertex.getOptions().isEmpty()) {
            MenuItem menuItem = new MenuItem();
            menuItem.setTitle("Default");
            menuItem.setType("postback");
            menuItem.setPayload(vertex.getStep().getTargetId());
            menuItems.add(menuItem);
        }

        pushMessage.setMenuItems(menuItems);

        return pushMessage;
    }

    private PushMessage processTextMessage(Vertex vertex) {
        String logprefix = "";
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();

        PushMessage pushMessage = new PushMessage();

        pushMessage.setTitle(vertex.getInfo().getTitle());
        pushMessage.setSubTitle(vertex.getInfo().getText());
        pushMessage.setMessage(vertex.getInfo().getText());
        pushMessage.setUrl("");
        pushMessage.setUrlType("");

        return pushMessage;
    }

}
