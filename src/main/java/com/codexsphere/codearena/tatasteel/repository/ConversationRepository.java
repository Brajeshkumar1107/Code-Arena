package com.codexsphere.codearena.tatasteel.repository;

import com.codexsphere.codearena.tatasteel.conversation.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByPhoneNumber(String phoneNumber);

}
