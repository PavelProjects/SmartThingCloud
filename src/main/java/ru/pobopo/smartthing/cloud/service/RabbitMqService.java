package ru.pobopo.smartthing.cloud.service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.UnsupportedMessageClassException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.rabbitmq.BaseMessage;
import ru.pobopo.smartthing.cloud.rabbitmq.MessageResponse;

public interface RabbitMqService {
    void createQueues(GatewayEntity entity) throws IOException, TimeoutException;
    void addQueueListener(GatewayEntity entity, Consumer<MessageResponse> consumer) throws IOException;
    <T extends BaseMessage> String send(GatewayEntity entity, T message)
        throws IOException, TimeoutException, UnsupportedMessageClassException, ValidationException;
}
