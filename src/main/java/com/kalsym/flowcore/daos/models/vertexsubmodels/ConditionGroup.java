package com.kalsym.flowcore.daos.models.vertexsubmodels;

import com.kalsym.flowcore.models.enums.ConditionOperator;
import lombok.Getter;
import lombok.Setter;


/**
 *
 * @author Sarosh
 */
@Getter
@Setter
public class ConditionGroup {

    private String field;
    private ConditionOperator condtionOperatoro;
    private Boolean caseSensitive;
    private String value ;
}
