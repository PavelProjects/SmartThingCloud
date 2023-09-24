package ru.pobopo.smartthing.cloud.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

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
