package com.kalsym.flowcore.daos.repositories;

import com.kalsym.flowcore.daos.models.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Sarosh
 */
@Repository
public interface ConversationsRepostiory extends MongoRepository<Conversation, String> {
    
    
    

}
