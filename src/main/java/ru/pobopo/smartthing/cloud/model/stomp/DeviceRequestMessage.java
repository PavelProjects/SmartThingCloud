package ru.pobopo.smartthing.cloud.model.stomp;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class DeviceRequestMessage extends BaseMessage {
    private DeviceRequest request;

    public DeviceRequestMessage() {
        super(GatewayMessageType.DEVICE_REQUEST);
    }
    public DeviceRequestMessage(DeviceRequest request) {
        super(GatewayMessageType.DEVICE_REQUEST);
        this.request = request;
    }
}
