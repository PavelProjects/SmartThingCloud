package ru.pobopo.smartthing.cloud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GatewayQueueInfo {
    private String queueIn;
    private String queueOut;
}
