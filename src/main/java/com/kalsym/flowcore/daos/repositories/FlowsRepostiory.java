package com.kalsym.flowcore.daos.repositories;

import com.kalsym.flowcore.daos.models.Flow;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 *
 * @author Sarosh
 */
public interface FlowsRepostiory extends MongoRepository<Flow, String> {

    public Flow findByBotId(String botId);
}
