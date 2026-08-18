package com.codexsphere.codearena.kafka.consumer;


import com.codexsphere.codearena.dto.request.ExecuteCodeRequest;
import com.codexsphere.codearena.execution.ExecutionManager;
import com.codexsphere.codearena.kafka.event.SubmissionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
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