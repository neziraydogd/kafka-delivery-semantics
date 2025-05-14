package na.library.kafkadeliverysemantics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import na.library.kafkadeliverysemantics.entity.Message;
import na.library.kafkadeliverysemantics.entity.OutboxEvent;
import na.library.kafkadeliverysemantics.repository.OutboxRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Profile("exactly-once")
@Slf4j
public class OutboxRelayService {

    private static final int SEND_TIMEOUT_SECONDS = 10;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxRelayService(OutboxRepository outboxRepository,
                              @Qualifier("exactlyOnceKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
                              ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    // Run every 15 seconds
    @Scheduled(fixedRate = 15000)
    public void relayMessages() {
        List<OutboxEvent> unprocessedEvents = outboxRepository.findUnprocessedEvents();

        if (!unprocessedEvents.isEmpty()) {
            log.info("Found {} unprocessed events to relay", unprocessedEvents.size());

            for (OutboxEvent event : unprocessedEvents) {
                try {
                    // Use kafkaTemplate.executeInTransaction to ensure there's always a transaction
                    kafkaTemplate.executeInTransaction(operations -> {
                        relayEventInTransaction(event, operations);
                        return null;
                    });
                } catch (Exception e) {
                    log.error("Failed to relay event: {}", event.getId(), e);
                    // Continue with the next event
                }
            }
        }
    }

    // This method handles the Kafka message send process within an existing transaction
    private void relayEventInTransaction(OutboxEvent event, KafkaOperations<String, Object> operations) {
        if (event == null || event.getPayload() == null) {
            log.error("Invalid event received: {}", event);
            return;
        }

        try {
            // Deserialize the event payload to Message object
            Message message = objectMapper.readValue(event.getPayload(), Message.class);

            // Determine the topic based on event type
            String topic = determineTopicFromEventType(event.getEventType());

            // Send message to Kafka using the operations template that's already in a transaction
            CompletableFuture<SendResult<String, Object>> future = operations.send(topic, message.getId(), message);

            try {
                // Wait for the result with a timeout to avoid blocking indefinitely
                SendResult<String, Object> result = future.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                // Log the result of the Kafka send operation
                log.info("Relayed message to Kafka: {}, offset: {}", message.getId(), result.getRecordMetadata().offset());

                // After successful message send, mark the event as processed in the database
                event.setProcessed(true);
                outboxRepository.save(event);  // Save event status to mark it as processed

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread interrupted while relaying message: {}", message.getId(), e);
                throw new RuntimeException("Thread interrupted while relaying message", e);
            } catch (ExecutionException e) {
                log.error("Error executing Kafka send for message: {}", message.getId(), e);
                throw new RuntimeException("Failed to send message to Kafka", e.getCause());
            } catch (TimeoutException e) {
                log.error("Timeout while sending message to Kafka: {}", message.getId(), e);
                throw new RuntimeException("Timeout sending message to Kafka", e);
            }

        } catch (Exception e) {
            // In case of failure (Kafka or JPA), the transaction will roll back
            log.error("Error processing outbox event: {}", event.getId(), e);
            throw new RuntimeException("Failed to relay message", e);
        }
    }

    // This helper method determines the Kafka topic based on the event type
    private String determineTopicFromEventType(String eventType) {
        if (eventType == null) {
            log.warn("Event type is null, using default topic");
            return "default-topic";
        }

        // Map event types to Kafka topics
        return switch (eventType) {
            case "MESSAGE_CREATED" -> "message-created-topic";
            case "MESSAGE_UPDATED" -> "message-updated-topic";
            case "MESSAGE_DELETED" -> "message-deleted-topic";
            default -> "default-topic";
        };
    }
}