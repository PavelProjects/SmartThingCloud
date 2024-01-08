package ru.pobopo.smartthing.cloud;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.model.Role;
import ru.pobopo.smartthing.cloud.service.UserService;

@Slf4j
@SpringBootApplication
public class SmartThingCloudApplication {
    public static final String VERSION = "1.0";

    public static void main(String[] args) {
        SpringApplication.run(SmartThingCloudApplication.class, args);
    }

    @Bean
    CommandLineRunner run(Environment environment, UserService userService)  {
        return args -> {
            String login = environment.getProperty("admin.login", "admin");
            String password = environment.getProperty("admin.password", "admin");

            if (StringUtils.isBlank(login)) {
                return;
            }

            UserEntity adminUser = userService.createUser(login, password);
            userService.grantUserRole(adminUser, Role.ADMIN.getName());
        };
    }
}
