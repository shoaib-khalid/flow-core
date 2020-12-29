package com.kalsym.flowcore.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Data maybe a targetId for next vertex or simple string. Referral can be used
 * for for discounts or offers. callbackUrl is url where flow-core sends message
 * after completion of processing.
 *
 * @author Sarosh
 */
@Getter
@Setter
@ToString
public class RequestPayload {
    private String data;
    private String referral;
    private String msgId;
    private Boolean isGuest;
    private String callbackUrl;
}
