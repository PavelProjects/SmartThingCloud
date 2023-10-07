package ru.pobopo.smartthing.cloud.controller;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import javax.naming.AuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smartthing.cloud.controller.model.CreateGatewayRequest;
import ru.pobopo.smartthing.cloud.dto.GatewayShortDto;
import ru.pobopo.smartthing.cloud.dto.GatewayDto;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.mapper.GatewayMapper;
import ru.pobopo.smartthing.cloud.service.GatewayService;
import ru.pobopo.smartthing.cloud.service.RabbitMqService;

@RestController
@RequestMapping("/gateway/management")
@Slf4j
public class GatewayManagementController {
    private final GatewayService gatewayService;
    private final RabbitMqService rabbitMqService;
    private final GatewayMapper gatewayMapper;

    @Autowired
    public GatewayManagementController(
        GatewayService gatewayService,
        RabbitMqService rabbitMqService,
        GatewayMapper gatewayMapper
    ) {
        this.gatewayService = gatewayService;
        this.rabbitMqService = rabbitMqService;
        this.gatewayMapper = gatewayMapper;
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public GatewayShortDto createGateway(@RequestBody CreateGatewayRequest request)
        throws AuthenticationException, ValidationException, IOException, TimeoutException {
        GatewayEntity gatewayEntity = gatewayService.createGateway(request.getName(), request.getDescription());
        Objects.requireNonNull(gatewayEntity);
        return gatewayMapper.toShortDto(gatewayEntity);
    }

    @PutMapping("/update")
    public void updateGateway(@RequestBody GatewayShortDto dto)
        throws AccessDeniedException, ValidationException, AuthenticationException {
        gatewayService.updateGateway(dto);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteGateway(@PathVariable String id)
        throws AccessDeniedException, ValidationException, AuthenticationException, IOException {
        gatewayService.deleteGateway(id);
    }


    @GetMapping("/list")
    public List<GatewayDto> getList() throws AuthenticationException, InterruptedException {
        List<GatewayEntity> gateways = gatewayService.getUserGateways();
        List<GatewayDto> dtos = gatewayMapper.toDto(gateways);
        rabbitMqService.checkIsOnline(dtos);
        return dtos;
    }

    @GetMapping("/{id}")
    public GatewayDto getById(@PathVariable String id) throws IOException {
        GatewayDto dto = gatewayMapper.toDto(gatewayService.getGateway(id));
        if (dto == null) {
            return null;
        }
        dto.setOnline(rabbitMqService.isOnline(dto));
        return dto;
    }
}