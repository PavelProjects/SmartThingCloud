package ru.pobopo.smartthing.cloud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import javax.annotation.PreDestroy;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.dto.GatewayDto;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.UnsupportedMessageClassException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.rabbitmq.MessageConsumer;
import ru.pobopo.smartthing.cloud.rabbitmq.MessageResponse;
import ru.pobopo.smartthing.cloud.service.RabbitMqService;
import ru.pobopo.smartthing.cloud.rabbitmq.BaseMessage;
import ru.pobopo.smartthing.cloud.rabbitmq.DeviceRequestMessage;
import ru.pobopo.smartthing.cloud.rabbitmq.GatewayCommand;
import ru.pobopo.smartthing.cloud.rabbitmq.GatewayMessageType;

@Component
@Slf4j
public class RabbitMqServiceImpl implements RabbitMqService {
    private final ConnectionFactory connectionFactory;
    private Connection connection;
    private Channel channel;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> consumersTags = new HashSet<>();

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
    public void createQueues(GatewayEntity entity) throws IOException {
        channel.queueDeclare(entity.getQueueIn(), false, false, false, null);
        channel.queueDeclare(entity.getQueueOut(), false, false, false, null);
        log.info("Created queues for {}", entity);
    }

    @Override
    public void deleteQueues(GatewayEntity entity) throws IOException {
        channel.queueDelete(entity.getQueueIn());
        channel.queueDelete(entity.getQueueOut());
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
            entity.getQueueOut(),
            false,
            consumerTag,
            new MessageConsumer(channel, consumer)
        );
        consumersTags.add(consumerTag);
    }

    @Override
    public void removeQueueListener(GatewayEntity entity) throws IOException {
        String consumerTag = buildConsumerTag(entity);
        channel.basicCancel(consumerTag);
        consumersTags.remove(consumerTag);
    }

    @Override
    public <T extends BaseMessage> String send(GatewayEntity entity, T message)
        throws IOException, UnsupportedMessageClassException, ValidationException {
        Objects.requireNonNull(entity, "Gateway entity is missing!");
        String queue = entity.getQueueIn();

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
    public boolean isOnline(GatewayDto gatewayDto) throws IOException {
        return channel.consumerCount(gatewayDto.getQueueIn()) > 0;
    }

    @Override
    public void checkIsOnline(List<GatewayDto> gateways) throws InterruptedException {
        int size = gateways.size();
        if (size < 1) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(size);
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        gateways.forEach(entity ->
            executorService.submit(() -> {
                boolean online = false;
                try {
                    online = isOnline(entity);
                } catch (IOException e) {
                    log.error("Failed to check gateway {} availability: {}", entity, e.getMessage());
                }
                entity.setOnline(online);
                latch.countDown();
            })
        );
        latch.await();
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
