package ru.pobopo.smartthing.cloud.model.stomp;

import lombok.Data;
import lombok.ToString;
import ru.pobopo.smartthing.cloud.model.DeviceInfo;

import java.util.HashMap;
import java.util.Map;

@Data
@ToString
public class DeviceRequest {
    private DeviceInfo device;
    private String command;
    private Map<String, Object> params = new HashMap<>();
}
