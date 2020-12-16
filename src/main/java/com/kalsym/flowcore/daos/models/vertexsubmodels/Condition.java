package com.kalsym.flowcore.daos.models.vertexsubmodels;

import lombok.Getter;
import lombok.Setter;
import com.kalsym.flowcore.models.enums.ConditionOperator;
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
}
