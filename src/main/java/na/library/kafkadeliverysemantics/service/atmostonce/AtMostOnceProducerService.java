package na.library.kafkadeliverysemantics.service.atmostonce;

import lombok.extern.slf4j.Slf4j;
import na.library.kafkadeliverysemantics.entity.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Profile("at-most-once")
@Slf4j
public class AtMostOnceProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "at-most-once-topic";

    public AtMostOnceProducerService(@Qualifier("atMostOnceKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String content) {
        String messageId = UUID.randomUUID().toString();
        Message message =  Message.builder()
                .id(messageId)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
        // Fire and forget - we don't wait for any acknowledgment
        kafkaTemplate.send(TOPIC, messageId, message);
        log.info("Message sent in fire-and-forget mode: {}", messageId);
    }
}