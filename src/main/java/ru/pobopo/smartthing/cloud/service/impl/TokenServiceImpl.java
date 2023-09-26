package ru.pobopo.smartthing.cloud.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.naming.AuthenticationException;
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
    private final GatewayRepository gatewayRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Autowired
    public TokenServiceImpl(
        TokenInfoRepository tokenRepository,
        UserDetailsService userDetailsService,
        GatewayRepository gatewayRepository,
        JwtTokenUtil jwtTokenUtil
    ) {
        this.tokenRepository = tokenRepository;
        this.userDetailsService = userDetailsService;
        this.gatewayRepository = gatewayRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    @Transactional
    public String generateToken(UserEntity user) throws AuthenticationException, AccessDeniedException {
        deactivateOldToken(user);

        TokenInfoEntity tokenInfoEntity = buildDefaultToken(user);
        tokenInfoEntity.setType(TokenType.USER.getName());
        tokenRepository.save(tokenInfoEntity);

        return generateToken(tokenInfoEntity);
    }

    @Override
    @Transactional
    public String generateToken(UserEntity user, GatewayEntity gatewayEntity)
        throws AuthenticationException, AccessDeniedException {
        TokenInfoEntity tokenInfoEntity = buildDefaultToken(user);
        deactivateOldToken(gatewayEntity);

        tokenInfoEntity.setGateway(gatewayEntity);
        tokenInfoEntity.setType(TokenType.GATEWAY.getName());

        tokenRepository.save(tokenInfoEntity);
        return generateToken(tokenInfoEntity);
    }

    //todo add cache
    @Override
    public UserDetails validateToken(String token) throws AccessDeniedException, AuthenticationException {
        if (StringUtils.isBlank(token)) {
            throw new AccessDeniedException("Token is missing!");
        }

        TokenType tokenType = TokenType.fromString(
            jwtTokenUtil.getClaimFromToken(token, claims -> (String) claims.get(TYPE_CLAIM))
        );
        if (tokenType == null) {
            throw new AccessDeniedException("Unknown token type!");
        }

        String tokenId = jwtTokenUtil.getTokenSubject(token);
        TokenInfoEntity tokenInfoEntity = tokenRepository.findByIdAndType(tokenId, tokenType.getName());
        if (tokenInfoEntity == null || !tokenInfoEntity.isActive()) {
            throw new AccessDeniedException("Unknown token");
        }

        if (jwtTokenUtil.isTokenExpired(token)) {
            deactivateToken(tokenInfoEntity);
            throw new AccessDeniedException("Token expired!");
        }

        String ownerLogin = jwtTokenUtil.getClaimFromToken(token, claims -> (String) claims.get(OWNER_CLAIM));
        UserDetails user = userDetailsService.loadUserByUsername(ownerLogin);

        if (TokenType.GATEWAY.equals(tokenType)) {
            String gatewayId = jwtTokenUtil.getClaimFromToken(token, claims -> (String) claims.get(GATEWAY_CLAIM));
            Optional<GatewayEntity> gateway = gatewayRepository.findById(gatewayId);
            if (gateway.isEmpty()) {
                throw new AccessDeniedException("Gateway not found");
            }
            ContextHolder.setCurrentGateway(gateway.get());
        }

        ContextHolder.setTokenType(tokenType);
        ContextHolder.setTokenId(tokenId);

        return user;
    }

    @Override
    public void deactivateToken(String tokenId) throws AccessDeniedException, AuthenticationException {
        if (StringUtils.isBlank(tokenId)) {
            return;
        }

        Optional<TokenInfoEntity> tokenInfoEntity = tokenRepository.findById(tokenId);
        if (tokenInfoEntity.isEmpty()) {
            throw new AccessDeniedException("Unknown token");
        }
        deactivateToken(tokenInfoEntity.get());
    }

    @Override
    public List<TokenInfoEntity> getActiveGatewayTokens(UserEntity owner) {
        return tokenRepository.findByActiveAndOwnerAndGatewayIsNotNull(true, owner);
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

    private void deactivateOldToken(UserEntity user) throws AuthenticationException, AccessDeniedException {
        deactivateToken(tokenRepository.findByActiveAndOwnerAndGatewayIsNull(true, user));
    }

    private void deactivateOldToken(GatewayEntity gateway) throws AuthenticationException, AccessDeniedException {
        deactivateToken(tokenRepository.findByActiveAndGateway(true, gateway));
    }

    private void deactivateToken(TokenInfoEntity oldToken) throws AuthenticationException, AccessDeniedException {
        if (oldToken == null) {
            return;
        }

        if (oldToken.getGateway() != null && !AuthoritiesUtil.canManageToken(oldToken)) {
            throw new AccessDeniedException("You are not the owner!");
        }

        oldToken.setActive(false);
        oldToken.setDeactivationDate(LocalDateTime.now());
        tokenRepository.save(oldToken);
        log.warn("Token {} was deactivated by {}", oldToken, AuthoritiesUtil.getCurrentUserLogin());
    }

    private TokenInfoEntity buildDefaultToken(UserEntity owner) {
        TokenInfoEntity ownerEntity = new TokenInfoEntity();
        ownerEntity.setOwner(owner);
        ownerEntity.setCreationDate(LocalDateTime.now());
        return ownerEntity;
    }
}
