package ru.pobopo.smartthing.cloud.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import ru.pobopo.smartthing.cloud.dto.GatewayRequestDto;
import ru.pobopo.smartthing.cloud.entity.GatewayRequestEntity;

@Mapper(componentModel = "spring", uses = {GatewayMapper.class, UserMapper.class})
public interface GatewayRequestMapper {
    GatewayRequestDto toDto(GatewayRequestEntity entity);
    List<GatewayRequestDto> toDto(List<GatewayRequestEntity> entity);
}
