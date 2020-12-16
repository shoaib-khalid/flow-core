package com.kalsym.flowcore.daos.models.vertexsubmodels;

import com.kalsym.flowcore.models.enums.VertexActionType;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
public class Action {

    private VertexActionType type;
    
    private ExternalRequest externalRequest;
}
