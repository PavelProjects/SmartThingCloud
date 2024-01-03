package ru.pobopo.smartthing.cloud.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.jwt.JwtTokenUtil;
import ru.pobopo.smartthing.cloud.model.AuthorizedUser;
import ru.pobopo.smartthing.cloud.model.TokenType;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.cloud.repository.UserRepository;

import javax.naming.AuthenticationException;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class AuthService {
    public static final String USER_COOKIE_NAME = "SMTJwt";

    private final UserRepository userRepository;
    private final GatewayRepository gatewayRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final JedisPool jedisPool;
    private final GatewayBrokerService gatewayBrokerService;

    @Value("${jwt.token.ttl}")
    private long tokenTimeToLive;

    @Autowired
    public AuthService(
            UserRepository userRepository,
            GatewayRepository gatewayRepository,
            JwtTokenUtil jwtTokenUtil,
            JedisPool jedisPool,
            GatewayBrokerService gatewayBrokerService) {
        this.userRepository = userRepository;
        this.gatewayRepository = gatewayRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.jedisPool = jedisPool;
        this.gatewayBrokerService = gatewayBrokerService;
    }

    public AuthorizedUser authorizeUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UserEntity user = userRepository.findByLogin(userDetails.getUsername());
        return AuthorizedUser.build(TokenType.USER, user, userDetails.getAuthorities());
    }

    public String getUserToken(AuthorizedUser authorizedUser) {
        return generateTokenIfNeedTo(authorizedUser, tokenTimeToLive);
    }

    public ResponseCookie getUserCookie(AuthorizedUser authorizedUser) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(
                        USER_COOKIE_NAME,
                        getUserToken(authorizedUser)
                )
                .path("/")
                .maxAge(tokenTimeToLive)
                .httpOnly(true);
        return builder.build();
    }

    public String getGatewayToken(String gatewayId, int days)
            throws ValidationException, AuthenticationException, AccessDeniedException {
        GatewayEntity gateway = getGatewayWithValidation(gatewayId);

        AuthorizedUser user = (AuthorizedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AuthorizedUser authorizedUser = AuthorizedUser.build(
                TokenType.GATEWAY,
                user.getUser(),
                List.of(),
                gateway
        );
        long ttl = (long) days * 24 * 3600;
        String token = generateTokenIfNeedTo(authorizedUser, ttl);
        log.info(
                "Authorized user [{}] generated token for gateway [{}] for {} days",
                authorizedUser,
                gatewayId,
                days > 0 ? days : "[inf]"
        );

        try {
            gatewayBrokerService.addResponseListener(gateway);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to add queue response listener", exception);
        }

        return token;
    }

    public AuthorizedUser validateToken(String token) throws AccessDeniedException {
        if (StringUtils.isBlank(token)) {
            throw new AccessDeniedException("Token is missing!");
        }
        if (jwtTokenUtil.isTokenExpired(token)) {
            throw new AccessDeniedException("Token expired!");
        }
        AuthorizedUser authorizedUser = AuthorizedUser.fromClaims(jwtTokenUtil.getAllClaimsFromToken(token));

        String cachedToken = getTokenFromRedis(authorizedUser);
        if (!StringUtils.equals(token, cachedToken)) {
            log.debug("Tokens not equals (given ::: cached): {} ::: {}", token, cachedToken);
            throw new AccessDeniedException("Not valid token");
        }

        checkExistence(authorizedUser);
        log.debug("Authorized user: {}", authorizedUser);
        return authorizedUser;
    }

    public void logout() throws AuthenticationException {
        removeTokenFromRedis(AuthorisationUtils.getAuthorizedUser());
    }

    public void logoutGateway(String gatewayId)
            throws ValidationException, AuthenticationException, AccessDeniedException {
        GatewayEntity gateway = getGatewayWithValidation(gatewayId);
        AuthorizedUser authorizedUser = AuthorisationUtils.getAuthorizedUser();
        removeTokenFromRedis(authorizedUser.getUser(), gateway);

        try {
            log.warn("Trying to publish gateway logout event");
            gatewayBrokerService.gatewayLogout(gateway);
        } catch (Exception e) {
            log.error("Failed to publish gateway login event", e);
        }
    }

    private String generateTokenIfNeedTo(AuthorizedUser authorizedUser, long ttl) {
        String token = getTokenFromRedis(authorizedUser);
        if (StringUtils.isNotBlank(token)) {
            log.debug("Got active token from redis for authorized user [{}], reusing", authorizedUser);
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
            if (ttl > 0) {
                jedis.setex(key, ttl, token);
                log.info("Saved authorization {} in redis with key [{}] (ttl {})", authorizedUser, key, tokenTimeToLive);
            } else {
                jedis.set(key, token);
                log.info("Saved authorization {} in redis with key [{}] (no ttl)", authorizedUser, key);
            }
        }
    }

    private String getTokenFromRedis(AuthorizedUser authorizedUser) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = buildRedisKey(authorizedUser.getUser(), authorizedUser.getGateway());
            log.debug("Trying to get token from redis for authorized user [{}] by key {}", authorizedUser, key);
            String token = jedis.get(key);
            if (StringUtils.isBlank(token)) {
                log.debug("Token not found");
            } else {
                log.debug("Got token from cache");
            }
            return token;
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
