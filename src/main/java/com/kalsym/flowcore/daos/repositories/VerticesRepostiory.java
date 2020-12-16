package com.kalsym.flowcore.daos.repositories;

import com.kalsym.flowcore.daos.models.Vertex;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 *
 * @author Sarosh
 */
public interface VerticesRepostiory extends MongoRepository<Vertex, String> {

}
