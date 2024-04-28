package ru.pobopo.smartthing.cloud.controller.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.pobopo.smartthing.model.stomp.GatewayCommandMessage;

@EqualsAndHashCode(callSuper = true)
@Data
public class SendCommandRequest extends GatewayCommandMessage {
    private String gatewayId;
}
