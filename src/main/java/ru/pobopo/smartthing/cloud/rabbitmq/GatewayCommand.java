package ru.pobopo.smartthing.cloud.rabbitmq;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class GatewayCommand extends BaseMessage {
    private String command;
    private Map<String, Object> parameters;
}
