package ru.pobopo.smartthing.cloud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;
import ru.pobopo.smartthing.cloud.dto.GatewayShortDto;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.cloud.repository.GatewayRequestRepository;

import javax.naming.AuthenticationException;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class GatewayService {
    private final GatewayRepository gatewayRepository;
    private final GatewayRequestRepository requestRepository;
    private final GatewayAuthService authService;

    public GatewayEntity createGateway(String name, String description)
            throws AuthenticationException, ValidationException {
        validateName(null, name);

        GatewayEntity gatewayEntity = new GatewayEntity();
        gatewayEntity.setOwner(AuthorisationUtils.getCurrentUser());
        gatewayEntity.setName(name);
        gatewayEntity.setDescription(description);
        gatewayEntity.setCreationDate(LocalDateTime.now());

        gatewayRepository.save(gatewayEntity);

        return gatewayEntity;
    }

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

    @Transactional
    public void deleteGateway(String id)
            throws Exception {
        GatewayEntity entity = getGatewayWithValidation(id);
        log.warn("Deleting gateway {}", entity);

        log.warn("Deleting gateway's requests");
        requestRepository.deleteByGateway(entity);

        gatewayRepository.delete(entity);

        authService.logout(id);

        log.warn("Gateway {} was deleted!", entity);
    }

    public List<GatewayEntity> getUserGateways() throws AuthenticationException {
        return gatewayRepository.findByOwnerLogin(AuthorisationUtils.getCurrentUser().getLogin());
    }

    public GatewayEntity getUserGatewayByName(String name) throws AuthenticationException {
        return gatewayRepository.findByNameAndOwnerLogin(name, AuthorisationUtils.getCurrentUser().getLogin());
    }

    public GatewayEntity getGateway(String id) throws AccessDeniedException, ValidationException, AuthenticationException {
        return getGatewayWithValidation(id);
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
