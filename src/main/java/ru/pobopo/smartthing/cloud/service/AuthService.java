package ru.pobopo.smartthing.cloud.service;

import javax.naming.AuthenticationException;
import javax.servlet.http.Cookie;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.model.AuthorizedUser;

@Component
public interface AuthService {
    AuthorizedUser authorizeUser(Authentication authentication);

    String getUserToken(AuthorizedUser authorizedUser);
    Cookie getUserCookie(AuthorizedUser authorizedUser);

    String getGatewayToken(String gatewayId, int days) throws ValidationException, AuthenticationException, AccessDeniedException;

    AuthorizedUser validateToken(String token) throws AccessDeniedException;

    void logout() throws AuthenticationException;
    void logoutGateway(String gatewayId) throws ValidationException, AuthenticationException, AccessDeniedException;
}
