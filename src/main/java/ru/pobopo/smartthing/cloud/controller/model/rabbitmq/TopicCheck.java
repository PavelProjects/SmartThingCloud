package ru.pobopo.smartthing.cloud.controller.model.rabbitmq;

import lombok.Data;

@Data
public class TopicCheck extends ResourceCheck {
    private String routing_key;
}
