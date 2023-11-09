package ru.pobopo.smartthing.cloud;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.service.GatewayBrokerService;
import ru.pobopo.smartthing.cloud.service.UserService;
import ru.pobopo.smartthing.cloud.service.impl.AuthorisationUtils;

@SpringBootApplication
public class SmartThingCloudApplication {
    public static final String VERSION = "1.0";

    public static void main(String[] args) {
        SpringApplication.run(SmartThingCloudApplication.class, args);
    }

    @Bean
    CommandLineRunner run(Environment environment, UserService userService, GatewayBrokerService gatewayMessagingService)  {
        return args -> {
            String login = environment.getProperty("admin.login", "admin");
            String password = environment.getProperty("admin.password", "admin");

            UserEntity adminUser = userService.createUser(login, password);
            userService.grantUserRole(adminUser, AuthorisationUtils.ADMIN_ROLE);

            gatewayMessagingService.addResponseListeners();
        };
    }
}
