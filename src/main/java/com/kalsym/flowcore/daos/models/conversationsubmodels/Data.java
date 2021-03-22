package com.kalsym.flowcore.daos.models.conversationsubmodels;

import com.kalsym.flowcore.VersionHolder;
import com.kalsym.flowcore.utils.Logger;
import java.util.HashMap;
import java.util.Map;
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
        String value = null;
//        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, "", " variableName: " + variableName);
//
//        for (Map.Entry<String, String> mapElement : variables.entrySet()) {
//            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, "s", "mapElement.getKey(): " + mapElement.getKey() + ": " + mapElement.getValue());
//        }
//
//        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, "s", "variables.get(" + variableName + "): " + variables.get(variableName));

        if (null != variables) {

            value = variables.get(variableName);
        }
        return value;
    }

    public void setVariableValue(String variableName, String value) {
        if (null == variables) {
            variables = new HashMap<>();
        }
        variables.put(variableName, value);
    }
}
