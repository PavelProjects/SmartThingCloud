package ru.pobopo.smartthing.cloud.controller;

import java.util.List;
import java.util.Objects;
import javax.naming.AuthenticationException;
import javax.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smartthing.cloud.controller.model.SendCommandRequest;
import ru.pobopo.smartthing.cloud.controller.model.SendDeviceRequest;
import ru.pobopo.smartthing.cloud.dto.GatewayRequestDto;
import ru.pobopo.smartthing.cloud.dto.RequestTemplateDto;
import ru.pobopo.smartthing.cloud.entity.GatewayRequestEntity;
import ru.pobopo.smartthing.cloud.mapper.GatewayRequestMapper;
import ru.pobopo.smartthing.cloud.mapper.RequestTemplateMapper;
import ru.pobopo.smartthing.cloud.service.GatewayMessagingService;

@RestController
@RequestMapping("/gateway/request")
public class GatewayRequestController {

    private final RequestTemplateMapper requestTemplateMapper;
    private final GatewayRequestMapper gatewayRequestMapper;
    private final GatewayMessagingService requestService;

    @Autowired
    public GatewayRequestController(
        RequestTemplateMapper requestTemplateMapper,
        GatewayRequestMapper gatewayRequestMapper,
        GatewayMessagingService requestService
    ) {
        this.requestTemplateMapper = requestTemplateMapper;
        this.gatewayRequestMapper = gatewayRequestMapper;
        this.requestService = requestService;
    }

    @GetMapping("/template")
    public List<RequestTemplateDto> getTemplates() {
        return requestTemplateMapper.toDto(requestService.getRequestTemplates());
    }

    // todo filtration by gateway, date, status and etc
    @GetMapping("/list")
    public List<GatewayRequestDto> getRequests(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "100") int size
    ) throws AuthenticationException {
        return gatewayRequestMapper.toDto(requestService.getUserRequests(page, size));
    }

    @GetMapping("/{id}")
    public GatewayRequestDto getRequest(@PathVariable String id) throws AuthenticationException {
        return gatewayRequestMapper.toDto(requestService.getUserRequestById(id));
    }

    @PostMapping("/command")
    public GatewayRequestDto sendCommand(@RequestBody SendCommandRequest messageRequest) throws Exception {
        Objects.requireNonNull(messageRequest);

        GatewayRequestEntity entity = requestService.sendMessage(
            messageRequest.getGatewayId(),
            messageRequest.toGatewayCommand()
        );
        return gatewayRequestMapper.toDto(entity);
    }

    @PostMapping("/send")
    public GatewayRequestDto sendRequest(@RequestBody SendDeviceRequest deviceRequest) throws Exception {
        Objects.requireNonNull(deviceRequest);

        GatewayRequestEntity entity = requestService.sendMessage(
            deviceRequest.getGatewayId(),
            deviceRequest.toDeviceRequest()
        );
        return gatewayRequestMapper.toDto(entity);

    }
}
