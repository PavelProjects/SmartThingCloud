package ru.pobopo.smartthing.cloud.service;

import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.model.AuthorizedUser;

import javax.naming.AuthenticationException;

@Component
public interface AuthService {
    AuthorizedUser authorizeUser(Authentication authentication);

    String createUserToken(AuthorizedUser authorizedUser);
    ResponseCookie getUserCookie(AuthorizedUser authorizedUser);

    String createGatewayToken(String gatewayId, int days) throws ValidationException, AuthenticationException, AccessDeniedException;

    AuthorizedUser validateToken(String token) throws AccessDeniedException;

    void logout() throws AuthenticationException;
    void logoutGateway(String gatewayId) throws ValidationException, AuthenticationException, AccessDeniedException;
}
