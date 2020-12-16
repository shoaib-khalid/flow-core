package com.kalsym.flowcore.daos.models.vertexsubmodels;

import com.kalsym.flowcore.models.enums.VertexType;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
public class Info {

    private String title;
    private String text;
    private VertexType type;
}
