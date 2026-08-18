package com.codexsphere.codearena.kafka.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.util.List;

@Configuration
public class KafkaConsumerConfig {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {

        return new CommonErrorHandler() {

            @Override
            public boolean handleOne(
                    Exception thrownException,
                    ConsumerRecord<?, ?> record,
                    Consumer<?, ?> consumer,
                    MessageListenerContainer container
            ) {

                log.error(
                        "Kafka record processing failed. "
                                + "topic={}, partition={}, offset={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        thrownException
                );

                return true;
            }

            @Override
            public void handleRemaining(
                    Exception thrownException,
                    List<ConsumerRecord<?, ?>> records,
                    Consumer<?, ?> consumer,
                    MessageListenerContainer container
            ) {

                log.error(
                        "Kafka batch processing failed. records={}",
                        records.size(),
                        thrownException
                );
            }
        };
    }
}
