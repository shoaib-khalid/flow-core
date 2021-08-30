package com.kalsym.flowcore.models;

import com.kalsym.flowcore.models.pushmessages.*;
import com.kalsym.flowcore.daos.models.*;
import com.kalsym.flowcore.daos.models.conversationsubmodels.Data;
import com.kalsym.flowcore.models.enums.VertexType;
import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
public class Dispatch {

    private String stepId;
    private PushMessage pushMessage;
    private VertexType type;
    private HashMap<String, String> variables;
    private boolean repeat;
    Vertex vertex;
    private String referenceId;

    private Integer isLastVertex;

    public Dispatch() {
    }

    public Dispatch(Vertex vertex, Data data, String refId, String referenceId) {
        this.stepId = vertex.getMxGraphId();
        this.type = vertex.getInfo().getType();
        this.vertex = vertex;
        this.referenceId = referenceId;
        this.isLastVertex = vertex.getIsLastVertex();
        pushMessage = vertex.getPushMessage(data, refId);
    }

    public String getVariableValue(String variableName) {
        if (null != variables) {
            variables.get(variableName);
        }
        return null;
    }

    public void setVariableValue(String variableName, String value) {
        if (null == variables) {
            variables = new HashMap<>();
        }
        variables.put(variableName, value);
    }

    public PushMessage generatePushMessage(Data data, String refId) {
        return vertex.getPushMessage(data, refId);
    }

}
