package com.kalsym.flowcore.services;

import com.kalsym.flowcore.VersionHolder;
import com.kalsym.flowcore.models.*;
import com.kalsym.flowcore.daos.models.*;
import com.kalsym.flowcore.daos.models.vertexsubmodels.*;
import com.kalsym.flowcore.daos.repositories.VerticesRepostiory;
import com.kalsym.flowcore.models.enums.*;
import com.kalsym.flowcore.utils.Logger;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Sarosh
 */
@Service
public class VerticesHandler {

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
    public Dispatch processVertex(Conversation conversation, Vertex vertex, String inputData) {

        String logprefix = conversation.getSenderId();
        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertexType: " + vertex.getInfo().getType());

        Vertex nextVertex = null;

        Dispatch dispatch = null;

        if (VertexType.ACTION == vertex.getInfo().getType()) {
            dispatch = getStepByAction(conversation, vertex);
        }

        if (VertexType.MENU_MESSAGE == vertex.getInfo().getType()) {
            dispatch = getStepByMenu(vertex, conversation, inputData);
        }

        if (VertexType.IMMEDIATE_TEXT_MESSAGE == vertex.getInfo().getType()) {
            Step step = vertex.getStep();
            //todo: changing to findByMxGraphId
            Optional<Vertex> optionalVertex = verticesRepostiory.findByFlowIdAndMxGraphId(conversation.getFlowId(), step.getTargetId());
            if(optionalVertex.isPresent()){
                nextVertex = optionalVertex.get();
            }else
                return null;
            dispatch = new Dispatch(nextVertex, conversation.getData(), logprefix, conversation.getRefrenceId());
        }
        if (VertexType.HANDOVER == vertex.getInfo().getType()) {
            dispatch = getStepByHandover(conversation, vertex, inputData);
        }

        if (VertexType.TEXT_MESSAGE == vertex.getInfo().getType()) {
            dispatch = getStepByText(conversation, vertex, inputData);
        }

        if (VertexType.WIZARD == vertex.getInfo().getType()) {
            dispatch = getStepByWizard(conversation, vertex, inputData);
        }

        if (VertexType.CONDITION == vertex.getInfo().getType()) {
            Step step = vertex.matchConditions(conversation.getData());
            Optional<Vertex> optionalVertex = verticesRepostiory.findByFlowIdAndMxGraphId(conversation.getFlowId(), step.getTargetId());
            if(optionalVertex.isPresent()){
                nextVertex = optionalVertex.get();
            }else
            {
                Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertex not found with id: " + step.getTargetId());
                return null;
            }
            dispatch = new Dispatch(nextVertex, conversation.getData(), logprefix, conversation.getRefrenceId());
        }

        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertexId: " + vertex.getId());
        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "nextVertexId: " + dispatch.getStepId());

