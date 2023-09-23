package ru.pobopo.smartthing.cloud.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.pobopo.smartthing.cloud.entity.RequestTemplateEntity;
import ru.pobopo.smartthing.cloud.entity.UserEntity;

@Repository
public interface RequestTemplateRepository extends JpaRepository<RequestTemplateEntity, String> {
    List<RequestTemplateEntity> findByOwnerOrOwnerIsNull(UserEntity owner);
}
