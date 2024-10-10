package ru.pobopo.smartthing.cloud.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smartthing.cloud.aspect.RequiredRole;
import ru.pobopo.smartthing.cloud.controller.dto.SendCommandRequest;
import ru.pobopo.smartthing.cloud.exception.CommandNotAllowed;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.model.AuthenticatedUser;
import ru.pobopo.smartthing.cloud.service.gateway.GatewayRequestService;
import ru.pobopo.smartthing.model.InternalHttpResponse;
import ru.pobopo.smartthing.model.stomp.*;

import java.util.Objects;
import java.util.concurrent.TimeoutException;

import static ru.pobopo.smartthing.cloud.model.Role.Constants.GATEWAY;
import static ru.pobopo.smartthing.cloud.model.Role.Constants.USER;

@Slf4j
@RestController
@RequestMapping("/api/gateway/requests")
@RequiredArgsConstructor
public class GatewayRequestController {
    private final GatewayRequestService requestService;

    @RequiredRole(roles = USER)
    @PostMapping("/{gatewayId}")
    public ResponseEntity<String> sendGatewayRequest(
            @PathVariable String gatewayId,
            @RequestBody GatewayRequestMessage requestMessage
    ) throws ValidationException {
        InternalHttpResponse response = requestService.sendGatewayRequest(gatewayId, requestMessage);
        return response.toResponseEntity();
    }

    @RequiredRole(roles = {USER, GATEWAY})
    @PostMapping("/device")
    public ResponseEntity<String> sendDeviceRequest(@RequestBody DeviceRequest deviceRequest) throws ValidationException {
        InternalHttpResponse response = requestService.sendDeviceRequest(deviceRequest);
        return response.toResponseEntity();
    }

    @RequiredRole(roles = USER)
    @PostMapping("/command")
    public Object sendCommand(@RequestBody SendCommandRequest messageRequest) throws Exception {
        Objects.requireNonNull(messageRequest);
        if (messageRequest.getCommand() == null || GatewayCommand.LOGOUT.equals(messageRequest.getCommand())) {
            throw new CommandNotAllowed("Command not allowed");
        }

        ResponseMessage response = requestService.sendMessage(messageRequest.getGatewayId(), messageRequest);
        return response.getData();
    }

    @RequiredRole(roles = GATEWAY)
    @PostMapping("/response")
    public void processResponse(@RequestBody ResponseMessage response) throws InterruptedException, TimeoutException {
        requestService.processResponse(Objects.requireNonNull(response));
    }

    @RequiredRole(roles = GATEWAY)
    @PostMapping("/notification")
    public void notification(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody GatewayNotification notification
    ) throws ValidationException {
        requestService.notification(authenticatedUser, Objects.requireNonNull(notification));
    }

    @RequiredRole(roles = GATEWAY)
    @PostMapping("/event")
    public void event(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam GatewayEventType event
    ) throws ValidationException {
        requestService.event(authenticatedUser, Objects.requireNonNull(event));
    }
}