        return dispatch;
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
    private Dispatch getStepByText(Conversation conversation, Vertex vertex, String text) {

        String logprefix = conversation.getSenderId();

        Dispatch dispatch = null;
        Validation validation = vertex.getValidation();
        try {
            Pattern p = Pattern.compile(validation.getRegex());
            Matcher m = p.matcher(text);

            if (m.matches()) {
                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "validation success for data: " + text);

                Vertex nextVertex;
                Optional<Vertex> optionalVertex = verticesRepostiory.findByFlowIdAndMxGraphId(conversation.getFlowId(), vertex.getStep().getTargetId());
                if(optionalVertex.isPresent()){
                    nextVertex = optionalVertex.get();
                }else
                {
                    Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertex not found with id: " + vertex.getStep().getTargetId());
                    return null;
                }
                dispatch = new Dispatch(nextVertex, conversation.getData(), logprefix, conversation.getRefrenceId());
                dispatch.setVariableValue(vertex.getDataVariable(), text);
                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "variable added " + vertex.getDataVariable() + ": " + text);
                return dispatch;
            } else {
                Logger.application.error("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "validation failed for data: " + text);
                vertex.getInfo().setText(vertex.getValidation().getRetry().getMessage());
                dispatch = new Dispatch(vertex, conversation.getData(), logprefix, conversation.getRefrenceId());
                return dispatch;
            }
        } catch (Exception e) {
            Logger.application.error("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "validation exception " + text, e.getMessage());
            Vertex nextVertex ;
            Optional<Vertex> optionalVertex = verticesRepostiory.findByFlowIdAndMxGraphId(conversation.getFlowId(), vertex.getStep().getTargetId());
            if(optionalVertex.isPresent()){
                nextVertex = optionalVertex.get();
            }else
            {
                Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertex not found with id: " + vertex.getStep().getTargetId());
                return null;
            }
            dispatch = new Dispatch(nextVertex, conversation.getData(), logprefix, conversation.getRefrenceId());
            dispatch.setVariableValue(vertex.getDataVariable(), text);
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "variable added " + vertex.getDataVariable() + ": " + text);
            return dispatch;
        }

    }

    /**
     * Returns the step after processing handover input data.
     *
     * @param conversation
     * @param vertex
     * @param inputData
     * @return Step
     */
    private Dispatch getStepByHandover(Conversation conversation, Vertex vertex, String inputData) {

        String logprefix = conversation.getSenderId();

        String csrName = "";
        String event = "";
        Dispatch dispatch = null;
//        try {
//            JSONObject evetnObj = new JSONObject(inputData);
//            event = evetnObj.getString("event");
//            csrName = evetnObj.getString("agentName");
//        } catch (Exception e) {
//            Logger.application.error("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "Error reading json: ", e);
//        }

        if (HandoverAction.LIVECHATSESSIONTAKEN.toString().equals(event.toUpperCase())) {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "in if : Next Vertex Id: " + vertex.getStep().getTargetId());
            vertex.getInfo().setText(vertex.getHandover().getConnectMessage());
            dispatch = new Dispatch(vertex, conversation.getData(), logprefix, conversation.getRefrenceId());
            dispatch.setVariableValue("csrName", csrName);
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "csrName: " + csrName);

        } else if (HandoverAction.FORWARDED.toString().equals(event.toUpperCase())) {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "in else 1 : Next Vertex Id: " + vertex.getStep().getTargetId());
            vertex.getInfo().setText(vertex.getHandover().getForwardMessage());
            dispatch = new Dispatch(vertex, conversation.getData(), logprefix, conversation.getRefrenceId());
        } else if (HandoverAction.LIVECHATSESSION.toString().equals(event.toUpperCase())) {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "in else if 2 : Next Vertex Id: " + vertex.getStep().getTargetId());
            Vertex nextVertex;
            Optional<Vertex> optionalVertex = verticesRepostiory.findByFlowIdAndMxGraphId(conversation.getFlowId(), vertex.getStep().getTargetId());
            if(optionalVertex.isPresent()){
                nextVertex = optionalVertex.get();
            }else
            {
                Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertex not found with id: " + vertex.getStep().getTargetId());
                return null;
            }
            dispatch = new Dispatch(nextVertex, conversation.getData(), logprefix, conversation.getRefrenceId());
        } else {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "in else : Next Vertex Id: " + vertex.toString());
            Vertex nextVertex;
            Optional<Vertex> optionalVertex = verticesRepostiory.findByFlowIdAndMxGraphId(conversation.getFlowId(), vertex.getStep().getTargetId());
            if(optionalVertex.isPresent()){
                nextVertex = optionalVertex.get();
            }else
            {
                Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertex not found with id: " + vertex.getStep().getTargetId());
                return null;
            }
            dispatch = new Dispatch(nextVertex, conversation.getData(), logprefix, conversation.getRefrenceId());
        }
        return dispatch;
    }

    /**
     * Performs provided actions for a conversation and returns next Step
     * accordingly.
     *
     * @param conversation
     * @param vertex
     * @return Next Step
     */
    private Dispatch getStepByAction(Conversation conversation, Vertex vertex) {

        String logprefix = conversation.getSenderId();

        Step step = null;

        Dispatch tempDispatch = null;

        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "actions count: " + vertex.getActions().size());
        for (Action action : vertex.getActions()) {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "actionType: " + action.getType());
            if (VertexActionType.EXTERNAL_REQUEST == action.getType()) {
                tempDispatch = action.getExternalRequest().processExternalRequest(conversation.getData(), restTemplate, logprefix);
            }

            if (null != tempDispatch.getStepId()) {
                Vertex nextVertex;
                Optional<Vertex> optionalVertex = verticesRepostiory.findByFlowIdAndMxGraphId(conversation.getFlowId(), tempDispatch.getStepId());
                if(optionalVertex.isPresent()){
                    nextVertex = optionalVertex.get();
                }else
                {
                    Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertex not found with id: " + tempDispatch.getStepId());
                    return null;
                }
                tempDispatch = new Dispatch(nextVertex, conversation.getData(), logprefix, conversation.getRefrenceId());
                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "error processing action: " + action.getType());
                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "errorStepTargetId: " + tempDispatch.getStepId());
                return tempDispatch;
            }
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "successfully processed action: " + action.getType());
        }

        if (step == null) {
            step = vertex.getStep();
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "no error assigning next step: " + step.getTargetId());
        }

        Vertex nextVertex ;
        Optional<Vertex> optionalVertex = verticesRepostiory.findByFlowIdAndMxGraphId(conversation.getFlowId(), vertex.getStep().getTargetId());
        if(optionalVertex.isPresent()){
            nextVertex = optionalVertex.get();
        }else
        {
            Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertex not found with id: " + vertex.getStep().getTargetId());
            return null;
        }

        Dispatch dispatch = new Dispatch(nextVertex, conversation.getData(), logprefix, conversation.getRefrenceId());
        dispatch.setVariables(tempDispatch.getVariables());
        return dispatch;
    }

    private Dispatch getStepByWizard(Conversation conversation, Vertex vertex, String value) {
        String logprefix = conversation.getSenderId();
        Dispatch dispatch = null;

        Vertex nextVertex ;
        Optional<Vertex> optionalVertex = verticesRepostiory.findByFlowIdAndMxGraphId(conversation.getFlowId(), vertex.getStep().getTargetId());
        if(optionalVertex.isPresent()){
            nextVertex = optionalVertex.get();
        }else
        {
            Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertex not found with id: " + vertex.getStep().getTargetId());
            return null;
        }
        dispatch = new Dispatch(nextVertex, conversation.getData(), logprefix, conversation.getRefrenceId());
        dispatch.setVariableValue(vertex.getDataVariable(), value);
        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "variable added " + vertex.getDataVariable() + ": " + value);
        return dispatch;
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
    private Dispatch getStepByMenu(Vertex vertex, Conversation conversation, String value) {
        String logprefix = conversation.getSenderId();
        Option option = vertex.matchOptions(value, conversation.getData().getVariables());

        if (null != option) {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "matched " + value + ": " + option.getText());
            Vertex nextVertex;
            Optional<Vertex> optionalVertex = verticesRepostiory.findByFlowIdAndMxGraphId(conversation.getFlowId(), option.getStep().getTargetId());
            if(optionalVertex.isPresent()){
                nextVertex = optionalVertex.get();
            }else
            {
                Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertex not found with id: " + option.getStep().getTargetId());
                return null;
            }
            Dispatch dispatch = new Dispatch(nextVertex, conversation.getData(), logprefix, conversation.getRefrenceId());

            String variableValue = option.getText();

            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, variableValue + " starts with and ends with $%: " + (variableValue.startsWith("$%") && variableValue.endsWith("$%")));

            if (variableValue.startsWith("$%") && variableValue.endsWith("$%")) {

                String varKey = variableValue.replace("$%", "");
                variableValue = conversation.getData().getVariableValue(varKey);
                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, " variableValue: " + variableValue);

            } else {
                variableValue = option.getText();
            }
            dispatch.setVariableValue(vertex.getDataVariable(), variableValue);
            return dispatch;
        }
        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "no value matched");
        Vertex nextVertex;
        Optional<Vertex> optionalVertex = verticesRepostiory.findByFlowIdAndMxGraphId(conversation.getFlowId(), vertex.getStep().getTargetId());
        if(optionalVertex.isPresent()){
            nextVertex = optionalVertex.get();
        }else
        {
            Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "vertex not found with id: " + vertex.getStep().getTargetId());
            return null;
        }
        Dispatch dispatch = new Dispatch(nextVertex, conversation.getData(), logprefix, conversation.getRefrenceId());
        return dispatch;
    }

}
