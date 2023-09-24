package ru.pobopo.smartthing.cloud.service.impl;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.rabbitmq.RabbitCreditsHolder;
import ru.pobopo.smartthing.cloud.context.ContextHolder;
import ru.pobopo.smartthing.cloud.controller.model.rabbitmq.BasicCheck;
import ru.pobopo.smartthing.cloud.controller.model.rabbitmq.ResourceCheck;
import ru.pobopo.smartthing.cloud.controller.model.rabbitmq.TopicCheck;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.cloud.service.RabbitAuthService;
import ru.pobopo.smartthing.cloud.service.TokenService;

@Component
@Slf4j
public class RabbitAuthServiceImpl implements RabbitAuthService {
    private static final String ALLOW = "allow";
    private static final String DENY = "deny";

    private final RabbitCreditsHolder creditsHolder;
    private final TokenService tokenService;
    private final GatewayRepository gatewayRepository;

    @Autowired
    public RabbitAuthServiceImpl(
        RabbitCreditsHolder creditsHolder,
        TokenService tokenService,
        GatewayRepository gatewayRepository
    ) {
        this.creditsHolder = creditsHolder;
        this.tokenService = tokenService;
        this.gatewayRepository = gatewayRepository;
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
            tokenService.validateToken(password);
            GatewayEntity gateway = ContextHolder.getCurrentGateway();
            if (gateway == null) {
                log.warn("No gateway in context! Wrong token?");
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
        return gateway.isPresent() ? ALLOW : DENY;
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
}
