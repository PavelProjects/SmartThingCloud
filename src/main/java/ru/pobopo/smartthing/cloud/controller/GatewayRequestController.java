package ru.pobopo.smartthing.cloud.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smartthing.cloud.annotation.RequiredRole;
import ru.pobopo.smartthing.cloud.controller.model.SendCommandRequest;
import ru.pobopo.smartthing.cloud.exception.CommandNotAllowed;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.model.AuthenticatedUser;
import ru.pobopo.smartthing.cloud.service.GatewayRequestService;
import ru.pobopo.smartthing.model.InternalHttpResponse;
import ru.pobopo.smartthing.model.stomp.*;

import java.util.Objects;

import static ru.pobopo.smartthing.cloud.model.Role.Constants.GATEWAY;
import static ru.pobopo.smartthing.cloud.model.Role.Constants.USER;

@RestController
@RequestMapping("/gateway/requests")
@RequiredArgsConstructor
public class GatewayRequestController {
    private final GatewayRequestService requestService;

    @RequiredRole(roles = {USER, GATEWAY})
    @PostMapping("/device")
    public ResponseEntity<String> sendRequest(@RequestBody DeviceRequest deviceRequest) throws Exception {
        Objects.requireNonNull(deviceRequest);

        InternalHttpResponse response = requestService.sendMessage(
                deviceRequest.getGatewayId(),
                new DeviceRequestMessage(deviceRequest)
        );
        return new ResponseEntity<>(response.getData(), HttpStatus.valueOf(response.getStatus()));
    }

    // todo bad idea, should split logic for http requests and other commands
    @RequiredRole(roles = USER)
    @PostMapping("/command")
    public ResponseEntity<String> sendCommand(@RequestBody SendCommandRequest messageRequest) throws Exception {
        Objects.requireNonNull(messageRequest);
        if (messageRequest.getCommand() == null || GatewayCommand.LOGOUT.equals(messageRequest.getCommand())) {
            throw new CommandNotAllowed("Command not allowed");
        }

        InternalHttpResponse response = requestService.sendMessage(messageRequest.getGatewayId(), messageRequest);
        return new ResponseEntity<>(response.getData(), HttpStatus.valueOf(response.getStatus()));
    }

    @RequiredRole(roles = GATEWAY)
    @PostMapping("/response")
    public void processResponse(@RequestBody ResponseMessage response) {
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
