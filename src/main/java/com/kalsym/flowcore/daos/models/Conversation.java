package com.kalsym.flowcore.daos.models;

import com.kalsym.flowcore.daos.models.conversationsubmodels.*;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
@Document
public class Conversation {

    @Id
    private String id;

    private Data data;

    @CreatedDate
    private Date createdDate;
    @LastModifiedDate
    private Date lastModifiedDate;

    private String senderId;
    private String refrenceId;

    private String latestVertexId;

    private String flowId;
}
