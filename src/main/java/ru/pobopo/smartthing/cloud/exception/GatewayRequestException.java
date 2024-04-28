package ru.pobopo.smartthing.cloud.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.pobopo.smartthing.model.stomp.ResponseMessage;

@Getter
@RequiredArgsConstructor
public class GatewayRequestException extends Exception {
    private final ResponseMessage responseMessage;
}
