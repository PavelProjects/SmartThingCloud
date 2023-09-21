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
public class DeviceRequestMessage extends BaseMessage {
    private String target;
    private String path;
    private String method;
    private String payload;
    private Map<String, String> headers;
}
