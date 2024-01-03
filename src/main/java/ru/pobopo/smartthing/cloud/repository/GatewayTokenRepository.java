package ru.pobopo.smartthing.cloud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.GatewayTokenEntity;

@Repository
public interface GatewayTokenRepository extends JpaRepository<GatewayTokenEntity, String> {
    GatewayTokenEntity findByGateway(GatewayEntity gateway);
}
