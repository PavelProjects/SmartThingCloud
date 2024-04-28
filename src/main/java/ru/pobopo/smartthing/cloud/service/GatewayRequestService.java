package ru.pobopo.smartthing.cloud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.GatewayRequestException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.model.AuthenticatedUser;
import ru.pobopo.smartthing.cloud.model.ResponseMessageHolder;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.model.GatewayInfo;
import ru.pobopo.smartthing.model.InternalHttpResponse;
import ru.pobopo.smartthing.model.stomp.*;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
@RequiredArgsConstructor
public class GatewayRequestService {
    private final GatewayRepository gatewayRepository;
    private final SimpMessagingTemplate stompService;

    private final Map<UUID, ResponseMessageHolder> resultsMap = new ConcurrentHashMap<>();

    public <T extends BaseMessage> InternalHttpResponse sendMessage(String gatewayId, T message) throws Exception {
        Optional<GatewayEntity> gateway = gatewayRepository.findById(gatewayId);
        if (gateway.isEmpty()) {
            throw new NotFoundException("Gateway with id " + gatewayId + " not found!");
        }
        ResponseMessage responseMessage = sendMessage(gateway.get(), message);
        return responseMessage.getResponse();
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
        if (!message.isNeedResponse()) {
            return ResponseMessage.builder().response(InternalHttpResponse.builder().status(200).build()).build();
        }

        resultsMap.put(message.getId(), new ResponseMessageHolder());
        synchronized (resultsMap.get(message.getId())) {
            resultsMap.get(message.getId()).wait(5000);
        }
        ResponseMessage responseMessage = resultsMap.remove(message.getId()).getResponseMessage();
        if (responseMessage == null) {
            throw new TimeoutException("Failed to obtain response from gateway - timeout");
        }
        if (responseMessage.isSuccess()) {
            log.info("Request id={} finished", message.getId());
            return responseMessage;
        }
        log.error("Gateway request failed: {}", responseMessage);
        throw new GatewayRequestException(responseMessage);
    }

    public void processResponse(ResponseMessage response) {
        Objects.requireNonNull(response);
        if (response.getRequestId() == null) {
            return;
        }

        UUID id = response.getRequestId();
        if (resultsMap.containsKey(id)) {
            synchronized (resultsMap.get(id)) {
                resultsMap.get(id).setResponseMessage(response);
                resultsMap.get(id).notify();
            }
        }
    }

    //todo impl email, push and etc notifications
    public void notification(AuthenticatedUser authenticatedUser, GatewayNotification notification) throws ValidationException {
        Optional<GatewayEntity> gatewayOpt = gatewayRepository.findById(authenticatedUser.getGateway().getId());
        if (gatewayOpt.isEmpty()) {
            throw new ValidationException("Can't find gateway from token!");
        }
        GatewayEntity gateway = gatewayOpt.get();
        notification.setGateway(new GatewayInfo(gateway.getId(), gateway.getName(), gateway.getDescription()));
        log.info("Sending notification to {}: {}", gateway.getOwner(), notification);
        stompService.convertAndSendToUser(
            gateway.getOwner().getLogin(),
            "/topic/notification",
            notification
        );
    }

    public void event(AuthenticatedUser authenticatedUser, GatewayEventType event) throws ValidationException {
        Optional<GatewayEntity> gatewayOpt = gatewayRepository.findById(authenticatedUser.getGateway().getId());
        if (gatewayOpt.isEmpty()) {
            throw new ValidationException("Can't find gateway from token!");
        }
        GatewayEntity gateway = gatewayOpt.get();
        GatewayEvent gatewayEvent = new GatewayEvent(
                new GatewayInfo(gateway.getId(), gateway.getName(), gateway.getDescription()),
                event
        );
        log.info("Sending gateway event to {}: {}", gateway.getOwner(), gatewayEvent);
        stompService.convertAndSendToUser(
                gateway.getOwner().getLogin(),
                "/topic/event",
                gatewayEvent
        );
    }
}
