package na.library.kafkadeliverysemantics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedMessage {
    
    @Id
    @Column(name = "message_id", nullable = false, unique = true)
    private String messageId;
    
    @Column(name = "processed_timestamp", nullable = false)
    private long processedTimestamp;
}