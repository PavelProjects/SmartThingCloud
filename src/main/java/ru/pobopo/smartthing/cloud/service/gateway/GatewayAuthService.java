package ru.pobopo.smartthing.cloud.service.gateway;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
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
import ru.pobopo.smartthing.cloud.service.AuthorisationUtils;
import ru.pobopo.smartthing.model.stomp.GatewayCommand;
import ru.pobopo.smartthing.model.stomp.GatewayCommandMessage;
import ru.pobopo.smartthing.model.stomp.GatewayEventType;

import javax.naming.AuthenticationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayAuthService {
    private static final String CLAIM_TOKEN_ID = "token_id";

    private final GatewayRepository gatewayRepository;
    private final GatewayTokenRepository gatewayTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final GatewayRequestService requestService;
    private final SimpUserRegistry userRegistry;

    @Transactional
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

        GatewayTokenEntity gatewayToken = new GatewayTokenEntity();
        gatewayToken.setOwner(authenticatedUser.getUser());
        gatewayToken.setGateway(authenticatedUser.getGateway());
        gatewayToken.setCreationDate(LocalDateTime.now());
        gatewayTokenRepository.save(gatewayToken);

        Map<String, Object> claims = authenticatedUser.toClaims();
        claims.put(CLAIM_TOKEN_ID, gatewayToken.getId());

        String token = jwtTokenUtil.doGenerateToken(TokenType.GATEWAY.getName(), claims,-1);
        log.info(
                "Authorized user [{}] generated token for gateway [{}] (id={})",
                authenticatedUser,
                gatewayId,
                gatewayToken.getId()
        );
        return token;
    }

    public void validate(AuthenticatedUser authenticatedUser, HttpServletRequest request, Claims claims) throws AccessDeniedException {
        String tokenId = claims.get(CLAIM_TOKEN_ID, String.class);
        GatewayEntity gateway = authenticatedUser.getGateway();
        if (gateway == null || StringUtils.isBlank(tokenId) || authenticatedUser.getUser() == null) {
            throw new AccessDeniedException("General info missing");
        }

        Optional<GatewayTokenEntity> tokenEntity = gatewayTokenRepository.findByGateway(gateway);
        if (tokenEntity.isEmpty() || !StringUtils.equals(tokenEntity.get().getId(), tokenId)) {
            throw new AccessDeniedException("Unknown gateway token");
        }

        SimpUser user = userRegistry.getUser(gateway.getId());
        if (StringUtils.equals(request.getPathInfo(), "/smt-ws") && user != null) {
            throw new AccessDeniedException("There is already connected gateway by this token");
        }
    }

    @Transactional
    public void logout(String gatewayId) throws Exception {
        GatewayEntity gateway = getGatewayWithValidation(gatewayId);
        Optional<GatewayTokenEntity> tokenEntity = gatewayTokenRepository.findByGateway(gateway);
        if (tokenEntity.isEmpty()) {
            return;
        }
        gatewayTokenRepository.delete(tokenEntity.get());

        AuthenticatedUser user = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (user.getTokenType().equals(TokenType.USER)) {
            GatewayCommandMessage message = new GatewayCommandMessage(GatewayCommand.LOGOUT, null);
            message.setNeedResponse(false);
            requestService.sendMessage(gatewayId, message);
        } else if (userRegistry.getUser(gatewayId) != null) {
            requestService.event(user, GatewayEventType.DISCONNECTED);
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
