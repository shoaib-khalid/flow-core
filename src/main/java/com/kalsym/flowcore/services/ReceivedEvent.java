package com.kalsym.flowcore.services;

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
public class ReceivedEvent {

    private String object;
    private List<Entry> entry;
}
