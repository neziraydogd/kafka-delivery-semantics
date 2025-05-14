package na.library.kafkadeliverysemantics.service.exactlyonce;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import na.library.kafkadeliverysemantics.entity.Message;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service responsible for sending messages to Kafka with exactly-once semantics.
 * Uses Kafka transactions to ensure messages are delivered exactly once.
 */
@Service
@Profile("exactly-once")
@Slf4j
public class ExactlyOnceProducerService {

    private static final String TOPIC = "exactly-once-topic";
    private static final int SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ExactlyOnceProducerService(@Qualifier("exactlyOnceKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a single message to Kafka within a transaction.
     * If any exception occurs, the transaction will be rolled back.
     *
     * @param content The message content to send
     * @throws MessageSendException if the message cannot be sent
     */
    @Transactional("kafkaTransactionManager")
    @Retryable(
            value = {MessageSendException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendMessage(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be null or empty");
        }

        String messageId = UUID.randomUUID().toString();
        Message message = createMessage(messageId, content);

        try {
            // Send synchronously to ensure transactional behavior
            SendResult<String, Object> result = kafkaTemplate.send(TOPIC, messageId, message)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("Message sent successfully in transaction: {}, offset: {}",
                    messageId, result.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessageSendException("Thread interrupted while sending message: " + messageId, e);
        } catch (ExecutionException | TimeoutException e) {
            log.error("Failed to send message: {}", messageId, e);
            throw new MessageSendException("Failed to send message: " + messageId, e);
        }
    }

    /**
     * Sends multiple messages in a single transaction.
     * All messages will be sent or none will be (atomic).
     *
     * @param contents Array of message contents to send
     * @throws MessageSendException if any message cannot be sent
     */
    @Transactional("kafkaTransactionManager")
    @Retryable(
            value = {MessageSendException.class},
            maxAttempts = 2,
            backoff = @Backoff(delay = 1000)
    )
    public void sendMessagesInTransaction(String... contents) {
        if (contents == null || contents.length == 0) {
            throw new IllegalArgumentException("Message contents cannot be null or empty");
        }

        try {
            for (String content : contents) {
                if (content == null || content.isBlank()) {
                    throw new IllegalArgumentException("Individual message content cannot be null or empty");
                }

                String messageId = UUID.randomUUID().toString();
                Message message = createMessage(messageId, content);

                // Send but don't wait for result of each individual message
                CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(TOPIC, messageId, message);
                future.whenComplete((result, ex) -> handleSendResult(messageId, result, ex));

                log.debug("Message queued in transaction: {}", messageId);
            }

            // Ensure all messages are sent by flushing
            kafkaTemplate.flush();
            //If it is stopped here, it is observed that the messages are not committed.
            // Because kafkaTransactionManager has not committed yet.
            log.info("docker exec kafka1 kafka-console-consumer --bootstrap-server kafka1:9092 --topic exactly-once-topic --from-beginning --isolation-level read_uncommitted");
        } catch (Exception e) {
            log.error("Transaction failed, will be rolled back", e);
            throw new MessageSendException("Failed to send messages in transaction", e);
        }
    }

    /**
     * Creates a message entity with the given ID and content.
     *
     * @param messageId Unique identifier for the message
     * @param content Message content
     * @return The created message entity
     */
    private Message createMessage(String messageId, String content) {
        return Message.builder()
                .id(messageId)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Handles the result of asynchronous message sending.
     * This is used for batch messages where we don't need to wait for each result.
     *
     * @param messageId ID of the message being sent
     * @param result Result of the send operation (may be null if an exception occurred)
     * @param ex Exception that occurred during sending (may be null if successful)
     */
    private void handleSendResult(String messageId, SendResult<String, Object> result, Throwable ex) {
        if (ex == null) {
            log.debug("Message sent successfully: {}, offset: {}",
                    messageId, result.getRecordMetadata().offset());
        } else {
            log.error("Failed to send message: {}", messageId, ex);
            // The exception will propagate and cause the transaction to roll back
            throw new MessageSendException("Failed to send message: " + messageId, ex);
        }
    }

    /**
     * Custom exception for message sending failures.
     * Used to isolate Kafka-specific exceptions and provide better retry handling.
     */
    public static class MessageSendException extends RuntimeException {
        public MessageSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}