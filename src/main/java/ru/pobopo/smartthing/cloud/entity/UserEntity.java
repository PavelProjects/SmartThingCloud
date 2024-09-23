package ru.pobopo.smartthing.cloud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = UserEntity.TYPE)
@Getter
@Setter
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


    @Override
    public String toString() {
        return String.format(
            "User(id=%s, login=%s)",
            id, login
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        UserEntity user = (UserEntity) obj;
        return StringUtils.equals(user.getId(), getId())
            && StringUtils.equals(user.getLogin(), getLogin());
    }
}
