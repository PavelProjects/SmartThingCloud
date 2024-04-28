package ru.pobopo.smartthing.cloud.mapper;

import org.mapstruct.Mapper;
import ru.pobopo.smartthing.cloud.dto.GatewayDto;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GatewayMapper {
    GatewayDto toDto(GatewayEntity gatewayEntity);
    List<GatewayDto> toDto(List<GatewayEntity> gatewayEntity);
}
