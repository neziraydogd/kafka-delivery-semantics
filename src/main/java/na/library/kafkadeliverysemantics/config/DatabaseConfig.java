package na.library.kafkadeliverysemantics.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "na.library.kafkadeliverysemantics")
@Profile("exactly-once")
public class DatabaseConfig {

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager dbTransactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}