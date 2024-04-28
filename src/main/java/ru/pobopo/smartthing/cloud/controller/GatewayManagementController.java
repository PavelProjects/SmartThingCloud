package ru.pobopo.smartthing.cloud.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smartthing.cloud.annotation.RequiredRole;
import ru.pobopo.smartthing.cloud.controller.model.CreateGatewayRequest;
import ru.pobopo.smartthing.cloud.dto.GatewayDto;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.GatewayTokenEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.mapper.GatewayMapper;
import ru.pobopo.smartthing.cloud.service.GatewayAuthService;
import ru.pobopo.smartthing.cloud.service.GatewayService;

import javax.naming.AuthenticationException;
import java.util.List;
import java.util.Map;
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
    private final GatewayAuthService gatewayAuthService;

    @RequiredRole(roles = USER)
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public GatewayDto createGateway(@RequestBody CreateGatewayRequest request)
            throws AuthenticationException, ValidationException {
        GatewayEntity gatewayEntity = gatewayService.createGateway(request.getName(), request.getDescription());
        Objects.requireNonNull(gatewayEntity);
        return gatewayMapper.toDto(gatewayEntity);
    }

    @RequiredRole(roles = USER)
    @PutMapping("/update")
    public void updateGateway(@RequestBody GatewayDto dto)
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
        Map<String, GatewayTokenEntity> tokens = gatewayAuthService.getTokens(gateways);
        return gatewayMapper.toDto(gateways).stream().peek(
            gatewayDto -> {
                gatewayDto.setOnline(userRegistry.getUser(gatewayDto.getId()) != null);
                gatewayDto.setHaveToken(tokens.containsKey(gatewayDto.getId()));
            }
        ).collect(Collectors.toList());
    }

    @RequiredRole(roles = USER)
    @GetMapping("/{id}")
    public GatewayDto getById(@PathVariable String id) throws AccessDeniedException, ValidationException, AuthenticationException {
        GatewayEntity gateway = gatewayService.getGateway(id);
        GatewayDto gatewayDto = gatewayMapper.toDto(gateway);
        gatewayDto.setOnline(userRegistry.getUser(gatewayDto.getId()) != null);
        gatewayDto.setHaveToken(gatewayAuthService.getToken(gateway) != null);
        return gatewayDto;
    }
}
