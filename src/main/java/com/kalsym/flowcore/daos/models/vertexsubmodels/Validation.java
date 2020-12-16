package com.kalsym.flowcore.daos.models.vertexsubmodels;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
public class Validation {

    private String inputType;
    private String phone;
    private String regex;
    private Retry retry;

}
