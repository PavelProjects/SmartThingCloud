package ru.pobopo.smartthing.cloud.service;

import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.controller.model.rabbitmq.BasicCheck;
import ru.pobopo.smartthing.cloud.controller.model.rabbitmq.ResourceCheck;
import ru.pobopo.smartthing.cloud.controller.model.rabbitmq.TopicCheck;

@Component
public interface RabbitAuthService {
    String authUser(String username, String password);
    String authVhost(BasicCheck check);
    String authResource(ResourceCheck check);
    String authTopic(TopicCheck check);
}
