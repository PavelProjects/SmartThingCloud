package ru.pobopo.smartthing.cloud.config;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class RabbitMqConfig {
    @Bean
    public RabbitCreditsHolder creditsHolder(Environment environment) {
        String user = environment.getProperty("BROKER_USER", "supercoollogin");
        String password = environment.getProperty("BROKER_PASSWORD", "evenmorecoolerpassword");
        return new RabbitCreditsHolder(user, password);
    }

    @Bean
    public ConnectionFactory connectionFactory(Environment environment, RabbitCreditsHolder creditsHolder) {
        String brokerHost = environment.getProperty("BROKER_HOST", "localhost");

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(brokerHost);
        connectionFactory.setUsername(creditsHolder.getLogin());
        connectionFactory.setPassword(creditsHolder.getPassword());
        return connectionFactory;
    }
}
