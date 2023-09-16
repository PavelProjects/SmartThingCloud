package ru.pobopo.smartthing.cloud.controller.model;

import lombok.Data;

@Data
public class AuthRequest {
    private String login;
    private String password;
}
