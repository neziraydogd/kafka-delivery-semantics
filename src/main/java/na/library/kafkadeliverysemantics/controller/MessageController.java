package na.library.kafkadeliverysemantics.controller;

import lombok.RequiredArgsConstructor;
import na.library.kafkadeliverysemantics.entity.Message;
import na.library.kafkadeliverysemantics.service.KafkaSpringTransactionService;
import na.library.kafkadeliverysemantics.service.MessageService;
import na.library.kafkadeliverysemantics.service.atleastonce.AtLeastOnceProducerService;
import na.library.kafkadeliverysemantics.service.atmostonce.AtMostOnceProducerService;
import na.library.kafkadeliverysemantics.service.exactlyonce.ExactlyOnceProducerService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    // Conditional autowiring based on active profile
    private final AtMostOnceProducerService atMostOnceProducerService;
    private final AtLeastOnceProducerService atLeastOnceProducerService;
    private final ExactlyOnceProducerService exactlyOnceProducerService;
    private final MessageService messageService;
    private final KafkaSpringTransactionService kafkaSpringTransactionService;

    @PostMapping("/at-least-once")
    @Profile("at-least-once")
    public ResponseEntity<String> sendAtLeastOnce(@RequestBody String content) {
        atLeastOnceProducerService.sendMessage(content);
        return ResponseEntity.ok("Message sent with at-least-once delivery semantics");
    }

    @PostMapping("/at-most-once")
    @Profile("at-most-once")
    public ResponseEntity<String> sendAtMostOnce(@RequestBody String content) {
        atMostOnceProducerService.sendMessage(content);
        return ResponseEntity.ok("Message sent with at-most-once delivery semantics");
    }

    @PostMapping("/at-least-once-retry")
    @Profile("at-least-once")
    public ResponseEntity<String> sendAtLeastOnceWithRetry(@RequestBody String content) {
        atLeastOnceProducerService.sendMessageWithRetry(content, 3);
        return ResponseEntity.ok("Message sent with at-least-once delivery semantics and retry");
    }

    @PostMapping("/exactly-once")
    @Profile("exactly-once")
    public ResponseEntity<String> sendExactlyOnce(@RequestBody String content) {
        exactlyOnceProducerService.sendMessage(content);
        return ResponseEntity.ok("Message sent with exactly-once delivery semantics");
    }

    @PostMapping("/exactly-once-batch")
    @Profile("exactly-once")
    public ResponseEntity<String> sendExactlyOnceBatch(@RequestBody String[] contents) {
        exactlyOnceProducerService.sendMessagesInTransaction(contents);
        return ResponseEntity.ok("Multiple messages sent with exactly-once delivery semantics");
    }

    @PostMapping("/outbox")
    @Profile("outbox")
    public ResponseEntity<Message> createWithOutbox(@RequestBody String content) {
        Message message = messageService.createMessage(content);
        return ResponseEntity.ok(message);
    }

    @PutMapping("/outbox/{id}")
    @Profile("outbox")
    public ResponseEntity<Message> updateWithOutbox(@PathVariable String id, @RequestBody String content) {
        Message message = messageService.updateMessage(id, content);
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/outbox/{id}")
    @Profile("outbox")
    public ResponseEntity<Void> deleteWithOutbox(@PathVariable String id) {
        messageService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/kafkaSpringTransaction")
    @Profile("outbox")
    public ResponseEntity<Boolean> kafkaSpringTransaction(@RequestBody String content) {
        kafkaSpringTransactionService.sendMessage(content);
        return ResponseEntity.ok(true);
    }
}