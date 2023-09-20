package ru.pobopo.smartthing.cloud.service.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GatewayCommand extends BaseMessage {
    private String command;
    private Map<String, Object> parameters;
}
