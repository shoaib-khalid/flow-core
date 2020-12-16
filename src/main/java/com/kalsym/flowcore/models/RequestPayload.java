package com.kalsym.flowcore.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
@ToString
public class RequestPayload {

    private String data;
    private String refId;
    private boolean isGuest;

    private String callbackUrl;
}
