package com.codexsphere.codearena.kafka.consumer;


import com.codexsphere.codearena.dto.request.ExecuteCodeRequest;
import com.codexsphere.codearena.execution.ExecutionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class SubmissionConsumer {

    private final ExecutionManager executionManager;

    @KafkaListener(
            topics = "submission-topic",
            groupId = "runner-group"
    )
    public void consume(ExecuteCodeRequest event) {

        executionManager.execute(event);

    }

}