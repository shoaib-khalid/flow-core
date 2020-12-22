package com.kalsym.flowcore.daos.models;

import com.kalsym.flowcore.daos.models.conversationsubmodels.Data;
import com.kalsym.flowcore.daos.models.vertexsubmodels.*;
import java.util.Date;
import java.util.List;
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
public class Vertex {

    @Id
    private String id;

    private Info info;

    private Validation validation;

    /**
     * Two variables from below will be null and only one will be assigned a
     * value
     */
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

    /**
     * Returns the step after matching the conditions inside the vertex. If non
     * of the conditions match returns the step from vertex.
     *
     * @param data Data from conversation
     * @return Step
     */
    public Step matchConditions(Data data) {

        for (Condition condition : conditions) {
            return condition.match(data);
        }

        return this.step;
    }

    /**
     * Traverses through options and takes matching value. If non of the options
     * match returns the step from vertex.
     *
     * @param targetId Id of next vertex
     * @return Step
     */
    public Step matchOptions(String targetId) {

        for (Option option : this.options) {
            if (targetId.equalsIgnoreCase(option.getStep().getTargetId())) {
                return option.getStep();
            }
        }

        return this.step;
    }

}
