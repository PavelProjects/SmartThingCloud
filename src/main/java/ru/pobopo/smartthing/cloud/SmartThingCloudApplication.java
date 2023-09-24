package ru.pobopo.smartthing.cloud;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.service.GatewayBrokerService;
import ru.pobopo.smartthing.cloud.service.UserService;
import ru.pobopo.smartthing.cloud.service.impl.AuthoritiesService;

@SpringBootApplication
public class SmartThingCloudApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartThingCloudApplication.class, args);
    }

    @Bean
    CommandLineRunner run(UserService userService, GatewayBrokerService gatewayMessagingService)  {
        return args -> {
            userService.createUser("test_user", "1");

            UserEntity adminUser = userService.createUser("admin", "admin");
            userService.grantUserRole(adminUser, AuthoritiesService.ADMIN_ROLE);

            gatewayMessagingService.addResponseListeners();
        };
    }
}
