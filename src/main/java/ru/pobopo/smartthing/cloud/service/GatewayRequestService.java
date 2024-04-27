package ru.pobopo.smartthing.cloud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.model.AuthenticatedUser;
import ru.pobopo.smartthing.cloud.model.ResponseMessageHolder;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.model.GatewayInfo;
import ru.pobopo.smartthing.model.stomp.*;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class GatewayRequestService {
    private final GatewayRepository gatewayRepository;
    private final SimpMessagingTemplate stompService;

    private final Map<UUID, ResponseMessageHolder> resultsMap = new ConcurrentHashMap<>();

    public void sendCommand(GatewayEntity entity, GatewayCommand command) {
        if (entity == null) {
            log.error("Gateway is missing!");
            return;
        }

        try {
            log.info("Trying to send logout event to [{}]", entity);
            GatewayCommandMessage commandRequest = new GatewayCommandMessage(command.name(), null);
            sendMessage(entity, commandRequest);
        } catch (Exception exception) {
            log.error("Failed to send logout message: {}", exception.getMessage());
        }
    }

    public <T extends BaseMessage> ResponseMessage sendMessage(String gatewayId, T message) throws Exception {
        Optional<GatewayEntity> gateway = gatewayRepository.findById(gatewayId);
        if (gateway.isEmpty()) {
            throw new NotFoundException("Gateway with id " + gatewayId + " not found!");
        }
        return sendMessage(gateway.get(), message);
    }

    @Transactional
    public <T extends BaseMessage> ResponseMessage sendMessage(GatewayEntity gateway, T message) throws Exception {
        Objects.requireNonNull(gateway, "Gateway entity is missing!");
        Objects.requireNonNull(message, "Message object is missing");

        if (!AuthorisationUtils.canManageGateway(gateway)) {
            throw new AccessDeniedException("Current user can't send request to gateway " + gateway.getId());
        }

        message.setId(UUID.randomUUID());
        stompService.convertAndSendToUser(
                gateway.getId(),
                "/queue/gateway/",
                message
        );
        log.info("User {} sent request {}", AuthorisationUtils.getCurrentUser(), message);

        resultsMap.put(message.getId(), new ResponseMessageHolder());
        synchronized (resultsMap.get(message.getId())) {
            resultsMap.get(message.getId()).wait(15000);
        }
        return resultsMap.remove(message.getId()).getResponse();
    }

    public void processResponse(ResponseMessage response) {
        Objects.requireNonNull(response);
        Objects.requireNonNull(response.getRequestId());

        UUID id = response.getRequestId();
        log.info("Request id={} finished", id);

        if (resultsMap.containsKey(id)) {
            synchronized (resultsMap.get(id)) {
                resultsMap.get(id).setResponse(response);
                resultsMap.get(id).notify();
            }
        }
    }

    public void notification(AuthenticatedUser authenticatedUser, GatewayNotification notification) {
        //todo impl email, push and etc notifications

        Optional<GatewayEntity> gatewayOpt = gatewayRepository.findById(authenticatedUser.getGateway().getId());
        if (gatewayOpt.isEmpty()) {
            throw new RuntimeException("Can't find gateway from token!");
        }
        GatewayEntity gateway = gatewayOpt.get();
        notification.setGateway(new GatewayInfo(gateway.getId(), gateway.getName(), gateway.getDescription()));
        log.debug("Sending notification to {}: {}", gateway.getOwner(), notification);
        stompService.convertAndSendToUser(
            gateway.getOwner().getLogin(),
            "/queue/notification",
            notification
        );
    }
}
