package ru.pobopo.smartthing.cloud.controller.model;

import lombok.Data;
import ru.pobopo.smartthing.model.stomp.DeviceRequest;

@Data
public class SendDeviceRequest {
    private String gatewayId;
    private DeviceRequest request;
}
