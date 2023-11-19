package ru.pobopo.smartthing.cloud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.pobopo.smartthing.cloud.entity.GatewayConfigEntity;

@Repository
public interface GatewayConfigRepository extends JpaRepository<GatewayConfigEntity, String> {
    GatewayConfigEntity findByGatewayId(String id);
}
