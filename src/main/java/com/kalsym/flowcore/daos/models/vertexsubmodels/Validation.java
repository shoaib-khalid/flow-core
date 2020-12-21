package com.kalsym.flowcore.daos.models.vertexsubmodels;

import com.kalsym.flowcore.models.enums.ValidationInputType;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
public class Validation {

    private ValidationInputType inputType;
    private String phone;
    private String regex;
    private Retry retry;

}
