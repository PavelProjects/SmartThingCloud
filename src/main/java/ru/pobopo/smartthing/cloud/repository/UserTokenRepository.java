package ru.pobopo.smartthing.cloud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.entity.UserTokenEntity;

import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserTokenEntity, String> {
    Optional<UserTokenEntity> findByUser(UserEntity user);
}
