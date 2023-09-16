package ru.pobopo.smartthing.cloud;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.pobopo.smartthing.cloud.exception.ValidationException;
import ru.pobopo.smartthing.cloud.service.UserService;

@SpringBootApplication
public class SmartThingCloudApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartThingCloudApplication.class, args);
    }

    @Bean
    CommandLineRunner run(UserService userService) {
        return args -> {
            try {
                userService.createUser("test_user", "1");
            } catch (ValidationException exception) {}
        };
    }
}
