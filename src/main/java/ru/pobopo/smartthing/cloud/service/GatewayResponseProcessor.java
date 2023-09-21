package ru.pobopo.smartthing.cloud.service;

import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.rabbitmq.MessageResponse;

@Component
public interface GatewayResponseProcessor {
    void process(MessageResponse response);
}
