package ru.pobopo.smartthing.cloud.mapper;

import org.mapstruct.Mapper;
import ru.pobopo.smartthing.cloud.controller.dto.UserDto;
import ru.pobopo.smartthing.cloud.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(UserEntity userEntity);
    UserEntity fromDto(UserDto userDto);
}
