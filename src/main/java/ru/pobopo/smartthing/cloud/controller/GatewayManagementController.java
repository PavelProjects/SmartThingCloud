package ru.pobopo.smartthing.cloud.controller;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import javax.naming.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smartthing.cloud.controller.model.CreateGatewayRequest;
import ru.pobopo.smartthing.cloud.controller.model.GenerateTokenRequest;
import ru.pobopo.smartthing.cloud.controller.model.TokenResponse;
import ru.pobopo.smartthing.cloud.dto.GatewayDto;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.mapper.GatewayMapper;
import ru.pobopo.smartthing.cloud.service.GatewayService;
import ru.pobopo.smartthing.cloud.service.TokenService;
import ru.pobopo.smartthing.cloud.service.impl.AuthoritiesService;

@RestController
@RequestMapping("/gateway/management")
public class GatewayManagementController {
    private final GatewayService gatewayService;
    private final GatewayMapper gatewayMapper;
    private final TokenService tokenService;

    @Autowired
    public GatewayManagementController(
        GatewayService gatewayService,
        GatewayMapper gatewayMapper,
        TokenService tokenService
    ) {
        this.gatewayService = gatewayService;
        this.gatewayMapper = gatewayMapper;
        this.tokenService = tokenService;
    }

    @PutMapping("/token/generate")
    public TokenResponse generateToken(@RequestBody GenerateTokenRequest request)
        throws ValidationException, AuthenticationException {
        return new TokenResponse(
            tokenService.generateToken(AuthoritiesService.getCurrentUserLogin(), request.getGatewayId())
        );
    }

    @PostMapping("/create")
    public GatewayDto createGateway(@RequestBody CreateGatewayRequest request)
        throws AuthenticationException, ValidationException, IOException, TimeoutException {
        GatewayEntity gatewayEntity = gatewayService.createGateway(request.getName(), request.getDescription());
        Objects.requireNonNull(gatewayEntity);
        return gatewayMapper.toDto(gatewayEntity);
    }

    @GetMapping("/list")
    public List<GatewayDto> getList() throws AuthenticationException {
        List<GatewayEntity> gateways = gatewayService.getUserGateways();
        Objects.requireNonNull(gateways);
        return gatewayMapper.toDto(gateways);
    }
}
