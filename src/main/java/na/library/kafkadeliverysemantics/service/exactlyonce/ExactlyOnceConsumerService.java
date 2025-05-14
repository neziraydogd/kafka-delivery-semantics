package na.library.kafkadeliverysemantics.service.exactlyonce;

import na.library.kafkadeliverysemantics.entity.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import na.library.kafkadeliverysemantics.entity.ProcessedMessage;
import na.library.kafkadeliverysemantics.repository.MessageProcessingRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("exactly-once")
@RequiredArgsConstructor
@Slf4j
public class ExactlyOnceConsumerService {

    // Simple in-memory deduplication store (should use a database in production)
    private final ConcurrentHashMap<String, Boolean> processedMessages = new ConcurrentHashMap<>();
    
    // In a real application, you would use a transactional database for storing processed IDs
    private final MessageProcessingRepository messageRepository;

    @KafkaListener(topics = "exactly-once-topic", groupId = "${consumer.exactlyonce.group.id}", containerFactory = "exactlyOnceContainerFactory")
    @Transactional("kafkaTransactionManager") // Use the same transaction manager as the producer
    public void consume(Message message, Acknowledgment acknowledgment) {
        String messageId = message.getId();
        
        try {
            // Check if this message has already been processed (deduplication)
            if (isMessageAlreadyProcessed(messageId)) {
                log.info("Message already processed, skipping: {}", messageId);
                acknowledgment.acknowledge();
                return;
            }
            
            log.info("Processing message in transaction: {}", message);
            
            // Process the message with your business logic
            processMessage(message);
            
            // Mark the message as processed in the same transaction
            markMessageAsProcessed(messageId);
            
            // Commit the offset as part of the transaction
            acknowledgment.acknowledge();
            
            log.info("Message processed and committed: {}", messageId);
        } catch (Exception e) {
            log.error("Error processing message (transaction will be rolled back): {}", messageId, e);
            // The transaction will be rolled back, so the message will be redelivered
            throw e; // Re-throw to ensure transaction rollback
        }
    }
    
    private boolean isMessageAlreadyProcessed(String messageId) {
        // First check the in-memory cache (fast)
        if (processedMessages.containsKey(messageId)) {
            return true;
        }
        
        // Then check the persistent store
        return messageRepository.existsByMessageId(messageId);
    }
    
    private void markMessageAsProcessed(String messageId) {
        // Update both in-memory cache and persistent store
        processedMessages.put(messageId, true);
        messageRepository.save(new ProcessedMessage(messageId, System.currentTimeMillis()));
    }
    
    private void processMessage(Message message) {
        // Implement your business logic here
        log.info("Processing message with ID: {}", message.getId());
        
        // Your actual business logic goes here
    }
}