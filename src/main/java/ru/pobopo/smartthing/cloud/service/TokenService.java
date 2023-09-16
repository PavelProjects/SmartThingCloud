package ru.pobopo.smartthing.cloud.service;

import org.springframework.security.core.userdetails.UserDetails;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;

public interface TokenService {
    String generateToken(String login) throws ValidationException;
    String generateToken(String login, String gatewayId) throws ValidationException;

    UserDetails validateToken(String token) throws AccessDeniedException;

    void deactivateToken(String token);
}
