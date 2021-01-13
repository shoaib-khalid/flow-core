package com.kalsym.flowcore.services;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.kalsym.flowcore.models.pushmessages.*;
import com.kalsym.flowcore.daos.models.*;
import com.kalsym.flowcore.daos.models.vertexsubmodels.*;
import com.kalsym.flowcore.daos.repositories.ConversationsRepostiory;
import com.kalsym.flowcore.daos.repositories.FlowsRepostiory;
import com.kalsym.flowcore.daos.repositories.VerticesRepostiory;
import com.kalsym.flowcore.models.enums.*;
import com.kalsym.flowcore.utils.Logger;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    public Vertex processVertex(Conversation conversation, Vertex vertex, String inputData) {

        String logprefix = conversation.getSenderId();
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
        Logger.info(logprefix, logLocation, "vertexType: " + vertex.getInfo().getType(), "");

        Vertex nextVertex = null;

        Step step = null;
        if (VertexType.ACTION == vertex.getInfo().getType()) {
            step = getStepByAction(conversation, vertex);
        }

        if (VertexType.MENU_MESSAGE == vertex.getInfo().getType()
                || VertexType.IMMEDIATE_MENU_MESSAGE == vertex.getInfo().getType()) {
            step = getStepByMenu(vertex, conversation, inputData);
        }

        if (VertexType.IMMEDIATE_TEXT_MESSAGE == vertex.getInfo().getType()) {
            step = getStepByText(conversation, vertex, inputData);
        }

        if (VertexType.TEXT_MESSAGE == vertex.getInfo().getType()) {
            step = getStepByText(conversation, vertex, inputData);
        }

        if (VertexType.CONDITION == vertex.getInfo().getType()) {
            step = vertex.matchConditions(conversation.getData());
        }

        Logger.info(logprefix, logLocation, "vertexId: " + vertex.getId(), "");
        Logger.info(logprefix, logLocation, "nextVertexId: " + step.getTargetId(), "");
        Optional<Vertex> vertexOpt = verticesRepostiory.findById(step.getTargetId());
        nextVertex = vertexOpt.get();

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

        String logprefix = conversation.getSenderId();
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
        Validation validation = vertex.getValidation();

        if (ValidationInputType.TEXT == validation.getInputType()) {
            Pattern p = Pattern.compile(validation.getRegex());
            Matcher m = p.matcher(text);

            if (m.matches()) {
                Logger.info(logprefix, logLocation, "validation success for data: " + text, "");

                conversation.setVariableValue(vertex.getDataVariable(), text);
                conversationsRepostiory.save(conversation);
                Logger.info(logprefix, logLocation, "saved " + vertex.getDataVariable() + ": " + text, "");

                return vertex.getStep();
            } else {
                Logger.info(logprefix, logLocation, "validation failed for data: " + text, "");

            }
        }

        Step step = new Step();
        step.setTargetId(vertex.getId());
        step.setTargetType(VertexTargetType.VERTEX);

        return step;
    }

    /**
     * Performs provided actions for a conversation and returns next Step
     * accordingly.
     *
     * @param conversation
     * @param vertex
     * @return Next Step
     */
    private Step getStepByAction(Conversation conversation, Vertex vertex) {

        String logprefix = conversation.getSenderId();
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();

        Step step = null;

        for (Action action : vertex.getActions()) {
            if (VertexActionType.EXTERNAL_REQUEST == action.getType()) {
                step = processExternalRequest(conversation, action.getExternalRequest());
            }

            if (step != null) {
                Logger.info(logprefix, logLocation, "error processing action: " + action.getType(), "");
                Logger.info(logprefix, logLocation, "errorStepTargetId: " + step.getTargetId(), "");
                return step;
            }

            Logger.info(logprefix, logLocation, "successfully processed action: " + action.getType(), "");

        }

        if (step == null) {
            step = vertex.getStep();
            Logger.info(logprefix, logLocation, "no error assigning next step: " + step.getTargetId(), "");
        }

        return step;
    }

    /**
     * Sends external request to provided URL and saves dataVariables after
     * successful response.
     *
     * @param conversation
     * @param externalRequest
     * @return Next Step
     */
    private Step processExternalRequest(Conversation conversation, ExternalRequest externalRequest) {
        String logprefix = conversation.getSenderId();
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();

        Logger.info(logprefix, logLocation, "url: " + externalRequest.getUrl(), "");
        Logger.info(logprefix, logLocation, "httpMethod: " + externalRequest.getHttpMethod(), "");

        try {
            HashMap<String, String> erHeaders = externalRequest.getHeaders();

            HttpHeaders headers = new HttpHeaders();

            if (null != erHeaders) {
                erHeaders.entrySet().forEach(mapElement -> {
                    headers.add(mapElement.getKey(), mapElement.getValue());
                });
            }

            ExternalRequestBody erBody = externalRequest.getBody();
            HttpEntity<String> requestEntity = null;
            if (DataFomat.JSON == erBody.getFormat()) {
                String payload = erBody.getPayload();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (null != conversation.getData().getVariables()) {
                    HashMap<String, String> datavariables = conversation.getData().getVariables();
                    for (Map.Entry<String, String> mapElement : datavariables.entrySet()) {
                        String key = mapElement.getKey();
                        String value = mapElement.getValue();
                        payload = payload.replace("$%" + key + "$%", value);
                    }
                }

                requestEntity = new HttpEntity<>(payload, headers);
            }

            URI uri = null;

            ResponseEntity<String> responseEntity = restTemplate.exchange(externalRequest.getUrl(), externalRequest.getHttpMethod(), requestEntity, String.class);

            Logger.info(logprefix, logLocation, "responseStatus: " + responseEntity.getStatusCode(), "");

            if (HttpStatus.ACCEPTED == responseEntity.getStatusCode()
                    || HttpStatus.CREATED == responseEntity.getStatusCode()
                    || HttpStatus.OK == responseEntity.getStatusCode()) {

                String responseBody = responseEntity.getBody();
                DocumentContext jsonContext = JsonPath.parse(responseBody);

                Logger.info(logprefix, logLocation, "responseBody: " + responseBody, "");

                List<ExternalRequestResponseMapping> responseMappings = externalRequest.getResponse().getMapping();

                for (ExternalRequestResponseMapping errm : responseMappings) {

                    try {
                        String value = jsonContext.read(errm.getPath()) + "";
                        conversation.setVariableValue(errm.getDataVariable(), value);
                        Logger.info(logprefix, logLocation, "saved " + errm.getDataVariable() + ": " + value, "");
                    } catch (Exception e) {
                        if (errm.isOptional()) {
                            Logger.warn(logprefix, logLocation, "could not read optional " + errm.getDataVariable(), "");

                        } else {
                            Logger.error(logprefix, logLocation, "Cannot find non-optional variable " + errm.getDataVariable(), "", e);
                            return externalRequest.getErrorStep();
                        }
                    }

                }

                conversationsRepostiory.save(conversation);

            } else {
                return externalRequest.getErrorStep();
            }
        } catch (Exception e) {
            Logger.error(logprefix, logLocation, "Error processing external request", "", e);
            return externalRequest.getErrorStep();
        }

        return null;
    }

    /**
     * Returns step after passing input through options. Also save the value of
     * the chosen option.
     *
     * @param vertex
     * @param conversation
     * @param value
     * @return Next Step
     */
    private Step getStepByMenu(Vertex vertex, Conversation conversation, String value) {
        String logprefix = conversation.getSenderId();
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
        Option option = vertex.matchOptions(value, conversation.getData().getVariables());

        if (null != option) {
            conversation.setVariableValue(vertex.getDataVariable(), value);
            Logger.info(logprefix, logLocation, "saved " + vertex.getDataVariable() + ": " + value, "");
            conversationsRepostiory.save(conversation);
            return option.getStep();
        }

        return vertex.getStep();
    }

}
