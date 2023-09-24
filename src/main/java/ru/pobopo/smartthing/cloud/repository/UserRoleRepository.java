package ru.pobopo.smartthing.cloud.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.entity.UserRoleEntity;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, String> {
    List<UserRoleEntity> findByUserId(String userId);
    UserRoleEntity findByUserIdAndRole(String userId, String role);
}
