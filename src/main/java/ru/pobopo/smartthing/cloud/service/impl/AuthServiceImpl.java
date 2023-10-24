package ru.pobopo.smartthing.cloud.service.impl;

import java.util.Optional;
import javax.naming.AuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.event.GatewayLoginEvent;
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

    @Autowired
    public AuthServiceImpl(
        UserRepository userRepository,
        GatewayRepository gatewayRepository,
        JwtTokenUtil jwtTokenUtil,
        ApplicationEventPublisher applicationEventPublisher
    ) {
        this.userRepository = userRepository;
        this.gatewayRepository = gatewayRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public String authUser(Authentication auth) {
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        UserEntity user = userRepository.findByLogin(userDetails.getUsername());
        AuthorizedUser authorizedUser = AuthorizedUser.build(TokenType.USER, user, userDetails.getAuthorities());
        return generateToken(authorizedUser);
    }

    @Override
    public String authGateway(String gatewayId)
        throws ValidationException, AuthenticationException, AccessDeniedException {
        if (StringUtils.isBlank(gatewayId)) {
            throw new ValidationException("Token id is missing!");
        }

        Optional<GatewayEntity> gatewayEntity = gatewayRepository.findById(gatewayId);
        if (gatewayEntity.isEmpty()) {
            throw new ValidationException("Gateway not found!");
        }

        if (!AuthoritiesUtil.canManageGateway(gatewayEntity.get())) {
            throw new AccessDeniedException("Current user can't manage gateway " + gatewayId);
        }

        AuthorizedUser user = (AuthorizedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AuthorizedUser authorizedUser = AuthorizedUser.build(
            TokenType.GATEWAY,
            user.getUser(),
            user.getAuthorities(),
            gatewayEntity.get()
        );
        String token = generateToken(authorizedUser);

        log.warn("Sending gateway login event");
        applicationEventPublisher.publishEvent(new GatewayLoginEvent(this, gatewayEntity.get()));

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
        return authorizedUser;
    }

    private void checkExistence(AuthorizedUser authorizedUser) throws AccessDeniedException {
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

    private String generateToken(AuthorizedUser authorizedUser) {
        return jwtTokenUtil.doGenerateToken(
            authorizedUser.getTokenType().getName(),
            authorizedUser.toClaims()
        );
    }
}
