package na.library.kafkadeliverysemantics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KafkaDeliverySemanticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaDeliverySemanticsApplication.class, args);
    }

}
