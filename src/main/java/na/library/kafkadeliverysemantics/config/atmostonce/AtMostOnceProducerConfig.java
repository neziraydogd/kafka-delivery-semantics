package na.library.kafkadeliverysemantics.config.atmostonce;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("at-most-once")
public class AtMostOnceProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.producer.key-serializer}")
    private String keySerializer;
    
    @Value("${spring.kafka.producer.value-serializer}")
    private String valueSerializer;

    @Bean
    public ProducerFactory<String, Object> atMostOnceProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        
        // At-most-once specific configurations
        configProps.put(ProducerConfig.ACKS_CONFIG, "0"); // Fire and forget (no acks)
        configProps.put(ProducerConfig.RETRIES_CONFIG, 0); // No retries
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 0); // No artificial delay
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean("atMostOnceKafkaTemplate")
    public KafkaTemplate<String, Object> atMostOnceKafkaTemplate() {
        return new KafkaTemplate<>(atMostOnceProducerFactory());
    }
}