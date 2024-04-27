package ru.pobopo.smartthing.cloud.controller;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smartthing.cloud.annotation.RequiredRole;
import ru.pobopo.smartthing.cloud.controller.model.SendCommandRequest;
import ru.pobopo.smartthing.cloud.controller.model.SendDeviceRequest;
import ru.pobopo.smartthing.cloud.exception.CommandNotAllowed;
import ru.pobopo.smartthing.cloud.model.AuthenticatedUser;
import ru.pobopo.smartthing.cloud.service.GatewayRequestService;
import ru.pobopo.smartthing.model.InternalHttpResponse;
import ru.pobopo.smartthing.model.stomp.DeviceRequestMessage;
import ru.pobopo.smartthing.model.stomp.GatewayCommand;
import ru.pobopo.smartthing.model.stomp.GatewayNotification;
import ru.pobopo.smartthing.model.stomp.ResponseMessage;

import java.util.Objects;

import static ru.pobopo.smartthing.cloud.model.Role.Constants.GATEWAY;
import static ru.pobopo.smartthing.cloud.model.Role.Constants.USER;

@RestController
@RequestMapping("/gateway/requests")
@RequiredArgsConstructor
public class GatewayRequestController {
    private final GatewayRequestService requestService;

    @RequiredRole(roles = USER)
    @PostMapping("/device")
    public ResponseEntity<String> sendRequest(@RequestBody SendDeviceRequest deviceRequest) throws Exception {
        Objects.requireNonNull(deviceRequest);

        InternalHttpResponse response = requestService.sendMessage(
                deviceRequest.getGatewayId(),
                new DeviceRequestMessage(deviceRequest.getRequest())
        );
        return new ResponseEntity<>(response.getData(), HttpStatus.valueOf(response.getStatus()));
    }

    @RequiredRole(roles = USER)
    @PostMapping("/command")
    public ResponseEntity<String> sendCommand(@RequestBody SendCommandRequest messageRequest) throws Exception {
        Objects.requireNonNull(messageRequest);
        if (StringUtils.equals(messageRequest.getCommand(), GatewayCommand.LOGOUT.getName())) {
            // todo filter all incoming commands
            throw new CommandNotAllowed("Command not allowed");
        }

        InternalHttpResponse response = requestService.sendMessage(
            messageRequest.getGatewayId(),
            messageRequest.toGatewayCommand()
        );
        return new ResponseEntity<>(response.getData(), HttpStatus.valueOf(response.getStatus()));
    }

    @RequiredRole(roles = GATEWAY)
    @PostMapping("/response")
    public void sendResponse(@RequestBody ResponseMessage response) {
        requestService.processResponse(Objects.requireNonNull(response));
    }

    @RequiredRole(roles = GATEWAY)
    @PostMapping("/notification")
    public void notification(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody GatewayNotification notification
    ) {
        Objects.requireNonNull(notification);

        requestService.notification(authenticatedUser, notification);
    }
}
