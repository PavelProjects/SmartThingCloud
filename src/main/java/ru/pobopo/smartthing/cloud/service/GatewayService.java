package ru.pobopo.smartthing.cloud.service;

import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.dto.GatewayShortDto;
import ru.pobopo.smartthing.cloud.entity.GatewayConfigEntity;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;

import javax.naming.AuthenticationException;
import java.io.IOException;
import java.util.List;

@Component
public interface GatewayService {
    GatewayEntity getGateway(String id);
    List<GatewayEntity> getUserGateways() throws AuthenticationException, InterruptedException;

    GatewayEntity getUserGatewayByName(String name) throws AuthenticationException;

    GatewayEntity createGateway(String name, String description) throws AuthenticationException, ValidationException, IOException;
    void updateGateway(GatewayShortDto gatewayShortDto) throws ValidationException, AuthenticationException, AccessDeniedException;
    void deleteGateway(String id)
        throws AccessDeniedException, ValidationException, AuthenticationException, IOException;

    GatewayConfigEntity getConfig(String gatewayId);
}
