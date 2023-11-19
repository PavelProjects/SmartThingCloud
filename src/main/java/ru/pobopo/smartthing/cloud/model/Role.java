package ru.pobopo.smartthing.cloud.model;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public enum Role {
    ADMIN("admin"),
    USER("user"),
    GATEWAY("gateway");

    private final String name;
    Role(String name) {
        this.name = name;
    }

    public static Role fromString(String type) {
        if (StringUtils.isBlank(type)) {
            return null;
        }
        for (Role iterate: values()) {
            if (StringUtils.equals(iterate.getName(), type)) {
                return iterate;
            }
        }
        return null;
    }

    public static class Constants {
        public static final String USER = "user";
        public static final String GATEWAY = "gateway";
        public static final String ADMIN = "admin";
    }
}
