package ru.pobopo.smartthing.cloud.model;

import lombok.Data;
import ru.pobopo.smartthing.model.stomp.ResponseMessage;

@Data
public class ResponseMessageHolder {
    private ResponseMessage responseMessage;
}
