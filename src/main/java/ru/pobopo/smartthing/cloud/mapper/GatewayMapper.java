package ru.pobopo.smartthing.cloud.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import ru.pobopo.smartthing.cloud.dto.GatewayDto;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;

@Mapper(componentModel = "spring")
public interface GatewayMapper {
    GatewayDto toDto(GatewayEntity gatewayEntity);
    GatewayEntity fromDto(GatewayDto gatewayDto);

    List<GatewayDto> toDto(List<GatewayEntity> gatewayEntities);
}
