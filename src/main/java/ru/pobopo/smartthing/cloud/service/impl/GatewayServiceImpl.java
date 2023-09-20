package ru.pobopo.smartthing.cloud.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import javax.naming.AuthenticationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.pobopo.smartthing.cloud.context.ContextHolder;
import ru.pobopo.smartthing.cloud.dto.GatewayQueueInfo;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.repository.GatewayRepository;
import ru.pobopo.smartthing.cloud.repository.UserRepository;
import ru.pobopo.smartthing.cloud.service.GatewayService;

@Component
public class GatewayServiceImpl implements GatewayService {
    private final GatewayRepository gatewayRepository;
    private final UserRepository userRepository;
    private final MessageBrokerServiceImpl messageBrokerService;

    @Autowired
    public GatewayServiceImpl(
        GatewayRepository gatewayRepository,
        UserRepository userRepository,
        MessageBrokerServiceImpl messageBrokerService
    ) {
        this.gatewayRepository = gatewayRepository;
        this.userRepository = userRepository;
        this.messageBrokerService = messageBrokerService;
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
    public GatewayQueueInfo getQueueInfo() throws ValidationException, IOException, TimeoutException {
        GatewayEntity entity = ContextHolder.getCurrentGateway();
        if (entity == null) {
            throw new ValidationException("No gateway! Wrong token?");
        }
        if (StringUtils.isBlank(entity.getQueueInName()) || StringUtils.isBlank(entity.getQueueOutName())) {
            String prefix = entity.getId() + "_" + entity.getName();
            entity.setQueueInName(prefix + "_in");
            entity.setQueueOutName(prefix + "_out");
            gatewayRepository.save(entity);
        }
        messageBrokerService.createQueues(entity);
        return new GatewayQueueInfo(entity.getQueueInName(), entity.getQueueOutName());
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
