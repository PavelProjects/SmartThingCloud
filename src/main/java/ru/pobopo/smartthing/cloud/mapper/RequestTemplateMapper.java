package ru.pobopo.smartthing.cloud.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import ru.pobopo.smartthing.cloud.dto.RequestTemplateDto;
import ru.pobopo.smartthing.cloud.entity.RequestTemplateEntity;

@Mapper(componentModel = "spring")
public interface RequestTemplateMapper {
    RequestTemplateDto toDto(RequestTemplateEntity entity);
    List<RequestTemplateDto> toDto(List<RequestTemplateEntity> entity);

    RequestTemplateEntity toEntity(RequestTemplateDto dto);
}
