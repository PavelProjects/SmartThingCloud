package ru.pobopo.smartthing.cloud.rabbitmq;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class MessageConsumer extends DefaultConsumer {
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final Consumer<MessageResponse> callback;

    public MessageConsumer(Channel channel, Consumer<MessageResponse> callback) {
        super(channel);
        this.callback = callback;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
        throws IOException {
        String message = new String(body);
        if (StringUtils.isBlank(message)) {
            log.error("[{}] Empty message!", consumerTag);
        }
        try {
            log.info("[{}] Processing response {}", consumerTag, message);
            callback.accept(objectMapper.readValue(message, MessageResponse.class));
            log.info("[{}] Response processing finshed!", consumerTag);
            getChannel().basicAck(envelope.getDeliveryTag(), true);
        } catch (Exception e) {
            log.error("[{}] Callback failed: {}", consumerTag, e.getMessage());
            getChannel().basicAck(envelope.getDeliveryTag(), false);
        }
    }
}
