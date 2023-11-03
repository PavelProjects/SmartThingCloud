package ru.pobopo.smartthing.cloud.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.pobopo.smartthing.cloud.controller.model.rabbitmq.BasicCheck;
import ru.pobopo.smartthing.cloud.controller.model.rabbitmq.ResourceCheck;
import ru.pobopo.smartthing.cloud.controller.model.rabbitmq.TopicCheck;
import ru.pobopo.smartthing.cloud.service.RabbitAuthService;

@CrossOrigin
@RestController
@RequestMapping("/rabbit/auth")
@Slf4j
public class RabbitAuthController {
    private final RabbitAuthService authService;

    @Autowired
    public RabbitAuthController(RabbitAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/user")
    public String authUser(@RequestParam("username") String username,
        @RequestParam("password") String password) {
        log.debug("Rabbitmq user: {} {}", username, password);
        return authService.authUser(username, password);
    }

    @PostMapping("vhost")
    public String vhost(BasicCheck check) {
        log.debug("Checking vhost access: {}", check);
        return authService.authVhost(check);
    }

    @PostMapping("resource")
    public String resource(ResourceCheck check) {
        log.debug("Checking resource access: {}", check);
        return authService.authResource(check);
    }

    @PostMapping("topic")
    public String topic(TopicCheck check) {
        log.debug("Checking topic access: {}", check);
        return authService.authTopic(check);
    }
}
