package com.kalsym.flowcore.daos.models.vertexsubmodels;

import com.kalsym.flowcore.daos.models.conversationsubmodels.Data;
import lombok.Getter;
import lombok.Setter;
import com.kalsym.flowcore.models.enums.ConditionOperator;
import com.kalsym.flowcore.utils.Logger;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
public class Condition {

    private ConditionOperator operator;
    private List<ConditionGroup> groups;
    private Step step;

    public Step match(Data data) {


        Step step = null;

        HashMap<String, String> variables = data.getVariables();

        for (ConditionGroup conditionGroup : groups) {

            String variableName = conditionGroup.getField();
            String variableValue = variables.get(variableName);

            if (ConditionOperator.OR == operator) {
                if (conditionGroup.match(variableValue)) {
                    return this.step;
                }
            } else {
                if (conditionGroup.match(variableValue)) {
                    step = this.step;
                } else {
                    step = null;
                }
            }

        }
        return step;
    }
}
