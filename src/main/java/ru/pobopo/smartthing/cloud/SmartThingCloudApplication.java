package ru.pobopo.smartthing.cloud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.pobopo.smartthing.cloud.config.UserConfig;
import ru.pobopo.smartthing.cloud.config.UsersSettings;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.service.UserService;

import java.util.List;

@Slf4j
@SpringBootApplication
public class SmartThingCloudApplication {
    public static final String VERSION = "1.0";

    public static void main(String[] args) {
        SpringApplication.run(SmartThingCloudApplication.class, args);
    }

    @Bean
    CommandLineRunner run(UserService userService, UsersSettings usersSettings)  {
        return args -> {
            List<UserConfig> users = usersSettings.getUsers();
            if (users == null || users.isEmpty()) {
                log.warn("No users provided");
                return;
            }
            users.forEach((user) -> {
                try {
                    UserEntity userEntity = userService.createUser(user.getLogin(), user.getPassword());
                    userService.grantUserRole(userEntity, user.getRole().getName());
                    log.info("Added user login={}, id={}", userEntity.getLogin(), userEntity.getId());
                } catch (Exception exception) {
                    log.error("Failed to create user {}: {}", user.getLogin(), exception.getMessage(), exception);
                }
            });

        };
    }
}
