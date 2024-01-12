package ru.pobopo.smartthing.cloud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.GatewayTokenEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.jwt.JwtTokenUtil;
import ru.pobopo.smartthing.cloud.model.AuthorizedUser;
import ru.pobopo.smartthing.cloud.model.TokenType;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.cloud.repository.GatewayTokenRepository;
import ru.pobopo.smartthing.cloud.repository.UserRepository;

import javax.naming.AuthenticationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayAuthService {
    private final GatewayRepository gatewayRepository;
    private final GatewayTokenRepository gatewayTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public String generateToken(String gatewayId) throws AccessDeniedException, ValidationException, AuthenticationException {
        GatewayEntity gateway = getGatewayWithValidation(gatewayId);
        Optional<GatewayTokenEntity> tokenEntity = gatewayTokenRepository.findByGateway(gateway);
        if(tokenEntity.isPresent()) {
            throw new ValidationException("Gateway already have active token!");
        }

        AuthorizedUser user = (AuthorizedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AuthorizedUser authorizedUser = AuthorizedUser.build(
                TokenType.GATEWAY,
                user.getUser(),
                List.of(),
                gateway
        );

        String token = jwtTokenUtil.doGenerateToken(
                authorizedUser.getTokenType().getName(),
                authorizedUser.toClaims(),
                -1
        );
        saveToken(authorizedUser, token);
        log.info(
                "Authorized user [{}] generated token for gateway [{}]",
                authorizedUser,
                gatewayId
        );
        return token;
    }

    public void validate(AuthorizedUser authorizedUser) throws AccessDeniedException {
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

    public void deleteToken(String gatewayId) throws AccessDeniedException, ValidationException, AuthenticationException {
        GatewayEntity gateway = getGatewayWithValidation(gatewayId);
        Optional<GatewayTokenEntity> tokenEntity = gatewayTokenRepository.findByGateway(gateway);
        if (tokenEntity.isEmpty()) {
            throw new ValidationException("There is not token for gateway " + gatewayId);
        }
        gatewayTokenRepository.delete(tokenEntity.get());
    }

    private void saveToken(AuthorizedUser authorizedUser, String token) {
        GatewayTokenEntity gatewayToken = new GatewayTokenEntity();
        gatewayToken.setToken(token);
        gatewayToken.setOwner(authorizedUser.getUser());
        gatewayToken.setGateway(authorizedUser.getGateway());
        gatewayToken.setCreationDate(LocalDateTime.now());
        gatewayTokenRepository.save(gatewayToken);
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
}
