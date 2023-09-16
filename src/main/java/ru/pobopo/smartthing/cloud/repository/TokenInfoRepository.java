package ru.pobopo.smartthing.cloud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.TokenInfoEntity;
import ru.pobopo.smartthing.cloud.entity.UserEntity;

@Repository
public interface TokenInfoRepository extends JpaRepository<TokenInfoEntity, String> {
    TokenInfoEntity findByIdAndType(String id, String type);
    TokenInfoEntity findByActiveAndOwnerAndGatewayIsNull(boolean active, UserEntity owner);
    TokenInfoEntity findByActiveAndGateway(boolean active, GatewayEntity gatewayEntity);
}
