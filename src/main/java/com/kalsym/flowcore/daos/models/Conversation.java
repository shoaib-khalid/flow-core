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

    /**
     *
     */
    private Data data;

    @CreatedDate
    private Date createdDate;
    @LastModifiedDate
    private Date lastModifiedDate;

    private String senderId;
    private String refrenceId;

    private String flowId;

    public void shiftVertex(String vertexId) {

        if (null == this.data) {
            this.data = new Data();
        }

        data.setCurrentVertexId(vertexId);
    }

    public void shiftVertex(Vertex vertex) {

        if (null == this.data) {
            this.data = new Data();
        }

        data.setCurrentVertexId(vertex.getId());
        this.flowId = vertex.getFlowId();
    }

    public String getVariableValue(String variableName) {
        if (null == this.data) {
            this.data = new Data();
        }

        return data.getVariableValue(variableName);
    }

    public void setVariableValue(String variableName, String value) {
        if (null == this.data) {
            this.data = new Data();
        }

        data.setVariableValue(variableName, value);

    }

    public void setIsGuest(Boolean isGuest) {
        if (null == this.data) {
            this.data = new Data();
        }

        data.setIsGuest(isGuest);
    }

    public Boolean getIsGuest() {
        if (null == this.data) {
            this.data = new Data();
            return false;
        }

        return data.getIsGuest();
    }
}
