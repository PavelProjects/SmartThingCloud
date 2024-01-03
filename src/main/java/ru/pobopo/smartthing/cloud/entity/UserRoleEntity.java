package ru.pobopo.smartthing.cloud.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = UserRoleEntity.TABLE_NAME)
@Data
public class UserRoleEntity {
    public static final String TABLE_NAME = "smt_user_role";

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
