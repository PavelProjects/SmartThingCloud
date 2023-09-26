package ru.pobopo.smartthing.cloud.service;

import java.util.List;
import javax.naming.AuthenticationException;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;

@Component
public interface AuthService {
    String authUser(String login, String password)
        throws ValidationException, AuthenticationException, AccessDeniedException;
    String authGateway(String gatewayId) throws ValidationException, AuthenticationException, AccessDeniedException;

    void userLogout() throws ValidationException, AccessDeniedException, AuthenticationException;
    void gatewayLogout() throws ValidationException, AccessDeniedException, AuthenticationException;

    List<GatewayEntity> getUserAuthorizedGateways()
        throws ValidationException, AuthenticationException;
}
