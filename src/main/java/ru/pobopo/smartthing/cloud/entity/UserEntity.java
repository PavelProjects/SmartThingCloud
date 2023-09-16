package ru.pobopo.smartthing.cloud.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = UserEntity.TYPE)
@Data
public class UserEntity {
    public static final String TYPE = "smt_user";

    @Id
    @GenericGenerator(name = "entity_id", strategy = "ru.pobopo.smartthing.cloud.entity.EntityIdGenerator")
    @GeneratedValue(generator = "entity_id")
    @Column
    private String id;

    @Column
    private String login;

    @Column
    private String password;

    @Column(name = "creation_date")
    private LocalDateTime creationDate = LocalDateTime.now();

}
