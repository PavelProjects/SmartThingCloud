package ru.pobopo.smartthing.cloud.controller;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smartthing.cloud.context.ContextHolder;
import ru.pobopo.smartthing.cloud.dto.GatewayDto;
import ru.pobopo.smartthing.cloud.dto.GatewayQueueInfo;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.mapper.GatewayMapper;
import ru.pobopo.smartthing.cloud.service.GatewayService;
import ru.pobopo.smartthing.cloud.service.MessageBrokerService;
import ru.pobopo.smartthing.cloud.service.model.GatewayCommand;

@RestController
@RequestMapping("/gateway/me")
public class GatewayController {
    private final GatewayService gatewayService;
    private final GatewayMapper gatewayMapper;
    private final MessageBrokerService messageBrokerService;

    @Autowired
    public GatewayController(
        GatewayService gatewayService,
        GatewayMapper gatewayMapper,
        MessageBrokerService messageBrokerService
    ) {
        this.gatewayService = gatewayService;
        this.gatewayMapper = gatewayMapper;
        this.messageBrokerService = messageBrokerService;
    }

    @GetMapping("/info")
    public GatewayDto getGatewayInfo() {
        GatewayEntity entity = ContextHolder.getCurrentGateway();
        if (entity == null) {
            return null;
        }
        return gatewayMapper.toDto(entity);
    }

    @GetMapping("/queue")
    public GatewayQueueInfo getQueue() throws ValidationException, IOException, TimeoutException {
        return gatewayService.getQueueInfo();
    }

    @PostMapping("/command")
    public void sendCommand(@RequestBody GatewayCommand command) throws IOException, TimeoutException {
        messageBrokerService.send(ContextHolder.getCurrentGateway(), command);
    }


}
