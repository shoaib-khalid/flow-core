package com.kalsym.flowcore.services;

import java.math.BigDecimal;
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
public class Messaging {

    private BigDecimal timestamp;
    private Sender sender;
    private Recipient recipient;

    private Postback postback;

    private Message message;
}
