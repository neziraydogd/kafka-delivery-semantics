package na.library.kafkadeliverysemantics.service.atmostonce;

import na.library.kafkadeliverysemantics.entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Profile("at-most-once")
@Slf4j
public class AtMostOnceConsumerService {

    @KafkaListener(topics = "at-most-once-topic", containerFactory = "atMostOnceContainerFactory", groupId = "${consumer.atmostonce.group.id}")
    public void consume(Message message) {
        log.info("Consumed message: {}", message);
        // Process message without any additional checks or error handling
        // If processing fails, the message is lost (at-most-once semantics)
        processMessage(message);
    }
    
    private void processMessage(Message message) {
        // Business logic for processing the message
        log.info("Processing message with ID: {}", message.getId());
    }
}