package ru.pobopo.smartthing.cloud.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pobopo.smartthing.cloud.context.ContextHolder;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.TokenInfoEntity;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.jwt.JwtTokenUtil;
import ru.pobopo.smartthing.cloud.jwt.TokenType;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.cloud.repository.TokenInfoRepository;
import ru.pobopo.smartthing.cloud.repository.UserRepository;
import ru.pobopo.smartthing.cloud.service.TokenService;

@Service
@Slf4j
public class TokenServiceImpl implements TokenService {
    private static final String TYPE_CLAIM = "type_id";
    private static final String OWNER_CLAIM = "owner_login";
    private static final String GATEWAY_CLAIM = "gateway_id";

    private final TokenInfoRepository tokenRepository;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final GatewayRepository gatewayRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Autowired
    public TokenServiceImpl(
        TokenInfoRepository tokenRepository,
        UserDetailsService userDetailsService,
        UserRepository userRepository,
        GatewayRepository gatewayRepository,
        JwtTokenUtil jwtTokenUtil
    ) {
        this.tokenRepository = tokenRepository;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.gatewayRepository = gatewayRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    @Transactional
    public String generateToken(String login) throws ValidationException {
        UserEntity user = getUserAndValidate(login);
        deactivateOldToken(user);

        TokenInfoEntity tokenInfoEntity = buildDefaultToken(user);
        tokenInfoEntity.setType(TokenType.USER.getName());
        tokenRepository.save(tokenInfoEntity);

        return generateToken(tokenInfoEntity);
    }

    @Override
    @Transactional
    public String generateToken(String login, String gatewayId) throws ValidationException {
        if (StringUtils.isBlank(gatewayId)) {
            throw new ValidationException("Token id is missing!");
        }

        UserEntity user = getUserAndValidate(login);
        TokenInfoEntity tokenInfoEntity = buildDefaultToken(user);
        Optional<GatewayEntity> gatewayEntity = gatewayRepository.findById(gatewayId);
        if (gatewayEntity.isEmpty()) {
            throw new ValidationException("Gateway not found!");
        }
        deactivateOldToken(gatewayEntity.get());

        tokenInfoEntity.setGateway(gatewayEntity.get());
        tokenInfoEntity.setType(TokenType.GATEWAY.getName());

        tokenRepository.save(tokenInfoEntity);
        return generateToken(tokenInfoEntity);
    }

    //todo add cache
    @Override
    public UserDetails validateToken(String token) throws AccessDeniedException {
        if (StringUtils.isBlank(token)) {
            throw new AccessDeniedException("Token is missing!");
        }

        String tokenId = jwtTokenUtil.getTokenSubject(token);
        String type = jwtTokenUtil.getClaimFromToken(token, claims -> (String) claims.get(TYPE_CLAIM));

        TokenInfoEntity tokenInfoEntity = tokenRepository.findByIdAndType(tokenId, type);
        if (tokenInfoEntity == null || !tokenInfoEntity.isActive()) {
            throw new AccessDeniedException("Bad token");
        }

        if (jwtTokenUtil.isTokenExpired(token)) {
            deactivateToken(tokenInfoEntity);
            throw new AccessDeniedException("Token expired!");
        }

        String ownerLogin = jwtTokenUtil.getClaimFromToken(token, claims -> (String) claims.get(OWNER_CLAIM));
        UserDetails user = userDetailsService.loadUserByUsername(ownerLogin);

        if (StringUtils.equals(type, TokenType.GATEWAY.getName())) {
            String gatewayId = jwtTokenUtil.getClaimFromToken(token, claims -> (String) claims.get(GATEWAY_CLAIM));
            Optional<GatewayEntity> gateway = gatewayRepository.findById(gatewayId);
            if (gateway.isEmpty()) {
                throw new AccessDeniedException("Gateway not found");
            }
            ContextHolder.setCurrentGateway(gateway.get());
        }

        return user;
    }

    @Override
    public void deactivateToken(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }

        String tokenId = jwtTokenUtil.getTokenSubject(token);
        if (StringUtils.isBlank(tokenId)) {
            return;
        }
        Optional<TokenInfoEntity> tokenInfoEntity = tokenRepository.findById(tokenId);
        if (tokenInfoEntity.isEmpty()) {
            return;
        }
        deactivateToken(tokenInfoEntity.get());
    }

    private String generateToken(TokenInfoEntity tokenInfoEntity) {
        HashMap<String, Object> claims = new HashMap<>();
        claims.put(TYPE_CLAIM, tokenInfoEntity.getType());
        claims.put(OWNER_CLAIM, tokenInfoEntity.getOwner().getLogin());
        if (tokenInfoEntity.getGateway() != null) {
            claims.put(GATEWAY_CLAIM, tokenInfoEntity.getGateway().getId());
        }
        return jwtTokenUtil.doGenerateToken(claims, tokenInfoEntity.getId());
    }

    private void deactivateOldToken(UserEntity user) {
        deactivateToken(tokenRepository.findByActiveAndOwnerAndGatewayIsNull(true, user));
    }

    private void deactivateOldToken(GatewayEntity gateway) {
        deactivateToken(tokenRepository.findByActiveAndGateway(true, gateway));
    }

    private void deactivateToken(TokenInfoEntity oldToken) {
        if (oldToken == null) {
            return;
        }

        oldToken.setActive(false);
        oldToken.setDeactivationDate(LocalDateTime.now());
        tokenRepository.save(oldToken);
        log.warn("Token {} was deactivated", oldToken);
    }

    private UserEntity getUserAndValidate(String login) throws ValidationException {
        if (StringUtils.isBlank(login)) {
            throw new ValidationException("User id is missing!");
        }
        UserEntity user = userRepository.findByLogin(login);
        if (user == null) {
            throw new ValidationException("User not found!");
        }
        return user;
    }

    private TokenInfoEntity buildDefaultToken(UserEntity owner) {
        TokenInfoEntity ownerEntity = new TokenInfoEntity();
        ownerEntity.setOwner(owner);
        ownerEntity.setCreationDate(LocalDateTime.now());
        return ownerEntity;
    }
}
