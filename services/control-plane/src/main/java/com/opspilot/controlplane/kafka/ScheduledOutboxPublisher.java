package com.opspilot.controlplane.kafka;

import com.opspilot.controlplane.domain.entity.OutboxEvent;
import com.opspilot.controlplane.domain.enums.OutboxStatus;
import com.opspilot.controlplane.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled poller implementing the Transactional Outbox Pattern.
 * Periodically polls the outbox_events table for PENDING events and publishes
 * them to Kafka, ensuring at-least-once delivery semantics without dual writes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledOutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaEventProducer kafkaEventProducer;

    @Value("${opspilot.outbox.batch-size:50}")
    private int batchSize;

    @Value("${opspilot.outbox.enabled:true}")
    private boolean outboxEnabled;

    private static final int MAX_RETRIES = 5;

    @Scheduled(fixedDelayString = "${opspilot.outbox.poll-interval-ms:5000}")
    @Transactional
    public void processOutbox() {
        if (!outboxEnabled) {
            return;
        }

        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents(
                OutboxStatus.PENDING,
                PageRequest.of(0, batchSize)
        );

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox event(s) to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            publishSingleEvent(event);
        }
    }

    public void publishSingleEvent(OutboxEvent event) {
        try {
            kafkaEventProducer.send(event.getTopic(), event.getPartitionKey(), event.getPayload())
                    .get(); // Synchronous get to confirm broker acknowledgment

            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(Instant.now());
            event.setLastError(null);
            outboxEventRepository.save(event);

            log.info("Outbox event [{}] published successfully to topic [{}]",
                    event.getEventId(), event.getTopic());
        } catch (Exception ex) {
            int retries = event.getRetryCount() + 1;
            event.setRetryCount(retries);
            event.setLastError(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());

            if (retries >= MAX_RETRIES) {
                event.setStatus(OutboxStatus.FAILED);
                log.error("Outbox event [{}] exceeded max retries ({}), marked as FAILED: {}",
                        event.getEventId(), MAX_RETRIES, ex.getMessage());
            } else {
                log.warn("Outbox event [{}] publish failed (retry {}/{}): {}",
                        event.getEventId(), retries, MAX_RETRIES, ex.getMessage());
            }
            outboxEventRepository.save(event);
        }
    }
}
