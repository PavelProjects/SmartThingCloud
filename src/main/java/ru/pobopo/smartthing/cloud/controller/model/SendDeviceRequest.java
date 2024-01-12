package ru.pobopo.smartthing.cloud.controller.model;

import lombok.Data;
import ru.pobopo.smartthing.cloud.model.stomp.DeviceRequestMessage;

import java.util.Map;

@Data
public class SendDeviceRequest {
    private String gatewayId;
    private String target;
    private String path;
    private String method;
    private String payload;
    private Map<String, String> headers;

    public DeviceRequestMessage toDeviceRequest() {
        DeviceRequestMessage requestMessage = new DeviceRequestMessage();
        requestMessage.setPath(path);
        requestMessage.setTarget(target);
        requestMessage.setMethod(method);
        requestMessage.setPayload(payload);
        requestMessage.setHeaders(headers);
        return requestMessage;
    }
}
