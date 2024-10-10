package ru.pobopo.smartthing.cloud.mapper;

import org.mapstruct.Mapper;
import ru.pobopo.smartthing.cloud.controller.dto.AuthorizedUserDto;
import ru.pobopo.smartthing.cloud.model.AuthenticatedUser;

@Mapper(componentModel = "spring", uses = {GatewayMapper.class, UserMapper.class})
public interface AuthorizedUserMapper {
    AuthorizedUserDto toDto(AuthenticatedUser authenticatedUser);
}
