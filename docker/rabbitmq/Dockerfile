FROM rabbitmq:3.8-management
COPY rabbitmq.conf /etc/rabbitmq/rabbitmq.config
RUN rabbitmq-plugins enable rabbitmq_management rabbitmq_auth_backend_http