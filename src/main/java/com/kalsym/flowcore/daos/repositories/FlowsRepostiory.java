package com.kalsym.flowcore.daos.repositories;

import com.kalsym.flowcore.daos.models.Flow;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

/**
 *
 * @author Sarosh
 */
public interface FlowsRepostiory extends MongoRepository<Flow, String> {

    //public Flow findByBotId(String botId);
//    @Query(value = "{ 'bookletSignups': { $elemMatch: { 'bookletId' : ?0 } }}")
//    List<Flow> findByBotId(String id);

    @Query(value = "{ 'botIds' : ?0}")
    public List<Flow> findByBotIds(String id);
}
