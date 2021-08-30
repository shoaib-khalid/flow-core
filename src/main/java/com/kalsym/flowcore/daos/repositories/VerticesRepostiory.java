package com.kalsym.flowcore.daos.repositories;

import com.kalsym.flowcore.daos.models.Vertex;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author Sarosh
 */
public interface VerticesRepostiory extends MongoRepository<Vertex, String> {

    @Query(value = "{'flowId' : ?0, 'mxGraphId' : ?1}")
    public Optional<Vertex> findByMxGraphId(String flowId, String mxGraphId);

}
