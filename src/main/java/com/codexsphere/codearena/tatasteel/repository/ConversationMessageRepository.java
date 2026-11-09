package com.codexsphere.codearena.tatasteel.repository;

import com.codexsphere.codearena.tatasteel.conversation.model.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findTop20ByConversationIdOrderByCreatedAtDesc(Long conversationId);

}
