package ru.pobopo.smartthing.cloud.rabbitmq;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class BaseMessage {
    private String requestId;
}
