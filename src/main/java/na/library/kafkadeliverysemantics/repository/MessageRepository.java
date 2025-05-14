package na.library.kafkadeliverysemantics.repository;

import na.library.kafkadeliverysemantics.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
}