package ru.pobopo.smartthing.cloud.mapper;

import org.mapstruct.Mapper;
import ru.pobopo.smartthing.cloud.dto.AuthorizedUserDto;
import ru.pobopo.smartthing.cloud.model.AuthorizedUser;

@Mapper(componentModel = "spring", uses = {GatewayMapper.class, UserMapper.class})
public interface AuthorizedUserMapper {
    AuthorizedUserDto toDto(AuthorizedUser authorizedUser);
}
