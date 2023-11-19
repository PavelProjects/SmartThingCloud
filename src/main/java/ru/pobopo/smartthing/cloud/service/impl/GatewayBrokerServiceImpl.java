package ru.pobopo.smartthing.cloud.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.GatewayRequestEntity;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.BrokerException;
import ru.pobopo.smartthing.cloud.rabbitmq.BaseMessage;
import ru.pobopo.smartthing.cloud.rabbitmq.DeviceRequestMessage;
import ru.pobopo.smartthing.cloud.rabbitmq.GatewayCommand;
import ru.pobopo.smartthing.cloud.rabbitmq.GatewayResponseProcessor;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.cloud.repository.GatewayRequestRepository;
import ru.pobopo.smartthing.cloud.service.GatewayBrokerService;
import ru.pobopo.smartthing.cloud.service.RabbitMqService;

import javax.naming.AuthenticationException;
import javax.validation.ValidationException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
public class GatewayBrokerServiceImpl implements GatewayBrokerService {

    private final int requestsLimit;
    private final GatewayRequestRepository requestRepository;
    private final GatewayRepository gatewayRepository;
    private final RabbitMqService rabbitMqService;
    private final GatewayResponseProcessor responseProcessor;

    @Autowired
    public GatewayBrokerServiceImpl(
        Environment environment,
        GatewayRequestRepository requestRepository,
        RabbitMqService rabbitMqService,
        GatewayRepository gatewayRepository,
        GatewayResponseProcessor responseProcessor
    ) {
        this.requestRepository = requestRepository;
        this.rabbitMqService = rabbitMqService;
        this.gatewayRepository = gatewayRepository;
        this.responseProcessor = responseProcessor;
        this.requestsLimit = Integer.parseInt(environment.getProperty("REQUESTS_LIMIT", "10"));

        log.debug("Requests limit: {}", requestsLimit);
    }


    @Override
    public List<GatewayRequestEntity> getUserRequests(int page, int size) throws AuthenticationException {
        return requestRepository.findByUser(AuthorisationUtils.getCurrentUser(), PageRequest.of(page, size, Sort.by("sentDate").descending()));
    }

    @Override
    public GatewayRequestEntity getUserRequestById(String id) throws AuthenticationException {
        if (StringUtils.isBlank(id)) {
            throw new ValidationException("Request id is missing!");
        }
        return requestRepository.findByUserAndId(AuthorisationUtils.getCurrentUser(), id);
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

        if (!AuthorisationUtils.canManageGateway(gateway)) {
            throw new AccessDeniedException("Current user can't send request to gateway " + gateway.getId());
        }

        String target = getTarget(gateway, message);
        long count = requestRepository.countByFinishedAndTarget(false, target);
        if (count >= requestsLimit) {
            throw new BrokerException(
                String.format(
                    "Can't send request - reached limit[%d] of unfinished requests for target. Current count = %d",
                    requestsLimit,
                    count
                )
            );
        }

        UserEntity user = AuthorisationUtils.getCurrentUser();
        GatewayRequestEntity requestEntity = new GatewayRequestEntity();
        requestEntity.setFinished(false);
        requestEntity.setMessage(message.toString());
        requestEntity.setSentDate(LocalDateTime.now());
        requestEntity.setGateway(gateway);
        requestEntity.setUser(user);
        requestEntity.setTarget(target);

        if (message.isCacheable()) {
            requestRepository.save(requestEntity);

            message.setRequestId(requestEntity.getId());
        }

        rabbitMqService.send(gateway, message);
        log.info("User {} sent request {} to {}", user, message, gateway);

        return requestEntity;
    }

    @Override
    public void addResponseListeners() throws IOException {
        List<GatewayEntity> entities = gatewayRepository.findAll();
        if (entities.isEmpty()) {
            log.info("No gateways were found -> skipping response listeners creation");
            return;
        }

        log.info("Adding response listeners for gateways. Total count: {}", entities.size());
        for (GatewayEntity entity: entities) {
            try {
                if (entity.getConfig() == null) {
                    rabbitMqService.createQueues(entity);
                    addResponseListener(entity);
                } else {
                    log.warn("Gateway {} has not config! Skipping.", entity);
                }
            } catch (Exception exception) {
                log.error("Failed to add response listener for gateway {}", entity, exception);
            }
        }
        log.info("Response listeners added.");
    }

    @Override
    public void addResponseListener(GatewayEntity entity) throws IOException {
        Objects.requireNonNull(entity);

        rabbitMqService.addQueueListener(entity, responseProcessor::process);
    }

    @Override
    public void removeResponseListener(GatewayEntity entity) throws IOException {
        rabbitMqService.removeQueueListener(entity);
    }

    @Override
    public void gatewayLogout(GatewayEntity entity) throws IOException {
        if (entity == null) {
            log.error("Gateway is missing!");
        }

        try {
            log.info("Trying to send logout event to [{}]", entity);
            GatewayCommand command = new GatewayCommand("logout", null);
            command.setCacheable(false);
            sendMessage(entity, command);
        } catch (Exception exception) {
            log.error("Failed to send logout message: {}", exception.getMessage());
        }
        log.warn("Removing queue response listener and queues for gateway [{}]", entity);
        removeResponseListener(entity);
    }

    private <T extends BaseMessage> String getTarget(GatewayEntity gateway, T message) {
        if (message instanceof DeviceRequestMessage) {
            return ((DeviceRequestMessage) message).getTarget();
        }
        return gateway.getId();
    }
}
