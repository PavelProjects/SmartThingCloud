package ru.pobopo.smartthing.cloud.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;
import ru.pobopo.smartthing.cloud.dto.GatewayShortDto;
import ru.pobopo.smartthing.cloud.entity.GatewayConfigEntity;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.repository.GatewayConfigRepository;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.cloud.repository.GatewayRequestRepository;
import ru.pobopo.smartthing.cloud.service.GatewayService;
import ru.pobopo.smartthing.cloud.service.RabbitMqService;

import javax.naming.AuthenticationException;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class GatewayServiceImpl implements GatewayService {
    private final GatewayRepository gatewayRepository;
    private final GatewayRequestRepository requestRepository;
    private final GatewayBrokerServiceImpl brokerService;
    private final GatewayConfigRepository configRepository;
    private final RabbitMqService rabbitMqService;

    @Autowired
    public GatewayServiceImpl(
            GatewayRepository gatewayRepository,
            GatewayRequestRepository requestRepository,
            GatewayBrokerServiceImpl brokerService,
            GatewayConfigRepository configRepository, RabbitMqService rabbitMqService) {
        this.gatewayRepository = gatewayRepository;
        this.requestRepository = requestRepository;
        this.brokerService = brokerService;
        this.configRepository = configRepository;
        this.rabbitMqService = rabbitMqService;
    }

    @Override
    public GatewayEntity createGateway(String name, String description)
            throws AuthenticationException, ValidationException, IOException {

        GatewayEntity gatewayEntity = createGatewayAndConfig(name, description);

        rabbitMqService.createQueues(gatewayEntity);

        return gatewayEntity;
    }

    @Transactional
    private GatewayEntity createGatewayAndConfig(String name, String description) throws ValidationException, AuthenticationException {
        validateName(null, name);

        GatewayEntity gatewayEntity = new GatewayEntity();
        gatewayEntity.setOwner(AuthorisationUtils.getCurrentUser());
        gatewayEntity.setName(name);
        gatewayEntity.setDescription(description);
        gatewayEntity.setCreationDate(LocalDateTime.now());

        gatewayRepository.save(gatewayEntity);

        GatewayConfigEntity configEntity = createGatewayConfig(gatewayEntity);
        gatewayEntity.setConfig(configEntity);

        return gatewayEntity;
    }

    @Override
    public void updateGateway(GatewayShortDto gatewayShortDto)
        throws ValidationException, AuthenticationException, AccessDeniedException {
        if (gatewayShortDto == null) {
            throw new ValidationException("Dto is null!");
        }

        GatewayEntity entity = getGatewayWithValidation(gatewayShortDto.getId());

        validateName(entity, gatewayShortDto.getName());

        entity.setName(gatewayShortDto.getName());
        entity.setDescription(gatewayShortDto.getDescription());
        gatewayRepository.save(entity);
    }

    @Override
    @Transactional
    public void deleteGateway(String id)
        throws AccessDeniedException, ValidationException, AuthenticationException, IOException {
        GatewayEntity entity = getGatewayWithValidation(id);
        log.warn("Deleting gateway {}", entity);

        log.warn("Deleting gateway's requests");
        requestRepository.deleteByGateway(entity);
        log.warn("Deleting gateway queues and response listeners");
        brokerService.removeResponseListener(entity);

        gatewayRepository.delete(entity);

        rabbitMqService.deleteQueues(entity);

        log.warn("Gateway {} was deleted!", entity);
    }

    @Override
    public List<GatewayEntity> getUserGateways() throws AuthenticationException {
        return gatewayRepository.findByOwnerLogin(AuthorisationUtils.getCurrentUser().getLogin());
    }

    @Override
    public GatewayEntity getUserGatewayByName(String name) throws AuthenticationException {
        return gatewayRepository.findByNameAndOwnerLogin(name, AuthorisationUtils.getCurrentUser().getLogin());
    }

    @Override
    public GatewayEntity getGateway(String id) {
        return gatewayRepository.findById(id).get();
    }

    @Override
    public GatewayConfigEntity getConfig(String gatewayId) {
        return configRepository.findByGatewayId(gatewayId);
    }

    private GatewayConfigEntity createGatewayConfig(GatewayEntity gateway) {
        String host = rabbitMqService.getBrokeHost();
        int port = rabbitMqService.getBrokePort();

        if (StringUtils.isBlank(host)) {
            throw new RuntimeException("Message broker address is null!");
        }

        String prefix = gateway.getId() + "_" + gateway.getName();

        GatewayConfigEntity configEntity = new GatewayConfigEntity();
        configEntity.setBrokerIp(host);
        configEntity.setBrokerPort(port);
        configEntity.setGateway(gateway);
        configEntity.setQueueIn(prefix + "_in");
        configEntity.setQueueOut(prefix + "_out");

        configRepository.save(configEntity);

        log.info("Created config for gateway [id={}] : {}", gateway.getId(), configEntity);

        return configEntity;
    }

    @NotNull
    private GatewayEntity getGatewayWithValidation(String id)
        throws ValidationException, AuthenticationException, AccessDeniedException {
        if (StringUtils.isBlank(id)) {
            throw new ValidationException("Gateway id is missing!");
        }

        Optional<GatewayEntity> gatewayOptional = gatewayRepository.findById(id);
        if (gatewayOptional.isEmpty()) {
            throw new NotFoundException("Gateway with id " + id + " not found!");
        }

        if (!AuthorisationUtils.canManageGateway(gatewayOptional.get())) {
            throw new AccessDeniedException("Current user can't manage gateway " + id);
        }
        return gatewayOptional.get();
    }

    private void validateName(GatewayEntity old, String name) throws ValidationException, AuthenticationException {
        if (StringUtils.isBlank(name)) {
            throw new ValidationException("Gateway name can't be empty!");
        }
        GatewayEntity entity = getUserGatewayByName(name);
        if (entity != null && old != null && !StringUtils.equals(old.getId(), entity.getId())) {
            throw new ValidationException("Current user already have gateway with name " + name);
        }
    }
}
