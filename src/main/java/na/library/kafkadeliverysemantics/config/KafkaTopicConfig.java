package na.library.kafkadeliverysemantics.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("topic-creation")
public class KafkaTopicConfig {

    @Value("${kafka.topic.atleastonce.name}")
    private String atLeastOnceTopicName;

    @Value("${kafka.topic.atmostonce.name}")
    private String atMostOnceTopicName;

    @Value("${kafka.topic.exactlyonce.name}")
    private String exactlyOnceTopicName;


    @Value("${kafka.topic.partitions:3}")
    private int partitions;

    @Value("${kafka.topic.replication-factor:3}")
    private short replicationFactor;

    @Bean
    public NewTopic atLeastOnceTopic() {
        return TopicBuilder.name(atLeastOnceTopicName)
                .partitions(partitions)
                .replicas(replicationFactor)
                .config("min.insync.replicas", "2")
                .config("cleanup.policy", "delete")
                .config("retention.ms", "604800000")
                .config("segment.bytes", "1073741824")
                .config("max.message.bytes", "1000000")
                .build();
    }

    @Bean
    public NewTopic atMostOnceTopic() {
        return TopicBuilder.name(atMostOnceTopicName)
                .partitions(partitions)
                .replicas(replicationFactor)
                .config("min.insync.replicas", "2")
                .config("cleanup.policy", "delete")
                .config("retention.ms", "604800000")
                .config("segment.bytes", "1073741824")
                .config("max.message.bytes", "1000000")
                .build();
    }

    @Bean
    public NewTopic exactlyOnceTopic() {
        return TopicBuilder.name(exactlyOnceTopicName)
                .partitions(partitions)
                .replicas(replicationFactor)
                .config("min.insync.replicas", "2")
                .config("cleanup.policy", "delete")
                .config("retention.ms", "604800000")
                .config("segment.bytes", "1073741824")
                .config("max.message.bytes", "1000000")
                .build();
    }
}