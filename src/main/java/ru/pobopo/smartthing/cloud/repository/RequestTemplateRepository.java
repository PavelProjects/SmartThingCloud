package ru.pobopo.smartthing.cloud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.pobopo.smartthing.cloud.entity.RequestTemplateEntity;

@Repository
public interface RequestTemplateRepository extends JpaRepository<RequestTemplateEntity, String> {
}
