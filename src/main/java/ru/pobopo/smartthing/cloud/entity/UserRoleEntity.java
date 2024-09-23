package ru.pobopo.smartthing.cloud.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = UserRoleEntity.TYPE)
@Data
public class UserRoleEntity {
    public static final String TYPE = "smt_user_role";

    @Id
    @GenericGenerator(name = "entity_id", strategy = "ru.pobopo.smartthing.cloud.entity.EntityIdGenerator")
    @GeneratedValue(generator = "entity_id")
    @Column
    private String id;

    @Column
    private String role;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "creation_date")
    private LocalDateTime creationDate = LocalDateTime.now();
}
