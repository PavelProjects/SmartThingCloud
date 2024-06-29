package ru.pobopo.smartthing.cloud.model;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public enum TokenType {
    USER("user"),
    USER_REFRESH("user_refresh"),
    GATEWAY("gateway");

    private final String name;
    TokenType(String name) {
        this.name = name;
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
