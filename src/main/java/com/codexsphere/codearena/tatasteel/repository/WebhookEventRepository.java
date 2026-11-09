package com.codexsphere.codearena.tatasteel.repository;

import com.codexsphere.codearena.tatasteel.whatsapp.model.WebhookEvent;
import com.codexsphere.codearena.tatasteel.whatsapp.model.WebhookEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    boolean existsByEventKey(String eventKey);

    Optional<WebhookEvent> findByEventKey(String eventKey);

    List<WebhookEvent> findTop50ByStatusAndRetryCountLessThanOrderByReceivedAtAsc(
            WebhookEventStatus status,
            int retryCount
    );

}
