package ru.pobopo.smartthing.cloud.service;

import java.util.List;
import javax.naming.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.TokenInfoEntity;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;

public interface TokenService {
    String generateToken(UserEntity user) throws AuthenticationException, AccessDeniedException;
    String generateToken(UserEntity user, GatewayEntity gatewayEntity) throws AuthenticationException, AccessDeniedException;

    UserDetails validateToken(String token) throws AccessDeniedException, AuthenticationException;
    boolean isAuthorized(GatewayEntity gateway);

    void deactivateToken(String tokenId) throws AccessDeniedException, AuthenticationException;

    List<TokenInfoEntity> getActiveGatewayTokens(UserEntity owner);
}
