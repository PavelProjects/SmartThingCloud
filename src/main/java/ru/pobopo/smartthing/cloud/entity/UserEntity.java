package ru.pobopo.smartthing.cloud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

@Data
@Entity
@SuperBuilder
@Table(name = UserEntity.TYPE)
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity extends BaseEntity {
    public static final String TYPE = "smt_user";

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
            getId(), login
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
