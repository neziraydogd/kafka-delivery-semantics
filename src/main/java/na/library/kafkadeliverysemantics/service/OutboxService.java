package na.library.kafkadeliverysemantics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import na.library.kafkadeliverysemantics.entity.Message;
import na.library.kafkadeliverysemantics.entity.OutboxEvent;
import na.library.kafkadeliverysemantics.repository.OutboxRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("outbox")
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional("transactionManager")
    public void saveToOutbox(Message message, String eventType) {
        try {
            // Convert message to JSON
            String payload = objectMapper.writeValueAsString(message);

            // Create outbox event
            OutboxEvent outboxEvent = new OutboxEvent(
                    message.getId(),           // aggregateId
                    "Message",                 // aggregateType
                    eventType,                 // eventType
                    payload                    // payload
            );

            // Save to outbox table (will be committed in the same transaction)
            outboxRepository.save(outboxEvent);

            log.info("Message saved to outbox: {}", message.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message to JSON", e);
            throw new RuntimeException("Failed to save to outbox", e);
        }
    }
}