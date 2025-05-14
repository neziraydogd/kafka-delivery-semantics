package na.library.kafkadeliverysemantics.repository;

import na.library.kafkadeliverysemantics.entity.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageProcessingRepository extends JpaRepository<ProcessedMessage, String> {
    boolean existsByMessageId(String messageId);
}