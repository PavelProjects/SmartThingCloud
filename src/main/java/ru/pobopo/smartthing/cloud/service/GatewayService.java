package ru.pobopo.smartthing.cloud.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import javax.naming.AuthenticationException;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.dto.GatewayQueueInfo;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.ValidationException;

@Component
public interface GatewayService {
    Optional<GatewayEntity> getGateway(String id);

    GatewayEntity findUserGateway(String name) throws AuthenticationException;
    List<GatewayEntity> getUserGateways() throws AuthenticationException;
    GatewayEntity createGateway(String name, String description) throws AuthenticationException, ValidationException;

    GatewayQueueInfo getQueueInfo() throws ValidationException, IOException, TimeoutException;
}
