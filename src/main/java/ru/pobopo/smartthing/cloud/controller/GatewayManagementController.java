package ru.pobopo.smartthing.cloud.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smartthing.cloud.annotation.RequiredRole;
import ru.pobopo.smartthing.cloud.controller.model.CreateGatewayRequest;
import ru.pobopo.smartthing.cloud.dto.GatewayDto;
import ru.pobopo.smartthing.cloud.dto.GatewayShortDto;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.mapper.GatewayMapper;
import ru.pobopo.smartthing.cloud.service.GatewayService;

import javax.naming.AuthenticationException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static ru.pobopo.smartthing.cloud.model.Role.Constants.USER;

@RestController
@RequestMapping("/gateway/management")
@Slf4j
@RequiredArgsConstructor
public class GatewayManagementController {
    private final GatewayService gatewayService;
    private final GatewayMapper gatewayMapper;
    private final SimpUserRegistry userRegistry;

    @RequiredRole(roles = USER)
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public GatewayShortDto createGateway(@RequestBody CreateGatewayRequest request)
            throws AuthenticationException, ValidationException {
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
    public void deleteGateway(@PathVariable String id) throws Exception {
        gatewayService.deleteGateway(id);
    }


    @RequiredRole(roles = USER)
    @GetMapping("/list")
    public List<GatewayDto> getList() throws AuthenticationException {
        List<GatewayEntity> gateways = gatewayService.getUserGateways();
        return gatewayMapper.toDto(gateways).stream().peek(
            gatewayDto -> gatewayDto.setOnline(userRegistry.getUser(gatewayDto.getId()) != null)
        ).collect(Collectors.toList());
    }

    @RequiredRole(roles = USER)
    @GetMapping("/{id}")
    public GatewayDto getById(@PathVariable String id) throws AccessDeniedException, ValidationException, AuthenticationException {
        return gatewayMapper.toDto(gatewayService.getGateway(id));
    }
}
