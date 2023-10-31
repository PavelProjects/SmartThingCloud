package ru.pobopo.smartthing.cloud.rabbitmq;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.entity.GatewayRequestEntity;
import ru.pobopo.smartthing.cloud.mapper.GatewayRequestMapper;
import ru.pobopo.smartthing.cloud.repository.GatewayRequestRepository;

@Component
@Slf4j
public class GatewayResponseProcessorImpl implements GatewayResponseProcessor {
    private final GatewayRequestRepository requestRepository;
    private final SimpMessagingTemplate stompService;
    private final GatewayRequestMapper requestMapper;

    @Autowired
    public GatewayResponseProcessorImpl(
        GatewayRequestRepository requestRepository,
        SimpMessagingTemplate stompService, GatewayRequestMapper requestMapper
    ) {
        this.requestRepository = requestRepository;
        this.stompService = stompService;
        this.requestMapper = requestMapper;
    }

    @Override
    public void process(MessageResponse response) {
        Objects.requireNonNull(response);
        if (StringUtils.isBlank(response.getRequestId())) {
            throw new RuntimeException("Request id is missing!");
        }
        Optional<GatewayRequestEntity> requestEntity = requestRepository.findById(response.getRequestId());
        if (requestEntity.isEmpty()) {
            throw new RuntimeException("Can't find request entity with id " + response.getRequestId());
        }

        GatewayRequestEntity entity = requestEntity.get();
        entity.setResult(response.getResponse());
        entity.setSuccess(response.isSuccess());
        entity.setFinished(true);
        entity.setReceiveDate(LocalDateTime.now());
        requestRepository.save(entity);

        String topic = buildTopicName(entity);
        log.info("Sending response info to {}", topic);
        stompService.convertAndSend(
            topic,
            requestMapper.toDto(entity)
        );
    }

    private static String buildTopicName(GatewayRequestEntity entity) {
        return "/response/" + entity.getUser().getLogin();
    }
}
