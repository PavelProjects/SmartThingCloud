package ru.pobopo.smartthing.cloud.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.UserEntity;

@Repository
public interface GatewayRepository extends JpaRepository<GatewayEntity, String> {
    GatewayEntity findByNameAndOwnerLogin(String name, String ownerLogin);
    List<GatewayEntity> findByOwnerLogin(String ownerLogin);
}
