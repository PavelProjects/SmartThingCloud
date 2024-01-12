package ru.pobopo.smartthing.cloud.stomp;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString(callSuper = true)
public class DeviceRequestMessage extends BaseMessage {
    private String target;
    private String path;
    private String method;
    private String payload;
    private Map<String, String> headers;

    public DeviceRequestMessage() {
        super(GatewayMessageType.DEVICE_REQUEST);
    }

    public DeviceRequestMessage(String target, String path, String method, String payload, Map<String, String> headers) {
        super(GatewayMessageType.DEVICE_REQUEST);
        this.target = target;
        this.path = path;
        this.method = method;
        this.payload = payload;
        this.headers = headers;
    }
}
