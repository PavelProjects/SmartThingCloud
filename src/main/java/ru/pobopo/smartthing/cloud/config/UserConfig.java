package ru.pobopo.smartthing.cloud.config;

import lombok.Data;
import ru.pobopo.smartthing.cloud.model.Role;

@Data
public class UserConfig {
    private String login;
    private String password;
    private Role role;
}
