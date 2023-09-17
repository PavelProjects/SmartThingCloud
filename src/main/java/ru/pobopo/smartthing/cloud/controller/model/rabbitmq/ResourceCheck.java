package ru.pobopo.smartthing.cloud.controller.model.rabbitmq;

import lombok.Data;

@Data
public class ResourceCheck extends BasicCheck {
    private String resource;
    private String name;
    private String permission;
}
