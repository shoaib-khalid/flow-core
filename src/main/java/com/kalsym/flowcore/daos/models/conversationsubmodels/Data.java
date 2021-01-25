package com.kalsym.flowcore.daos.models.conversationsubmodels;

import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
public class Data {

    private String currentVertexId;
    private int currentVertexSendCount;
    private HashMap<String, String> variables;
    private Boolean isGuest;

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
}
