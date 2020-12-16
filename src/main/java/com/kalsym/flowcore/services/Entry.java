package com.kalsym.flowcore.services;

import java.math.BigDecimal;
import java.util.List;
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
public class Entry {

    private String id;
    private BigDecimal time;
    private List<Messaging> messaging;
}
