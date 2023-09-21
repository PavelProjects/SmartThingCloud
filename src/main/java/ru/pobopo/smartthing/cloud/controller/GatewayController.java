package ru.pobopo.smartthing.cloud.controller;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smartthing.cloud.context.ContextHolder;
import ru.pobopo.smartthing.cloud.dto.GatewayDto;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.UnsupportedMessageClassException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.mapper.GatewayMapper;
import ru.pobopo.smartthing.cloud.service.RabbitMqService;
import ru.pobopo.smartthing.cloud.rabbitmq.GatewayCommand;

@RestController
@RequestMapping("/gateway/me")
public class GatewayController {
    private final GatewayMapper gatewayMapper;
    private final RabbitMqService rabbitMqService;

    @Autowired
    public GatewayController(
        GatewayMapper gatewayMapper,
        RabbitMqService rabbitMqService
    ) {
        this.gatewayMapper = gatewayMapper;
        this.rabbitMqService = rabbitMqService;
    }

    @GetMapping("/info")
    public GatewayDto getGatewayInfo() {
        GatewayEntity entity = ContextHolder.getCurrentGateway();
        if (entity == null) {
            return null;
        }
        return gatewayMapper.toDto(entity);
    }

    @PostMapping("/command")
    public void sendCommand(@RequestBody GatewayCommand command)
        throws IOException, TimeoutException, ValidationException, UnsupportedMessageClassException {
        rabbitMqService.send(ContextHolder.getCurrentGateway(), command);
    }
}
