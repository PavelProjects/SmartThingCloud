package ru.pobopo.smartthing.cloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.GatewayRequestException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.model.AuthenticatedUser;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.model.GatewayInfo;
import ru.pobopo.smartthing.model.InternalHttpResponse;
import ru.pobopo.smartthing.model.stomp.*;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
@RequiredArgsConstructor
public class GatewayRequestService {
    private final GatewayRepository gatewayRepository;
    private final SimpMessagingTemplate stompService;
    private final ObjectMapper objectMapper;

    private final Map<UUID, Exchanger<ResponseMessage>> resultsMap = new ConcurrentHashMap<>();

    public InternalHttpResponse sendGatewayRequest(String gatewayId, GatewayRequestMessage requestMessage) throws ValidationException {
        if (StringUtils.isBlank(gatewayId)) {
            throw new ValidationException("Gateway id is missing!");
        }
        Objects.requireNonNull(requestMessage);
        if (StringUtils.isBlank(requestMessage.getMethod()) || StringUtils.isBlank(requestMessage.getUrl())) {
            throw new ValidationException("Request method and url is required!");
        }

        ResponseMessage responseMessage = sendMessage(gatewayId, requestMessage);
        return objectMapper.convertValue(responseMessage.getData(), InternalHttpResponse.class);
    }

    public InternalHttpResponse sendDeviceRequest(DeviceRequest request) throws ValidationException {
        Objects.requireNonNull(request, "Device request is missing!");
        Objects.requireNonNull(request.getDevice(), "Device info is missing!");

        if (StringUtils.isBlank(request.getGatewayId())) {
            throw new ValidationException("Gateway id can't be blank!");
        }
        if (StringUtils.isBlank(request.getDevice().getIp()) && StringUtils.isBlank(request.getDevice().getName())) {
            throw new ValidationException("Target device name and ip can't be blank!");
        }

        ResponseMessage responseMessage = sendMessage(request.getGatewayId(), new DeviceRequestMessage(request));
        return objectMapper.convertValue(responseMessage.getData(), InternalHttpResponse.class);
    }

    public <T extends BaseMessage> ResponseMessage sendMessage(String gatewayId, T message) {
        Optional<GatewayEntity> gateway = gatewayRepository.findById(gatewayId);
        if (gateway.isEmpty()) {
            throw new AccessDeniedException("Gateway with id " + gatewayId + " not found!");
        }
        return sendMessage(gateway.get(), message);
    }

    @SneakyThrows
    public <T extends BaseMessage> ResponseMessage sendMessage(GatewayEntity gateway, T message) {
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
            return ResponseMessage.builder().data(InternalHttpResponse.builder().status(200).build()).build();
        }
        Exchanger<ResponseMessage> exchanger = new Exchanger<>();
        resultsMap.put(message.getId(), exchanger);
        try {
            ResponseMessage responseMessage = exchanger.exchange(null, 5000, TimeUnit.MILLISECONDS);
            if (responseMessage == null) {
                throw new TimeoutException("Failed to obtain response from gateway - timeout");
            }
            if (responseMessage.isSuccess()) {
                log.info("Request id={} finished", message.getId());
                return responseMessage;
            }
            log.error("Gateway request failed: {}", responseMessage);
            throw new GatewayRequestException(responseMessage);
        } finally {
            resultsMap.remove(message.getId());
        }
    }

    public void processResponse(ResponseMessage response) throws InterruptedException, TimeoutException {
        Objects.requireNonNull(response);
        if (response.getRequestId() == null) {
            return;
        }

        UUID id = response.getRequestId();
        if (resultsMap.containsKey(id)) {
            resultsMap.get(id).exchange(response, 5000, TimeUnit.MILLISECONDS);
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
