package na.library.kafkadeliverysemantics.service;

import na.library.kafkadeliverysemantics.entity.Message;
import na.library.kafkadeliverysemantics.service.exactlyonce.ExactlyOnceProducerService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class KafkaSpringTransactionService {
    private final MessageService messageService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.transactiontopic}")
    private String transactiontopic;

    private static final int SEND_TIMEOUT_SECONDS = 10;

    public KafkaSpringTransactionService(MessageService messageService,
                                         @Qualifier("exactlyOnceKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        this.messageService = messageService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional("transactionManager")
    public void sendMessage(String content) {
        String messageId = UUID.randomUUID().toString();
        Message message = messageService.createMessage(content);
        try {
            SendResult<String, Object> result = kafkaTemplate.send(transactiontopic, messageId, message)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            System.out.println(result.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExactlyOnceProducerService.MessageSendException("Thread interrupted while sending message: " + messageId, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new ExactlyOnceProducerService.MessageSendException("Failed to send message: " + messageId, e);
        }
    }
}
