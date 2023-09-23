package ru.pobopo.smartthing.cloud.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import javax.naming.AuthenticationException;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.GatewayRequestEntity;
import ru.pobopo.smartthing.cloud.entity.RequestTemplateEntity;
import ru.pobopo.smartthing.cloud.rabbitmq.BaseMessage;

@Component
public interface GatewayMessagingService {
    List<RequestTemplateEntity> getRequestTemplates();
    List<GatewayRequestEntity> getUserRequests(int page, int size) throws AuthenticationException;
    GatewayRequestEntity getUserRequestById(String id) throws AuthenticationException;

    <T extends BaseMessage> GatewayRequestEntity sendMessage(String gatewayId, T message) throws Exception;
    <T extends BaseMessage> GatewayRequestEntity sendMessage(GatewayEntity gateway, T message) throws Exception;

    void addResponseListeners() throws IOException, TimeoutException;
    void addResponseListener(GatewayEntity entity) throws IOException, TimeoutException;
}
