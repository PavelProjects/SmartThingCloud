package ru.pobopo.smartthing.cloud.dto;

import lombok.Data;

@Data
public class RequestTemplateDto {
    private String id;
    private String path;
    private String method;
    private String payload;
    private String supportedVersion;
}
