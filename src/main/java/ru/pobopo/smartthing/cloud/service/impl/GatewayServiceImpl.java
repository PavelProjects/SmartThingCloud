package ru.pobopo.smartthing.cloud.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javax.naming.AuthenticationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.cloud.repository.UserRepository;
import ru.pobopo.smartthing.cloud.service.GatewayService;

@Component
public class GatewayServiceImpl implements GatewayService {
    private final GatewayRepository gatewayRepository;
    private final UserRepository userRepository;

    @Autowired
    public GatewayServiceImpl(GatewayRepository gatewayRepository, UserRepository userRepository) {
        this.gatewayRepository = gatewayRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public GatewayEntity createGateway(String name, String description)
        throws AuthenticationException, ValidationException {
        if (StringUtils.isBlank(name)) {
            throw new ValidationException("Gateway name can't be empty!");
        }
        if (findUserGateway(name) != null) {
            throw new ValidationException("Current user already have gateway with name " + name);
        }

        GatewayEntity gatewayEntity = new GatewayEntity();
        gatewayEntity.setOwner(userRepository.findByLogin(AuthoritiesService.getCurrentUserLogin()));
        gatewayEntity.setName(name);
        gatewayEntity.setDescription(description);
        gatewayEntity.setCreationDate(LocalDateTime.now());

        gatewayRepository.save(gatewayEntity);
        return gatewayEntity;
    }

    @Override
    public List<GatewayEntity> getUserGateways() throws AuthenticationException {
        return gatewayRepository.findByOwnerLogin(AuthoritiesService.getCurrentUserLogin());
    }

    @Override
    public GatewayEntity findUserGateway(String name) throws AuthenticationException {
        return gatewayRepository.findByNameAndOwnerLogin(name, AuthoritiesService.getCurrentUserLogin());
    }

    @Override
    public Optional<GatewayEntity> getGateway(String id) {
        return gatewayRepository.findById(id);
    }
}
