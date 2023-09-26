package ru.pobopo.smartthing.cloud.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import ru.pobopo.smartthing.cloud.dto.GatewayShortDto;
import ru.pobopo.smartthing.cloud.dto.GatewayDto;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;

@Mapper(componentModel = "spring")
public interface GatewayMapper {
    GatewayShortDto toShortDto(GatewayEntity gatewayEntity);
    List<GatewayShortDto> toShortDto(List<GatewayEntity> gatewayEntities);

    GatewayDto toDto(GatewayEntity gatewayEntity);
    List<GatewayDto> toDto(List<GatewayEntity> gatewayEntity);
}
