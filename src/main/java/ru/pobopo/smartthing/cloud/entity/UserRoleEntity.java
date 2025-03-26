package ru.pobopo.smartthing.cloud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = UserRoleEntity.TYPE)
@Data
public class UserRoleEntity extends BaseEntity {
    public static final String TYPE = "smt_user_role";

    @Column
    private String role;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "creation_date")
    private LocalDateTime creationDate = LocalDateTime.now();
}
