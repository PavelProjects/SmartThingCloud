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
import ru.pobopo.smartthing.cloud.model.AuthenticatedUser;
import ru.pobopo.smartthing.cloud.model.TokenType;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.cloud.repository.GatewayTokenRepository;
import ru.pobopo.smartthing.cloud.repository.UserRepository;
import ru.pobopo.smartthing.model.stomp.GatewayCommand;
import ru.pobopo.smartthing.model.stomp.GatewayCommandMessage;

import javax.naming.AuthenticationException;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayAuthService {
    private final GatewayRepository gatewayRepository;
    private final GatewayTokenRepository gatewayTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final GatewayRequestService requestService;

    public String generateToken(String gatewayId) throws AccessDeniedException, ValidationException, AuthenticationException {
        GatewayEntity gateway = getGatewayWithValidation(gatewayId);
        Optional<GatewayTokenEntity> tokenEntity = gatewayTokenRepository.findByGateway(gateway);
        if(tokenEntity.isPresent()) {
            throw new ValidationException("Gateway already have active token!");
        }

        AuthenticatedUser user = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AuthenticatedUser authenticatedUser = AuthenticatedUser.build(
                TokenType.GATEWAY,
                user.getUser(),
                List.of(),
                gateway
        );

        String token = jwtTokenUtil.doGenerateToken(
                authenticatedUser.getTokenType().getName(),
                authenticatedUser.toClaims(),
                -1
        );
        saveToken(authenticatedUser, token);
        log.info(
                "Authorized user [{}] generated token for gateway [{}]",
                authenticatedUser,
                gatewayId
        );
        return token;
    }

    public void validate(AuthenticatedUser authenticatedUser, String token) throws AccessDeniedException {
        if (authenticatedUser.getUser() == null) {
            log.error("Missing user in token");
            throw new AccessDeniedException();
        }
        if (!userRepository.existsById(authenticatedUser.getUser().getId())) {
            throw new AccessDeniedException("User not found");
        }

        if (authenticatedUser.getGateway() == null) {
            throw new AccessDeniedException();
        }

        Optional<GatewayTokenEntity> tokenEntity = gatewayTokenRepository.findByGateway(authenticatedUser.getGateway());
        if (tokenEntity.isEmpty()) {
            throw new AccessDeniedException();
        }
        if (!tokenEntity.get().getToken().equals(token)) {
            throw new AccessDeniedException("Wrong token!");
        }
    }

    @Transactional
    public void logout(String gatewayId) throws Exception {
        GatewayEntity gateway = getGatewayWithValidation(gatewayId);
        Optional<GatewayTokenEntity> tokenEntity = gatewayTokenRepository.findByGateway(gateway);
        if (tokenEntity.isEmpty()) {
            throw new ValidationException("There is not token for gateway " + gatewayId);
        }
        gatewayTokenRepository.delete(tokenEntity.get());

        AuthenticatedUser user = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (user.getTokenType().equals(TokenType.USER)) {
            GatewayCommandMessage message = new GatewayCommandMessage(GatewayCommand.LOGOUT.getName(), null);
            requestService.sendMessage(gatewayId, message);
        }
    }

    public Map<String, GatewayTokenEntity> getTokens(List<GatewayEntity> gateways) {
        if (gateways == null || gateways.isEmpty()) {
            return Map.of();
        }

        List<GatewayTokenEntity> tokens = gatewayTokenRepository.findByGatewayIn(gateways);
        return tokens.stream().collect(Collectors.toMap(((tok) -> tok.getGateway().getId()), ((tok) -> tok)));
    }

    public GatewayTokenEntity getToken(GatewayEntity gateway) {
        return gatewayTokenRepository.findByGateway(gateway).orElse(null);
    }

    private void saveToken(AuthenticatedUser authenticatedUser, String token) {
        GatewayTokenEntity gatewayToken = new GatewayTokenEntity();
        gatewayToken.setToken(token);
        gatewayToken.setOwner(authenticatedUser.getUser());
        gatewayToken.setGateway(authenticatedUser.getGateway());
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
