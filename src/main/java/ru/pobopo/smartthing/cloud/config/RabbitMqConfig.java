package ru.pobopo.smartthing.cloud.config;

import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import ru.pobopo.smartthing.cloud.rabbitmq.RabbitCreditsHolder;

@Slf4j
@Configuration
public class RabbitMqConfig {
    @Bean
    public RabbitCreditsHolder creditsHolder(Environment environment) {
        String user = environment.getProperty("BROKER_USER", "cloudadmin");
        String password = environment.getProperty("BROKER_PASSWORD", "supercoolpassword");
        return new RabbitCreditsHolder(user, password);
    }

    @Bean
    public ConnectionFactory connectionFactory(Environment environment, RabbitCreditsHolder creditsHolder) {
        String brokerHost = environment.getProperty("BROKER_HOST", "localhost");
        String brokerPort = environment.getProperty("BROKER_PORT", "5672");

        log.info("RabbitMq configured host and port: {}:{}", brokerHost, brokerPort);

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(brokerHost);
        connectionFactory.setPort(Integer.parseInt(brokerPort));
        connectionFactory.setUsername(creditsHolder.getLogin());
        connectionFactory.setPassword(creditsHolder.getPassword());
        return connectionFactory;
    }
}
