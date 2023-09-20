package ru.pobopo.smartthing.cloud.service.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceRequestMessage extends BaseMessage {
    private String target;
    private String path;
    private String method;
    private String payload;
    private Map<String, String> headers;
}
