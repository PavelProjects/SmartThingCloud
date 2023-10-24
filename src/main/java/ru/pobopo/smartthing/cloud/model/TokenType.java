package ru.pobopo.smartthing.cloud.model;

import org.apache.commons.lang3.StringUtils;

public enum TokenType {
    USER("user_token"),
    GATEWAY("gateway_token");

    private String name;
    TokenType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static TokenType fromString(String type) {
        if (StringUtils.isBlank(type)) {
            return null;
        }
        for (TokenType iterate: values()) {
            if (StringUtils.equals(iterate.getName(), type)) {
                return iterate;
            }
        }
        return null;
    }
}
