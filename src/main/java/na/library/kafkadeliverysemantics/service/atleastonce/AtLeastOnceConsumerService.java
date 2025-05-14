package na.library.kafkadeliverysemantics.service.atleastonce;

import lombok.extern.slf4j.Slf4j;
import na.library.kafkadeliverysemantics.entity.Message;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@Profile("at-least-once")
@Slf4j
public class AtLeastOnceConsumerService {

    @KafkaListener(topics = "at-least-once-topic", containerFactory = "atLeastOnceContainerFactory", groupId = "${consumer.atleastonce.group.id}")
    public void consume(Message message, Acknowledgment acknowledgment) {
        try {
            log.info("Consuming message: {}", message);
            String a = null;
            if(a.length()>0){
                System.out.println("fafafaf");
            }
            // Process the message with your business logic
            processMessage(message);
            
            // Only acknowledge after successful processing
            acknowledgment.acknowledge();
            log.info("Message processed and acknowledged: {}", message.getId());
        } catch (Exception e) {
            // Log the error but don't acknowledge
            // The message will be redelivered according to Kafka's configuration
            log.error("Error processing message: {}", message.getId(), e);
            
            // Depending on your retry strategy, you might:
            // 1. Just log and let Kafka retry by not acknowledging
            // 2. Implement custom retry logic with backoff
            // 3. For poison messages, acknowledge after sending to DLQ
            handleProcessingError(message, e);
        }
    }
    
    private void processMessage(Message message) {
        // Implement your business logic here
        log.info("Processing message with ID: {}", message.getId());
        
        // Simulate processing that might fail randomly (for testing)
        if (Math.random() < 0.1) {
            throw new RuntimeException("Simulated random processing failure");
        }
    }
    
    private void handleProcessingError(Message message, Exception e) {
        // Track error count for this message ID in a persistent store
        // If error count exceeds threshold, send to DLQ and acknowledge
        // This is a simple implementation; in production, you'd use a DB to track retries
        
        log.warn("Message processing error, will be retried: {}", message.getId());
    }
}