package ru.pobopo.smartthing.cloud.context;

import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.jwt.TokenType;

public class ContextHolder {
    private static GatewayEntity gatewayEntity;
    private static TokenType tokenType;
    private static String tokenId;

    public static void clearContext() {
        ContextHolder.gatewayEntity = null;
        ContextHolder.tokenType = null;
        ContextHolder.tokenId = null;
    }

    public static void setCurrentGateway(GatewayEntity gatewayEntity) {
        ContextHolder.gatewayEntity = gatewayEntity;
    }

    public static GatewayEntity getCurrentGateway() {
        return ContextHolder.gatewayEntity;
    }

    public static TokenType getTokenType() {
        return tokenType;
    }

    public static void setTokenType(TokenType tokenType) {
        ContextHolder.tokenType = tokenType;
    }

    public static String getTokenId() {
        return tokenId;
    }

    public static void setTokenId(String tokenId) {
        ContextHolder.tokenId = tokenId;
    }
}
