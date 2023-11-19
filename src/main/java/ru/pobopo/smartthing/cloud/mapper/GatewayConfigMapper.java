package ru.pobopo.smartthing.cloud.mapper;

import org.mapstruct.Mapper;
import ru.pobopo.smartthing.cloud.dto.GatewayConfigDto;
import ru.pobopo.smartthing.cloud.entity.GatewayConfigEntity;

@Mapper(componentModel = "spring")
public interface GatewayConfigMapper {
    GatewayConfigDto toDto(GatewayConfigEntity entity);
}
