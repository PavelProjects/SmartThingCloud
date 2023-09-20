package ru.pobopo.smartthing.cloud.service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.service.model.DeviceRequestMessage;
import ru.pobopo.smartthing.cloud.service.model.GatewayCommand;

public interface MessageBrokerService {
    void createQueues(GatewayEntity entity) throws IOException, TimeoutException;
    void send(GatewayEntity entity, GatewayCommand gatewayCommand) throws IOException, TimeoutException ;
    void send(GatewayEntity entity, DeviceRequestMessage deviceRequestMessage) throws IOException, TimeoutException ;
}
