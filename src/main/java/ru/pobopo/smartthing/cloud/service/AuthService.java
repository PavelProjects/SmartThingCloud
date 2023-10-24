package ru.pobopo.smartthing.cloud.service;

import javax.naming.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.model.AuthorizedUser;

@Component
public interface AuthService {
    String authUser(Authentication auth);
    String authGateway(String gatewayId) throws ValidationException, AuthenticationException, AccessDeniedException;
    AuthorizedUser validateToken(String token) throws AccessDeniedException;
}
