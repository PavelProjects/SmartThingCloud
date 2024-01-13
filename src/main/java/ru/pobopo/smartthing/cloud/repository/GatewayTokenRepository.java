package ru.pobopo.smartthing.cloud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.GatewayTokenEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface GatewayTokenRepository extends JpaRepository<GatewayTokenEntity, String> {
    Optional<GatewayTokenEntity> findByGateway(GatewayEntity gateway);
    List<GatewayTokenEntity> findByGatewayIn(List<GatewayEntity> gateway);
}
