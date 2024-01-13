package ru.pobopo.smartthing.cloud.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smartthing.cloud.annotation.RequiredRole;
import ru.pobopo.smartthing.cloud.controller.model.SendCommandRequest;
import ru.pobopo.smartthing.cloud.controller.model.SendDeviceRequest;
import ru.pobopo.smartthing.cloud.dto.GatewayRequestDto;
import ru.pobopo.smartthing.cloud.entity.GatewayRequestEntity;
import ru.pobopo.smartthing.cloud.exception.CommandNotAllowed;
import ru.pobopo.smartthing.cloud.mapper.GatewayRequestMapper;
import ru.pobopo.smartthing.cloud.model.AuthorizedUser;
import ru.pobopo.smartthing.cloud.model.stomp.DeviceRequestMessage;
import ru.pobopo.smartthing.cloud.model.stomp.GatewayCommand;
import ru.pobopo.smartthing.cloud.model.stomp.GatewayNotification;
import ru.pobopo.smartthing.cloud.model.stomp.MessageResponse;
import ru.pobopo.smartthing.cloud.service.GatewayRequestService;

import javax.naming.AuthenticationException;
import java.util.List;
import java.util.Objects;

import static ru.pobopo.smartthing.cloud.model.Role.Constants.GATEWAY;
import static ru.pobopo.smartthing.cloud.model.Role.Constants.USER;

@RestController
@RequestMapping("/gateway/requests")
public class GatewayRequestController {
    private final GatewayRequestMapper gatewayRequestMapper;
    private final GatewayRequestService requestService;

    @Autowired
    public GatewayRequestController(
        GatewayRequestMapper gatewayRequestMapper,
        GatewayRequestService requestService
    ) {
        this.gatewayRequestMapper = gatewayRequestMapper;
        this.requestService = requestService;
    }

    @RequiredRole(roles = USER)
    @PostMapping("/device")
    public GatewayRequestDto sendRequest(@RequestBody SendDeviceRequest deviceRequest) throws Exception {
        Objects.requireNonNull(deviceRequest);

        GatewayRequestEntity entity = requestService.sendMessage(
                deviceRequest.getGatewayId(),
                new DeviceRequestMessage(deviceRequest.getRequest())
        );
        return gatewayRequestMapper.toDto(entity);
    }

    @RequiredRole(roles = USER)
    @PostMapping("/command")
    public GatewayRequestDto sendCommand(@RequestBody SendCommandRequest messageRequest) throws Exception {
        Objects.requireNonNull(messageRequest);
        if (StringUtils.equals(messageRequest.getCommand(), GatewayCommand.LOGOUT.getName())) {
            throw new CommandNotAllowed("Command logout not allowed");
        }

        GatewayRequestEntity entity = requestService.sendMessage(
            messageRequest.getGatewayId(),
            messageRequest.toGatewayCommand()
        );
        return gatewayRequestMapper.toDto(entity);
    }

    @RequiredRole(roles = GATEWAY)
    @PostMapping("/response")
    public void sendResponse(@RequestBody MessageResponse response) throws JsonProcessingException {
        Objects.requireNonNull(response);

        requestService.processResponse(response);
    }

    @RequiredRole(roles = GATEWAY)
    @PostMapping("/notification")
    public void notification(
            @AuthenticationPrincipal AuthorizedUser authorizedUser,
            @RequestBody GatewayNotification notification
    ) {
        Objects.requireNonNull(notification);

        requestService.notification(authorizedUser, notification);
    }

    // todo filtration by gateway, date, status and etc
    @RequiredRole(roles = USER)
    @GetMapping("/list")
    public List<GatewayRequestDto> getRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) throws AuthenticationException {
        return gatewayRequestMapper.toDto(requestService.getUserRequests(page, size));
    }

    @RequiredRole(roles = USER)
    @GetMapping("/{id}")
    public GatewayRequestDto getRequest(@PathVariable String id) throws AuthenticationException {
        return gatewayRequestMapper.toDto(requestService.getUserRequestById(id));
    }

}
