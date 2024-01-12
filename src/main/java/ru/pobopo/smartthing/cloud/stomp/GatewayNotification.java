package ru.pobopo.smartthing.cloud.stomp;

import lombok.Data;
import ru.pobopo.smartthing.cloud.dto.GatewayDto;
import ru.pobopo.smartthing.cloud.model.DeviceInfo;
import ru.pobopo.smartthing.cloud.model.Notification;

@Data
public class GatewayNotification {
    private GatewayDto gateway;
    private DeviceInfo device;
    private Notification notification;
}
