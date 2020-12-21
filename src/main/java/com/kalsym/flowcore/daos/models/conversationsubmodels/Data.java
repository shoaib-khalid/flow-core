package com.kalsym.flowcore.daos.models.conversationsubmodels;

import java.util.HashMap;import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
public class Data {

    private String currentVertexId;
    private int currentVertexSendCount;
    private HashMap<String, String> variables;
}
