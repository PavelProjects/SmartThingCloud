package ru.pobopo.smartthing.cloud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface GatewayRepository extends JpaRepository<GatewayEntity, String> {
    Optional<GatewayEntity> findByNameAndOwnerLogin(String name, String ownerLogin);
    List<GatewayEntity> findByOwnerLogin(String ownerLogin);
}
