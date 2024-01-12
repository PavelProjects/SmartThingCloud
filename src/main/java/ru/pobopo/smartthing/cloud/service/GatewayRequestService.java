package ru.pobopo.smartthing.cloud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.GatewayRequestEntity;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.mapper.GatewayMapper;
import ru.pobopo.smartthing.cloud.model.AuthorizedUser;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.cloud.repository.GatewayRequestRepository;
import ru.pobopo.smartthing.cloud.stomp.*;

import javax.naming.AuthenticationException;
import javax.validation.ValidationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class GatewayRequestService {
    private final GatewayRequestRepository requestRepository;
    private final GatewayRepository gatewayRepository;
    private final SimpMessagingTemplate stompService;
    private final GatewayMapper gatewayMapper;

    private final Map<String, GatewayRequestEntity> resultsMap = new ConcurrentHashMap<>();

    public List<GatewayRequestEntity> getUserRequests(int page, int size) throws AuthenticationException {
        return requestRepository.findByUser(AuthorisationUtils.getCurrentUser(), PageRequest.of(page, size, Sort.by("sentDate").descending()));
    }

    public GatewayRequestEntity getUserRequestById(String id) throws AuthenticationException {
        if (StringUtils.isBlank(id)) {
            throw new ValidationException("Request id is missing!");
        }
        return requestRepository.findByUserAndId(AuthorisationUtils.getCurrentUser(), id);
    }

    public void sendCommand(GatewayEntity entity, GatewayCommand command) {
        if (entity == null) {
            log.error("Gateway is missing!");
            return;
        }

        try {
            log.info("Trying to send logout event to [{}]", entity);
            GatewayCommandMessage commandRequest = new GatewayCommandMessage(command.name(), null);
            commandRequest.setCacheable(false);
            sendMessage(entity, commandRequest);
        } catch (Exception exception) {
            log.error("Failed to send logout message: {}", exception.getMessage());
        }
    }

    public <T extends BaseMessage> GatewayRequestEntity sendMessage(String gatewayId, T message) throws Exception {
        Optional<GatewayEntity> gateway = gatewayRepository.findById(gatewayId);
        if (gateway.isEmpty()) {
            throw new NotFoundException("Gateway with id " + gatewayId + " not found!");
        }
        return sendMessage(gateway.get(), message);
    }

    @Transactional
    public <T extends BaseMessage> GatewayRequestEntity sendMessage(GatewayEntity gateway, T message) throws Exception {
        Objects.requireNonNull(gateway, "Gateway entity is missing!");
        Objects.requireNonNull(message, "Message object is missing");

        if (!AuthorisationUtils.canManageGateway(gateway)) {
            throw new AccessDeniedException("Current user can't send request to gateway " + gateway.getId());
        }

        UserEntity user = AuthorisationUtils.getCurrentUser();
        GatewayRequestEntity requestEntity = new GatewayRequestEntity();
        requestEntity.setFinished(false);
        requestEntity.setMessage(message.toString());
        requestEntity.setSentDate(LocalDateTime.now());
        requestEntity.setGateway(gateway);
        requestEntity.setUser(user);
        requestEntity.setTarget(getTarget(gateway, message));

        if (message.isCacheable()) {
            requestRepository.save(requestEntity);

            message.setRequestId(requestEntity.getId());
        }

        stompService.convertAndSendToUser(
                gateway.getId(),
                "/queue/gateway/",
                message
        );
        log.info("User {} sent request {}", user, message);
        resultsMap.put(requestEntity.getId(), requestEntity);
        synchronized (resultsMap.get(requestEntity.getId())) {
            resultsMap.get(requestEntity.getId()).wait(5000);
        }

        return resultsMap.get(requestEntity.getId());
    }

    public void processResponse(MessageResponse response) {
        Objects.requireNonNull(response);
        if (StringUtils.isBlank(response.getRequestId())) {
            throw new RuntimeException("Request id is missing!");
        }
        Optional<GatewayRequestEntity> requestEntity = requestRepository.findById(response.getRequestId());
        if (requestEntity.isEmpty()) {
            throw new RuntimeException("Can't find request entity with id " + response.getRequestId());
        }

        GatewayRequestEntity entity = requestEntity.get();
        entity.setResult(response.getResponse());
        entity.setSuccess(response.isSuccess());
        entity.setFinished(true);
        entity.setReceiveDate(LocalDateTime.now());
        requestRepository.save(entity);

        String id = requestEntity.get().getId();
        log.info("Request {} finished", id);

        if (resultsMap.containsKey(id)) {
            synchronized (resultsMap.get(id)) {
                resultsMap.get(id).notify();
                resultsMap.put(id, requestEntity.get());
            }
        }
    }

    public void notification(AuthorizedUser authorizedUser, GatewayNotification notification) {
        //impl email, push and etc notifications

        Optional<GatewayEntity> gatewayOpt = gatewayRepository.findById(authorizedUser.getGateway().getId());
        if (gatewayOpt.isEmpty()) {
            throw new RuntimeException("Can't find gateway from token!");
        }
        GatewayEntity gateway = gatewayOpt.get();
        notification.setGateway(gatewayMapper.toDto(gateway));
        log.debug("Sending notification to {}: {}", gateway.getOwner(), notification);
        stompService.convertAndSendToUser(
            gateway.getOwner().getLogin(),
            "/queue/notification",
            notification
        );
    }

    private <T extends BaseMessage> String getTarget(GatewayEntity gateway, T message) {
        if (message instanceof DeviceRequestMessage) {
            return ((DeviceRequestMessage) message).getTarget();
        }
        return gateway.getId();
    }
}
