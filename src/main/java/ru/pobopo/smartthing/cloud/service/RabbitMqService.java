package ru.pobopo.smartthing.cloud.service;

import ru.pobopo.smartthing.cloud.entity.GatewayConfigEntity;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.UnsupportedMessageClassException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.rabbitmq.BaseMessage;
import ru.pobopo.smartthing.cloud.rabbitmq.MessageResponse;

import java.io.IOException;
import java.util.function.Consumer;

public interface RabbitMqService {
    String getBrokeHost();
    int getBrokePort();

    void createQueues(GatewayConfigEntity config) throws IOException;
    void deleteQueues(GatewayConfigEntity config) throws IOException;

    void addQueueListener(GatewayEntity entity, Consumer<MessageResponse> consumer) throws IOException;
    void removeQueueListener(GatewayEntity entity) throws IOException;

    <T extends BaseMessage> String send(GatewayEntity entity, T message)
        throws IOException, UnsupportedMessageClassException, ValidationException;

    boolean isOnline(GatewayEntity gateway) throws IOException;
}
