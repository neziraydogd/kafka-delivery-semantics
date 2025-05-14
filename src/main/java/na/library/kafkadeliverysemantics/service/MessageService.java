package na.library.kafkadeliverysemantics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import na.library.kafkadeliverysemantics.entity.Message;
import na.library.kafkadeliverysemantics.repository.MessageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Profile("outbox")
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final OutboxService outboxService;

    @Transactional("transactionManager")
    public Message createMessage(String content) {
        // Create the message
        String messageId = UUID.randomUUID().toString();
        Message message = Message.builder()
                .id(messageId)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();

        // Save to database
        messageRepository.save(message);
        log.info("Saved message to database: {}", messageId);

        // Save to outbox table in the same transaction
        outboxService.saveToOutbox(message, "MESSAGE_CREATED");

        return message;
    }

    @Transactional("transactionManager")
    public Message updateMessage(String messageId, String newContent) {
        // Find and update the message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        message.setContent(newContent);
        message.setTimestamp(LocalDateTime.now());

        // Save to database
        messageRepository.save(message);
        log.info("Updated message in database: {}", messageId);

        // Save to outbox table in the same transaction
        outboxService.saveToOutbox(message, "MESSAGE_UPDATED");

        return message;
    }

    @Transactional("transactionManager")
    public void deleteMessage(String messageId) {
        // Find the message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        // Delete from database
        messageRepository.deleteById(messageId);
        log.info("Deleted message from database: {}", messageId);

        // Save delete event to outbox table in the same transaction
        outboxService.saveToOutbox(message, "MESSAGE_DELETED");
    }
}