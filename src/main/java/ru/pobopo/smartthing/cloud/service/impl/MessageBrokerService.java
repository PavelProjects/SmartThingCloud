package ru.pobopo.smartthing.cloud.service.impl;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
//todo add interface, save topic to db?
public class MessageBrokerService {
    private final ConnectionFactory connectionFactory;

    @Autowired
    public MessageBrokerService(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void createQueue(String queueName) throws IOException, TimeoutException {
        try (Connection connection = connectionFactory.newConnection()) {
            Channel channel = connection.createChannel();
            channel.queueDeclare(queueName, false, false, false, null);
        }
    }

    public void sendToQueue(String queue, String message) throws IOException, TimeoutException {
        try (Connection connection = connectionFactory.newConnection()) {
            Channel channel = connection.createChannel();
            log.info("Sending to {} message {}", queue, message);
            channel.basicPublish("", queue, null, message.getBytes());
        }
    }

}
