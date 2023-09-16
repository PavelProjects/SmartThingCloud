package ru.pobopo.smartthing.cloud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.pobopo.smartthing.cloud.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    UserEntity findByLogin(String login);
}
