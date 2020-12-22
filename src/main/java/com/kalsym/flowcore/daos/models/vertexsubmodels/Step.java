package com.kalsym.flowcore.daos.models.vertexsubmodels;

import com.kalsym.flowcore.models.enums.VertexTargetType;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
public class Step {

    private VertexTargetType targetType;
    private String targetId;

}
