package com.kalsym.flowcore.daos.models.vertexsubmodels;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
public class Retry {

    private int count;
    private String message;

    private Step failureStep;
}
