package ru.pobopo.smartthing.cloud.service.impl;

import java.util.Optional;
import javax.naming.AuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.event.GatewayLoginEvent;
import ru.pobopo.smartthing.cloud.event.GatewayLogoutEvent;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.jwt.JwtTokenUtil;
import ru.pobopo.smartthing.cloud.model.TokenType;
import ru.pobopo.smartthing.cloud.model.AuthorizedUser;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.cloud.repository.UserRepository;
import ru.pobopo.smartthing.cloud.service.AuthService;

@Component
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final GatewayRepository gatewayRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final JedisPool jedisPool;

    @Value("${jwt.token.ttl}")
    private long tokenTimeToLive;

    @Autowired
    public AuthServiceImpl(
        UserRepository userRepository,
        GatewayRepository gatewayRepository,
        JwtTokenUtil jwtTokenUtil,
        ApplicationEventPublisher applicationEventPublisher,
        JedisPool jedisPool
    ) {
        this.userRepository = userRepository;
        this.gatewayRepository = gatewayRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.applicationEventPublisher = applicationEventPublisher;
        this.jedisPool = jedisPool;
    }

    @Override
    public String authUser(Authentication auth) {
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        UserEntity user = userRepository.findByLogin(userDetails.getUsername());
        AuthorizedUser authorizedUser = AuthorizedUser.build(TokenType.USER, user, userDetails.getAuthorities());
        return generateTokenIfNeedTo(authorizedUser, tokenTimeToLive);
    }

    @Override
    public String authGateway(String gatewayId, int days)
        throws ValidationException, AuthenticationException, AccessDeniedException {
        GatewayEntity gateway = getGatewayWithValidation(gatewayId);

        AuthorizedUser user = (AuthorizedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AuthorizedUser authorizedUser = AuthorizedUser.build(
            TokenType.GATEWAY,
            user.getUser(),
            user.getAuthorities(),
            gateway
        );
        long ttl = (long) days * 24 * 3600;
        String token = generateTokenIfNeedTo(authorizedUser, ttl);
        log.info("Authorized user [{}] generated token for gateway [{}] for {} days", authorizedUser, gatewayId, days);

        try {
            log.warn("Trying to publish gateway login event");
            applicationEventPublisher.publishEvent(new GatewayLoginEvent(this, gateway));
        } catch (Exception e) {
            log.error("Failed to publish gateway login event", e);
        }

        return token;
    }

    @Override
    public AuthorizedUser validateToken(String token) throws AccessDeniedException {
        if (StringUtils.isBlank(token)) {
            throw new AccessDeniedException("Token is missing!");
        }
        if (jwtTokenUtil.isTokenExpired(token)) {
            throw new AccessDeniedException("Token expired!");
        }
        AuthorizedUser authorizedUser = AuthorizedUser.fromClaims(jwtTokenUtil.getAllClaimsFromToken(token));
        checkExistence(authorizedUser);
        log.debug("Authorized user: {}", authorizedUser);
        return authorizedUser;
    }

    @Override
    public void logout() throws AuthenticationException {
        removeTokenFromRedis(AuthorisationUtils.getAuthorizedUser());
    }

    @Override
    public void logoutGateway(String gatewayId)
        throws ValidationException, AuthenticationException, AccessDeniedException {
        GatewayEntity gateway = getGatewayWithValidation(gatewayId);
        AuthorizedUser authorizedUser = AuthorisationUtils.getAuthorizedUser();
        removeTokenFromRedis(authorizedUser.getUser(), gateway);

        try {
            log.warn("Trying to publish gateway logout event");
            applicationEventPublisher.publishEvent(new GatewayLogoutEvent(this, gateway));
        } catch (Exception e) {
            log.error("Failed to publish gateway login event", e);
        }
    }

    private String generateTokenIfNeedTo(AuthorizedUser authorizedUser, long ttl) {
        String token = getTokenFromRedis(authorizedUser);
        if (StringUtils.isNotBlank(token)) {
            log.info("Got active token from redis for authorized user [{}], reusing", authorizedUser);
            return token;
        }

        log.info("Generating new token for authorized user [{}]", authorizedUser);
        token = jwtTokenUtil.doGenerateToken(
            authorizedUser.getTokenType().getName(),
            authorizedUser.toClaims(),
            ttl
        );
        saveTokenInRedis(authorizedUser, token, ttl);
        return token;
    }

    private void saveTokenInRedis(AuthorizedUser authorizedUser, String token, long ttl) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = buildRedisKey(authorizedUser.getUser(), authorizedUser.getGateway());
            jedis.setex(key, ttl, token);
            log.info("Saved authorization {} in redis with key [{}] (ttl {})", authorizedUser, key, tokenTimeToLive);
        }
    }

    private String getTokenFromRedis(AuthorizedUser authorizedUser) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = buildRedisKey(authorizedUser.getUser(), authorizedUser.getGateway());
            log.info("Trying to get token from redis for authorized user [{}] by key {}", authorizedUser, key);
            return jedis.get(key);
        }
    }

    private void removeTokenFromRedis(AuthorizedUser authorizedUser) {
        removeTokenFromRedis(authorizedUser.getUser(), authorizedUser.getGateway());
    }

    private void removeTokenFromRedis(UserEntity user, GatewayEntity gateway) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = buildRedisKey(user, gateway);
            log.info("User [{}] removed token from redis by key [{}]", user, key);
            jedis.del(key);
        }
    }

    private GatewayEntity getGatewayWithValidation(String gatewayId)
        throws ValidationException, AccessDeniedException, AuthenticationException {
        if (StringUtils.isBlank(gatewayId)) {
            throw new ValidationException("Gateway id is missing!");
        }

        Optional<GatewayEntity> gatewayEntity = gatewayRepository.findById(gatewayId);
        if (gatewayEntity.isEmpty()) {
            throw new ValidationException("Gateway not found!");
        }

        if (!AuthorisationUtils.canManageGateway(gatewayEntity.get())) {
            throw new AccessDeniedException("Current user can't manage gateway " + gatewayId);
        }
        return gatewayEntity.get();
    }


    private void checkExistence(AuthorizedUser authorizedUser) throws AccessDeniedException {
        if (StringUtils.isBlank(getTokenFromRedis(authorizedUser))) {
            throw new AccessDeniedException("Not valid token");
        }

        // is it really necessary?
        if (authorizedUser.getUser() == null) {
            log.error("Missing user in token");
            throw new AccessDeniedException();
        }
        if (!userRepository.existsById(authorizedUser.getUser().getId())) {
            throw new AccessDeniedException("User not found");
        }

        if (authorizedUser.getTokenType() == TokenType.GATEWAY) {
            if (authorizedUser.getGateway() == null) {
                throw new AccessDeniedException();
            }

            if (!gatewayRepository.existsById(authorizedUser.getGateway().getId())) {
                throw new AccessDeniedException("Gateway not found");
            }
        }
    }

    private String buildRedisKey(UserEntity user, GatewayEntity gateway) {
        StringBuilder builder = new StringBuilder();
        builder
            .append("usr_")
            .append(user.getId())
            .append("_")
            .append(user.getLogin());

        if (gateway != null) {
            builder.append("_gtw_").append(gateway.getId());
        }

        return builder.toString();
    }
}
