package ru.pobopo.smartthing.cloud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.UnsupportedMessageClassException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.rabbitmq.*;
import ru.pobopo.smartthing.cloud.service.RabbitMqService;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Component
@Slf4j
public class RabbitMqServiceImpl implements RabbitMqService {
    private final ConnectionFactory connectionFactory;
    private Connection connection;
    private Channel channel;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> consumersTags = new HashSet<>();

    @Value("${BROKER_HOST_GLOBAL}")
    private String brokerHost;

    @Value("${BROKER_PORT}")
    private int brokerPort;

    @Autowired
    public RabbitMqServiceImpl(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) throws IOException, TimeoutException {
        this.connection = connectionFactory.newConnection();
        this.channel = connection.createChannel();
    }

    @PreDestroy
    public void closeConnection() throws IOException {
        connection.close();
    }

    @Override
    public String getBrokeHost() {
        return brokerHost;
    }

    @Override
    public int getBrokePort() {
        return brokerPort;

    }

    @Override
    public void createQueues(GatewayEntity entity) throws IOException {
        channel.queueDeclare(entity.getConfig().getQueueIn(), false, false, false, null);
        channel.queueDeclare(entity.getConfig().getQueueOut(), false, false, false, null);
        log.info("Created queues for {}", entity);
    }

    @Override
    public void deleteQueues(GatewayEntity entity) throws IOException {
        channel.queueDelete(entity.getConfig().getQueueIn());
        channel.queueDelete(entity.getConfig().getQueueOut());
        log.info("Removed queues for {}", entity);
    }

    @Override
    public void addQueueListener(GatewayEntity entity, Consumer<MessageResponse> consumer) throws IOException {
        String consumerTag = buildConsumerTag(entity); // very bad todo rework!
        if (consumersTags.contains(consumerTag)) {
            log.info("Consumer with tag {} already exists", consumerTag);
            return;
        }
        channel.basicConsume(
            entity.getConfig().getQueueOut(),
            false,
            consumerTag,
            new MessageConsumer(channel, consumer)
        );
        consumersTags.add(consumerTag);
    }

    @Override
    public void removeQueueListener(GatewayEntity entity) throws IOException {
        String consumerTag = buildConsumerTag(entity);
        if (consumersTags.contains(consumerTag)) {
            channel.basicCancel(consumerTag);
            consumersTags.remove(consumerTag);
        } else {
            log.warn("No consumers for gateway {} by tag {}", entity, consumerTag);
        }
    }

    @Override
    public <T extends BaseMessage> String send(GatewayEntity entity, T message)
        throws IOException, UnsupportedMessageClassException, ValidationException {
        Objects.requireNonNull(entity, "Gateway entity is missing!");
        String queue = entity.getConfig().getQueueIn();

        if (StringUtils.isBlank(queue)) {
            throw new ValidationException("Queue in name are blank!");
        }

        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
            .type(defineMessageType(message).getType())
            .build();

        String messageStr = objectMapper.writeValueAsString(message);
        log.info("Sending to {} message {}", queue, message);
        channel.basicPublish("", queue, properties, messageStr.getBytes());
        return messageStr;
    }

    @Override
    public boolean isOnline(GatewayEntity gateway) throws IOException {
        if (gateway == null || gateway.getConfig() == null) {
            return false;
        }
        return channel.consumerCount(gateway.getConfig().getQueueIn()) > 0;
    }

    @NonNull
    private <T extends BaseMessage> GatewayMessageType defineMessageType(T message)
        throws UnsupportedMessageClassException {
        if (message instanceof DeviceRequestMessage) {
            return GatewayMessageType.DEVICE_REQUEST;
        }
        if (message instanceof GatewayCommand) {
            return GatewayMessageType.GATEWAY_COMMAND;
        }

        throw new UnsupportedMessageClassException(message.getClass());
    }

    private static String buildConsumerTag(GatewayEntity entity) {
        return "consumer_" + entity.getId();
    }
}
