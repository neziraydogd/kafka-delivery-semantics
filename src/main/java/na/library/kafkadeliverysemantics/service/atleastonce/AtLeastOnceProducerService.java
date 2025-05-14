package na.library.kafkadeliverysemantics.service.atleastonce;

import na.library.kafkadeliverysemantics.entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Profile("at-least-once")
@Slf4j
public class AtLeastOnceProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "at-least-once-topic";

    public AtLeastOnceProducerService(@Qualifier("atLeastOnceKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String content) {
        String messageId = UUID.randomUUID().toString();
        Message message =  Message.builder()
                .id(messageId)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
        
        // Send message and handle future result
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(TOPIC, messageId, message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Message sent successfully: {}, offset: {}", 
                        messageId, result.getRecordMetadata().offset());
            } else {
                // Handle failure - typically would retry or log for manual handling
                log.error("Unable to send message: " + messageId, ex);
            }
        });
    }
    
    // For critical messages where we need to ensure delivery
    public void sendMessageWithRetry(String content, int maxRetries) {
        String messageId = UUID.randomUUID().toString();
        Message message =  Message.builder()
                .id(messageId)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
        
        sendWithRetry(TOPIC, messageId, message, 0, maxRetries);
    }
    
    private void sendWithRetry(String topic, String key, Message message, int currentRetry, int maxRetries) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Message sent successfully: {}, offset: {}", 
                         message.getId(), result.getRecordMetadata().offset());
            } else {
                if (currentRetry < maxRetries) {
                    log.warn("Retrying message: {}, attempt: {}/{}", 
                             message.getId(), currentRetry + 1, maxRetries);
                    sendWithRetry(topic, key, message, currentRetry + 1, maxRetries);
                } else {
                    log.error("Failed to send message after {} retries: {}", 
                              maxRetries, message.getId(), ex);
                    // Here you would typically store the failed message for later recovery
                    storeFailedMessage(message);
                }
            }
        });
    }
    
    private void storeFailedMessage(Message message) {
        // Store in database or other persistent storage for manual recovery
        log.info("Storing failed message for recovery: {}", message.getId());
        // Implementation depends on your persistence layer
    }
}