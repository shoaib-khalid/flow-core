package com.kalsym.flowcore.daos.models;

import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
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
@ToString
public class Flow {

    @Id
    public String id;

    public String title;

    public String description;

    @CreatedDate
    public Date createdDate;
    @LastModifiedDate
    public Date lastModifiedDate;

    public List<String> botIds;

    public String topVertexId;
    
    public String storeId;

}
