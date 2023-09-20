package ru.pobopo.smartthing.cloud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.service.MessageBrokerService;
import ru.pobopo.smartthing.cloud.service.model.DeviceRequestMessage;
import ru.pobopo.smartthing.cloud.service.model.GatewayCommand;
import ru.pobopo.smartthing.cloud.service.model.GatewayMessageType;

@Component
@Slf4j
public class MessageBrokerServiceImpl implements MessageBrokerService {
    private final ConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public MessageBrokerServiceImpl(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void createQueues(GatewayEntity entity) throws IOException, TimeoutException {
        try (Connection connection = connectionFactory.newConnection()) {
            Channel channel = connection.createChannel();
            channel.queueDeclare(entity.getQueueInName(), false, false, false, null);
            channel.queueDeclare(entity.getQueueOutName(), false, false, false, null);
        }
    }

    @Override
    public void send(GatewayEntity entity, GatewayCommand gatewayCommand) throws IOException, TimeoutException {
        Objects.requireNonNull(entity, "Gateway entity is missing!");
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
            .type(GatewayMessageType.GATEWAY_COMMAND.getType())
            .build();
        sendToQueue(entity.getQueueInName(), properties, objectMapper.writeValueAsString(gatewayCommand));
    }

    @Override
    public void send(GatewayEntity entity, DeviceRequestMessage deviceRequestMessage) throws IOException, TimeoutException {
        Objects.requireNonNull(entity, "Gateway entity is missing!");
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
            .type(GatewayMessageType.DEVICE_REQUEST.getType())
            .build();
        sendToQueue(entity.getQueueInName(), properties, objectMapper.writeValueAsString(deviceRequestMessage));
    }

    private void sendToQueue(String queue, AMQP.BasicProperties properties, String message) throws IOException, TimeoutException {
        if (StringUtils.isBlank(queue) || StringUtils.isBlank(message)) {
            return;
        }

        try (Connection connection = connectionFactory.newConnection()) {
            Channel channel = connection.createChannel();
            log.info("Sending to {} message {}", queue, message);
            channel.basicPublish("", queue, properties, message.getBytes());
        }
    }

}
