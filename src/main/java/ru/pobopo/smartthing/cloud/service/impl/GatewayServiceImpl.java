package ru.pobopo.smartthing.cloud.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javax.naming.AuthenticationException;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;
import ru.pobopo.smartthing.cloud.dto.GatewayShortDto;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.cloud.repository.GatewayRequestRepository;
import ru.pobopo.smartthing.cloud.repository.UserRepository;
import ru.pobopo.smartthing.cloud.service.GatewayService;

@Component
@Slf4j
public class GatewayServiceImpl implements GatewayService {
    private final GatewayRepository gatewayRepository;
    private final GatewayRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final GatewayBrokerServiceImpl brokerService;

    @Autowired
    public GatewayServiceImpl(
        GatewayRepository gatewayRepository,
        GatewayRequestRepository requestRepository,
        UserRepository userRepository,
        GatewayBrokerServiceImpl brokerService
    ) {
        this.gatewayRepository = gatewayRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.brokerService = brokerService;
    }

    @Override
    @Transactional
    public GatewayEntity createGateway(String name, String description)
        throws AuthenticationException, ValidationException {
        validateName(name);

        GatewayEntity gatewayEntity = new GatewayEntity();
        gatewayEntity.setOwner(AuthorisationUtils.getCurrentUser());
        gatewayEntity.setName(name);
        gatewayEntity.setDescription(description);
        gatewayEntity.setCreationDate(LocalDateTime.now());

        gatewayRepository.save(gatewayEntity);

        String prefix = gatewayEntity.getId() + "_" + gatewayEntity.getName();
        gatewayEntity.setQueueIn(prefix + "_in");
        gatewayEntity.setQueueOut(prefix + "_out");
        gatewayRepository.save(gatewayEntity);

        return gatewayEntity;
    }

    @Override
    public void updateGateway(GatewayShortDto gatewayShortDto)
        throws ValidationException, AuthenticationException, AccessDeniedException {
        if (gatewayShortDto == null) {
            throw new ValidationException("Dto is null!");
        }

        GatewayEntity entity = getGatewayWithValidation(gatewayShortDto.getId());

        validateName(gatewayShortDto.getName());

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

    private void validateName(String name) throws ValidationException, AuthenticationException {
        if (StringUtils.isBlank(name)) {
            throw new ValidationException("Gateway name can't be empty!");
        }
        if (getUserGatewayByName(name) != null) {
            throw new ValidationException("Current user already have gateway with name " + name);
        }
    }
}
