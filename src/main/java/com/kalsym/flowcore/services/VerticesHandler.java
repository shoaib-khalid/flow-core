package com.kalsym.flowcore.services;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.kalsym.flowcore.models.pushmessages.*;
import com.kalsym.flowcore.daos.models.*;
import com.kalsym.flowcore.daos.models.vertexsubmodels.*;
import com.kalsym.flowcore.daos.models.conversationsubmodels.*;
import com.kalsym.flowcore.daos.repositories.ConversationsRepostiory;
import com.kalsym.flowcore.daos.repositories.FlowsRepostiory;
import com.kalsym.flowcore.daos.repositories.VerticesRepostiory;
import com.kalsym.flowcore.models.enums.*;
import com.kalsym.flowcore.utils.Logger;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    @Autowired
    RestTemplate restTemplate;

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

        String logprefix = conversation.getSenderId();
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
        Logger.info(logprefix, logLocation, "vertexType: " + vertex.getInfo().getType(), "");

        Vertex nextVertex = null;

        Step step = null;
        if (VertexType.ACTION == vertex.getInfo().getType()) {
            step = getStepByAction(conversation, vertex);
        }

        if (VertexType.MENU_MESSAGE == vertex.getInfo().getType()) {
            step = vertex.matchOptions(inputData);
        }

        if (VertexType.TEXT_MESSAGE == vertex.getInfo().getType()) {
            step = getStepByText(conversation, vertex, inputData);
        }

        if (VertexType.CONDITION == vertex.getInfo().getType()) {
            step = vertex.matchConditions(conversation.getData());
        }

        Logger.info(logprefix, logLocation, "nextVertex Id: " + step.getTargetId(), "");
        Optional<Vertex> vertexOpt = verticesRepostiory.findById(step.getTargetId());
        nextVertex = vertexOpt.get();

        if (VertexType.ACTION == nextVertex.getInfo().getType()
                || VertexType.CONDITION == nextVertex.getInfo().getType()) {
            nextVertex = getNextVertex(conversation, nextVertex, inputData);
        }

        return nextVertex;
    }

    /**
     * Returns the step after validating input. If the validation does not pass
     * than returns the same vertex in step. Also saves data in custom variable
     * of vertex if validation succeeds.
     *
     * @param conversation
     * @param vertex
     * @param text
     * @return Step
     */
    private Step getStepByText(Conversation conversation, Vertex vertex, String text) {
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
        step.setTargetType(VertexTargetType.VERTEX);

        return step;
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
        String logprefix = refId;
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
        Logger.info(logprefix, logLocation, "vertexType: " + vertex.getInfo().getType(), "");

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

        PushMessage pushMessage = new PushMessage();

        pushMessage.setTitle(vertex.getInfo().getTitle());
        pushMessage.setSubTitle(vertex.getInfo().getText());
        pushMessage.setMessage(vertex.getInfo().getText());
        pushMessage.setUrl("");
        pushMessage.setUrlType("");

        return pushMessage;
    }

    private Step getStepByAction(Conversation conversation, Vertex vertex) {

        Step step = null;

        for (Action action : vertex.getActions()) {
            if (VertexActionType.EXTERNAL_REQUEST == action.getType()) {
                step = processExternalRequest(conversation, action.getExternalRequest());
            }
        }

        if (step == null) {
            step = vertex.getStep();
        }

        return step;
    }

    private Step processExternalRequest(Conversation conversation, ExternalRequest externalRequest) {
        String logprefix = conversation.getSenderId();
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();

        Logger.info(logprefix, logLocation, "url: " + externalRequest.getUrl(), "");

        try {
            HashMap<String, String> erHeaders = externalRequest.getHeaders();

            HttpHeaders headers = new HttpHeaders();

            if (null != erHeaders) {
                erHeaders.entrySet().forEach(mapElement -> {
                    headers.add(mapElement.getKey(), mapElement.getValue());
                });
            }

            HashMap<String, String> erBody = externalRequest.getPayload();

            HashMap<String, String> requestBody = new HashMap<>();

            erBody.entrySet().forEach(mapElement -> {

                String elementValue = mapElement.getValue();

                if (elementValue.startsWith("$%") && elementValue.endsWith("$%")) {
                    elementValue = elementValue.replaceAll("&%", "");
                    requestBody.put(mapElement.getKey(), conversation.getVariableValue(elementValue));
                } else {
                    requestBody.put(mapElement.getKey(), elementValue);

                }
            });
            URI uri = null;
            HttpEntity<HashMap> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange(externalRequest.getUrl(), externalRequest.getHttpMethod(), requestEntity, String.class);

            if (HttpStatus.ACCEPTED == responseEntity.getStatusCode()
                    || HttpStatus.CREATED == responseEntity.getStatusCode()) {

                DocumentContext jsonContext = JsonPath.parse(responseEntity.getBody());

                HashMap<String, String> responseMapping = externalRequest.getResponseMapping();
                responseMapping.entrySet().forEach(mapElement -> {
                    String value = jsonContext.read(mapElement.getValue());
                    conversation.setVariableValue(mapElement.getKey(), value);
                });

                conversationsRepostiory.save(conversation);

            } else {
                return externalRequest.getErrorStep();
            }
        } catch (Exception e) {
            Logger.error(logprefix, logLocation, "Error processing request params", "", e);
            return externalRequest.getErrorStep();
        }

        return null;
    }

}
