package ru.pobopo.smartthing.cloud.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smartthing.cloud.context.ContextHolder;
import ru.pobopo.smartthing.cloud.dto.GatewayDto;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.mapper.GatewayMapper;

@RestController
@RequestMapping("/gateway/me")
public class GatewayController {
    private final GatewayMapper gatewayMapper;

    @Autowired
    public GatewayController(GatewayMapper gatewayMapper) {
        this.gatewayMapper = gatewayMapper;
    }

    @GetMapping("/info")
    public GatewayDto getGatewayInfo() {
        GatewayEntity entity = ContextHolder.getCurrentGateway();
        if (entity == null) {
            return null;
        }
        return gatewayMapper.toDto(entity);
    }
}
