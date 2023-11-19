package ru.pobopo.smartthing.cloud.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smartthing.cloud.annotation.RequiredRole;
import ru.pobopo.smartthing.cloud.controller.model.CreateGatewayRequest;
import ru.pobopo.smartthing.cloud.dto.GatewayConfigDto;
import ru.pobopo.smartthing.cloud.dto.GatewayDto;
import ru.pobopo.smartthing.cloud.dto.GatewayShortDto;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.mapper.GatewayConfigMapper;
import ru.pobopo.smartthing.cloud.mapper.GatewayMapper;
import ru.pobopo.smartthing.cloud.model.AuthorizedUser;
import ru.pobopo.smartthing.cloud.service.GatewayService;
import ru.pobopo.smartthing.cloud.service.RabbitMqService;

import javax.naming.AuthenticationException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import static ru.pobopo.smartthing.cloud.model.Role.Constants.GATEWAY;
import static ru.pobopo.smartthing.cloud.model.Role.Constants.USER;

@RestController
@RequestMapping("/gateway/management")
@Slf4j
public class GatewayManagementController {
    private final GatewayService gatewayService;
    private final RabbitMqService rabbitMqService;
    private final GatewayMapper gatewayMapper;
    private final GatewayConfigMapper configMapper;

    @Autowired
    public GatewayManagementController(
        GatewayService gatewayService,
        RabbitMqService rabbitMqService,
        GatewayMapper gatewayMapper,
        GatewayConfigMapper configMapper) {
        this.gatewayService = gatewayService;
        this.rabbitMqService = rabbitMqService;
        this.gatewayMapper = gatewayMapper;
        this.configMapper = configMapper;
    }

    @RequiredRole(roles = USER)
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public GatewayShortDto createGateway(@RequestBody CreateGatewayRequest request)
        throws AuthenticationException, ValidationException, IOException, TimeoutException {
        GatewayEntity gatewayEntity = gatewayService.createGateway(request.getName(), request.getDescription());
        Objects.requireNonNull(gatewayEntity);
        return gatewayMapper.toShortDto(gatewayEntity);
    }

    @RequiredRole(roles = USER)
    @PutMapping("/update")
    public void updateGateway(@RequestBody GatewayShortDto dto)
        throws AccessDeniedException, ValidationException, AuthenticationException {
        gatewayService.updateGateway(dto);
    }

    @RequiredRole(roles = USER)
    @DeleteMapping("/delete/{id}")
    public void deleteGateway(@PathVariable String id)
        throws AccessDeniedException, ValidationException, AuthenticationException, IOException {
        gatewayService.deleteGateway(id);
    }


    @RequiredRole(roles = USER)
    @GetMapping("/list")
    public List<GatewayDto> getList() throws AuthenticationException, InterruptedException {
        List<GatewayEntity> gateways = gatewayService.getUserGateways();
        List<GatewayDto> dtos = gatewayMapper.toDto(gateways);
        rabbitMqService.checkIsOnline(dtos);
        return dtos;
    }

    @RequiredRole(roles = USER)
    @GetMapping("/{id}")
    public GatewayDto getById(@PathVariable String id) throws IOException {
        GatewayDto dto = gatewayMapper.toDto(gatewayService.getGateway(id));
        if (dto == null) {
            return null;
        }
        dto.setOnline(rabbitMqService.isOnline(dto));
        return dto;
    }

    @RequiredRole(roles = USER)
    @GetMapping("/config/{id}")
    public GatewayConfigDto getConfigById(@PathVariable String id) {
        GatewayEntity gateway = gatewayService.getGateway(id);
        if (gateway == null || gateway.getConfig() == null) {
            return null;
        }
        return configMapper.toDto(gateway.getConfig());
    }

    @RequiredRole(roles = GATEWAY)
    @GetMapping("/config")
    public GatewayConfigDto getConfig(@AuthenticationPrincipal AuthorizedUser authorizedUser) throws AccessDeniedException {
        if (authorizedUser.getGateway() == null) {
            throw new AccessDeniedException("Gateway entity is missing");
        }
        return configMapper.toDto(gatewayService.getConfig(authorizedUser.getGateway().getId()));
    }
}
