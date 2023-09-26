package ru.pobopo.smartthing.cloud.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.naming.AuthenticationException;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.context.ContextHolder;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.TokenInfoEntity;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.jwt.TokenType;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.cloud.repository.UserRepository;
import ru.pobopo.smartthing.cloud.service.TokenService;
import ru.pobopo.smartthing.cloud.service.AuthService;

@Component
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final GatewayRepository gatewayRepository;

    @Autowired
    public AuthServiceImpl(
        AuthenticationManager authenticationManager,
        TokenService tokenService,
        UserRepository userRepository,
        GatewayRepository gatewayRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.gatewayRepository = gatewayRepository;
    }

    @Override
    public String authUser(String login, String password)
        throws ValidationException, AuthenticationException, AccessDeniedException {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(login, password)
        );
        if (!auth.isAuthenticated()) {
            throw new BadCredentialsException("Wrong user credits");
        }
        return tokenService.generateToken(getUser(login));
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

        return tokenService.generateToken(getCurrentUser(), gatewayEntity.get());
    }

    @Override
    public void userLogout() throws ValidationException, AccessDeniedException, AuthenticationException {
        logout(TokenType.USER);
    }

    @Override
    public void gatewayLogout() throws ValidationException, AccessDeniedException, AuthenticationException {
        logout(TokenType.GATEWAY);
    }

    @Override
    public List<GatewayEntity> getUserAuthorizedGateways()
        throws ValidationException, AuthenticationException {
        UserEntity user = getCurrentUser();
        List<TokenInfoEntity> tokens = tokenService.getActiveGatewayTokens(user);
        if (tokens.isEmpty()) {
            return List.of();
        }

        return tokens.stream().map(TokenInfoEntity::getGateway).collect(Collectors.toList());
    }

    private void logout(@NonNull TokenType requiredType) throws ValidationException, AccessDeniedException, AuthenticationException {
        if (!requiredType.equals(ContextHolder.getTokenType())) {
            throw new ValidationException("Wrong token type!");
        }
        tokenService.deactivateToken(ContextHolder.getTokenId());
    }

    private UserEntity getCurrentUser() throws ValidationException, AuthenticationException {
        return getUser(AuthoritiesUtil.getCurrentUserLogin());
    }

    private UserEntity getUser(String login) throws ValidationException {
        if (StringUtils.isBlank(login)) {
            throw new ValidationException("Login is missing");
        }
        UserEntity user = userRepository.findByLogin(login);
        if (user == null) {
            throw new ValidationException("User not found!");
        }
        return user;
    }
}
