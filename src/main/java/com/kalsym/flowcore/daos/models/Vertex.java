package com.kalsym.flowcore.daos.models;

import com.kalsym.flowcore.daos.models.vertexsubmodels.*;
import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
@Document
public class Vertex {

    @Id
    private String id;

    private Info info;

    private Validation validation;

    private List<Condition> conditions;
    private List<Action> actions;
    private List<Option> options;

    private Step step;

    private String customVariableName;

    @CreatedDate
    private Date createdDate;
    @LastModifiedDate
    private Date lastModifiedDate;

    private String flowId;

}
