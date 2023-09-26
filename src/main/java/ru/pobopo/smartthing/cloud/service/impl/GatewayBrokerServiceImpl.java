package ru.pobopo.smartthing.cloud.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import javax.naming.AuthenticationException;
import javax.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.GatewayRequestEntity;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.cloud.repository.GatewayRequestRepository;
import ru.pobopo.smartthing.cloud.repository.UserRepository;
import ru.pobopo.smartthing.cloud.service.GatewayBrokerService;
import ru.pobopo.smartthing.cloud.service.GatewayResponseProcessor;
import ru.pobopo.smartthing.cloud.service.RabbitMqService;
import ru.pobopo.smartthing.cloud.rabbitmq.BaseMessage;

@Component
@Slf4j
public class GatewayBrokerServiceImpl implements GatewayBrokerService {
    private final GatewayRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final GatewayRepository gatewayRepository;
    private final RabbitMqService rabbitMqService;
    private final GatewayResponseProcessor responseProcessor;

    @Autowired
    public GatewayBrokerServiceImpl(
        GatewayRequestRepository requestRepository,
        UserRepository userRepository,
        RabbitMqService rabbitMqService,
        GatewayRepository gatewayRepository,
        GatewayResponseProcessor responseProcessor
    ) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.rabbitMqService = rabbitMqService;
        this.gatewayRepository = gatewayRepository;
        this.responseProcessor = responseProcessor;
    }


    @Override
    public List<GatewayRequestEntity> getUserRequests(int page, int size) throws AuthenticationException {
        return requestRepository.findByUser(getCurrentUser(), PageRequest.of(page, size, Sort.by("sentDate").descending()));
    }

    @Override
    public GatewayRequestEntity getUserRequestById(String id) throws AuthenticationException {
        if (StringUtils.isBlank(id)) {
            throw new ValidationException("Request id is missing!");
        }
        return requestRepository.findByUserAndId(getCurrentUser(), id);
    }

    @Override
    public <T extends BaseMessage> GatewayRequestEntity sendMessage(String gatewayId, T message) throws Exception {
        Optional<GatewayEntity> gateway = gatewayRepository.findById(gatewayId);
        if (gateway.isEmpty()) {
            throw new NotFoundException("Gateway with id " + gatewayId + " not found!");
        }
        return sendMessage(gateway.get(), message);
    }

    @Transactional
    @Override
    public <T extends BaseMessage> GatewayRequestEntity sendMessage(GatewayEntity gateway, T message) throws Exception {
        Objects.requireNonNull(gateway, "Gateway entity is missing!");
        Objects.requireNonNull(message, "Message object is missing");

        if (!AuthoritiesUtil.canManageGateway(gateway)) {
            throw new AccessDeniedException("Current user can't send request to gateway " + gateway.getId());
        }

        UserEntity user = getCurrentUser();
        GatewayRequestEntity requestEntity = new GatewayRequestEntity();
        requestEntity.setFinished(false);
        requestEntity.setMessage(message.toString());
        requestEntity.setSentDate(LocalDateTime.now());
        requestEntity.setGateway(gateway);
        requestEntity.setUser(user);
        requestRepository.save(requestEntity);

        message.setRequestId(requestEntity.getId());

        rabbitMqService.send(gateway, message);
        log.info("User {} sent request {} to {}", user, message, gateway);

        return requestEntity;
    }

    @Override
    public void addResponseListeners() throws IOException, TimeoutException {
        List<GatewayEntity> entities = gatewayRepository.findAll();
        if (entities.size() == 0) {
            log.info("No gateways were found -> skipping response listeners creation");
            return;
        }

        log.info("Adding response listeners for gateways. Total count: {}", entities.size());
        for (GatewayEntity entity: entities) {
            addResponseListener(entity);
        }
        log.info("Response listeners added.");
    }

    @Override
    public void addResponseListener(GatewayEntity entity) throws IOException {
        Objects.requireNonNull(entity);

        rabbitMqService.createQueues(entity);
        rabbitMqService.addQueueListener(entity, responseProcessor::process);
    }

    @Override
    public void removeResponseListener(GatewayEntity entity) throws IOException {
        rabbitMqService.removeQueueListener(entity);
        rabbitMqService.deleteQueues(entity);
    }

    private UserEntity getCurrentUser() throws AuthenticationException {
        UserEntity user = userRepository.findByLogin(AuthoritiesUtil.getCurrentUserLogin());
        Objects.requireNonNull(user, "Can't find user " + AuthoritiesUtil.getCurrentUserLogin());
        return user;
    }
}
