package ru.pobopo.smartthing.cloud.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.controller.model.rabbitmq.BasicCheck;
import ru.pobopo.smartthing.cloud.controller.model.rabbitmq.ResourceCheck;
import ru.pobopo.smartthing.cloud.controller.model.rabbitmq.TopicCheck;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.model.AuthorizedUser;
import ru.pobopo.smartthing.cloud.model.TokenType;
import ru.pobopo.smartthing.cloud.rabbitmq.RabbitCreditsHolder;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.cloud.service.AuthService;
import ru.pobopo.smartthing.cloud.service.RabbitAuthService;

import java.util.Optional;

@Component
@Slf4j
public class RabbitAuthServiceImpl implements RabbitAuthService {
    private static final String ALLOW = "allow";
    private static final String DENY = "deny";

    private final RabbitCreditsHolder creditsHolder;
    private final GatewayRepository gatewayRepository;
    private final AuthService authService;
    
    @Autowired
    public RabbitAuthServiceImpl(
        RabbitCreditsHolder creditsHolder,
        GatewayRepository gatewayRepository,
        AuthService authService
    ) {
        this.creditsHolder = creditsHolder;
        this.gatewayRepository = gatewayRepository;
        this.authService = authService;
    }

        /*
            for gateway username = gateway_id, password = gateway token
         */
    @Override
    public String authUser(String username, String password) {
        if (isAdmin(username)) {
            return StringUtils.equals(creditsHolder.getPassword(), password) ? ALLOW : DENY;
        }
        try {
            AuthorizedUser authorizedUser = authService.validateToken(password);
            if (!TokenType.GATEWAY.equals(authorizedUser.getTokenType())) {
                log.error("Wrong token type! Got {}", authorizedUser.getTokenType().getName());
                return DENY;
            }
            GatewayEntity gateway = authorizedUser.getGateway();
            if (gateway == null) {
                log.warn("No gateway in authorization!");
                return DENY;
            }
            return StringUtils.equals(gateway.getId(), username) ? ALLOW : DENY;
        } catch (Exception exception) {
            log.error("RabbitMQ authorization failed for {} with password {}: {}", username, password, exception.getMessage());
            return DENY;
        }
    }

    @Override
    public String authVhost(BasicCheck check) {
        if (isAdmin(check.getUsername())) {
            return ALLOW;
        }
        Optional<GatewayEntity> gateway = gatewayRepository.findById(check.getUsername());
        return gateway.isPresent() ? ALLOW : DENY;
    }

    @Override
    public String authResource(ResourceCheck check) {
        if (isAdmin(check.getUsername())) {
            return ALLOW;
        }
        Optional<GatewayEntity> gateway = gatewayRepository.findById(check.getUsername());
        // todo resource=exchange, name=amq.default, permission=write -> allow
        // (resource=queue, name=test_gateway_123, permission=read) -> check gateway

        if (gateway.isEmpty()) {
            return DENY;
        }

        if (StringUtils.equals(check.getResource(), "exchange")) {
            return ALLOW;
        }

        if (StringUtils.equals(check.getResource(), "queue") && haveAccess(check, gateway.get())) {
            return ALLOW;
        }

        return DENY;
    }

    @Override
    public String authTopic(TopicCheck check) {
        if (isAdmin(check.getUsername())) {
            return ALLOW;
        }
        Optional<GatewayEntity> gateway = gatewayRepository.findById(check.getUsername());
        return gateway.isPresent() ? ALLOW : DENY;
    }

    private boolean isAdmin(String username) {
        return StringUtils.equals(creditsHolder.getLogin(), username);
    }

    private boolean haveAccess(ResourceCheck check, GatewayEntity gateway) {
        switch (check.getPermission()) {
            case "read":
                return StringUtils.equals(check.getName(), gateway.getConfig().getQueueIn());
            case "write":
                return  StringUtils.equals(check.getName(), gateway.getConfig().getQueueOut());
            default:
                log.info("Unknown permission: {}", check.getPermission());
                return false;
        }
    }
}
