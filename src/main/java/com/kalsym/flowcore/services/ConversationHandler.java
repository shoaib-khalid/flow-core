package com.kalsym.flowcore.services;

import com.kalsym.flowcore.daos.models.Conversation;
import com.kalsym.flowcore.daos.repositories.ConversationsRepostiory;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Handles conversation based on sender and refrenceId.
 *
 * @author Sarosh
 */
@Service
public class ConversationHandler {

    @Autowired
    ConversationsRepostiory conversationsRepostiory;

    /**
     * *
     * Return conversation of sender. If conversation does not exist returns a
     * new conversation.
     *
     * @param senderId
     * @param refrenceId
     * @return
     */
    public Conversation getConversation(String senderId, String refrenceId) {

        Conversation searchConvo = new Conversation();
        searchConvo.setSenderId(senderId);
        searchConvo.setRefrenceId(refrenceId);

        Example convoExample = Example.of(searchConvo,
                ExampleMatcher.matching()
                        .withStringMatcher(StringMatcher.CONTAINING)
                        .withIgnoreCase());

        List<Conversation> conversationList = conversationsRepostiory.findAll(convoExample, Sort.by("lastModifiedDate").descending());

        if (conversationList.size() > 0) {
            return conversationList.get(0);
        }

        Conversation newConversation = new Conversation();

        newConversation.setSenderId(senderId);
        newConversation.setRefrenceId(refrenceId);

        return conversationsRepostiory.save(newConversation);

    }
}
