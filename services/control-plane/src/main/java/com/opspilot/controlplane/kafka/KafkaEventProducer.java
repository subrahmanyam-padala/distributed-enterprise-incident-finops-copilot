package com.opspilot.controlplane.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Enterprise Kafka event producer with callback logging and trace preservation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public CompletableFuture<SendResult<String, String>> send(String topic, String key, String payload) {
        log.debug("Publishing message to topic [{}] with key [{}]", topic, key);

        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, payload);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully sent message to topic [{}] partition [{}] offset [{}] key [{}]",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        key);
            } else {
                log.error("Failed to send message to topic [{}] key [{}]: {}", topic, key, ex.getMessage(), ex);
            }
        });

        return future;
    }
}
