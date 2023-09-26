package ru.pobopo.smartthing.cloud.repository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.GatewayRequestEntity;
import ru.pobopo.smartthing.cloud.entity.UserEntity;

@Repository
public interface GatewayRequestRepository extends JpaRepository<GatewayRequestEntity, String> {
    List<GatewayRequestEntity> findByUser(UserEntity user, Pageable pageable);
    GatewayRequestEntity findByUserAndId(UserEntity user, String id);

    long countByFinishedAndTarget(boolean finished, String target);

    long deleteByGateway(GatewayEntity gateway);
}
