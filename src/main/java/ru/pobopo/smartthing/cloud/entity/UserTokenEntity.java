package ru.pobopo.smartthing.cloud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Entity
@Table(name = UserTokenEntity.TYPE)
@Getter
@Setter
@ToString
public class UserTokenEntity extends BaseEntity {
    public static final String TYPE = "smt_user_token";

    public final static String CLAIM_TOKEN_ID = "token_id";
    public final static String CLAIM_CREATION_DATE = "creation_date";

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

    public Map<String, Object> toClaims() {
        return Map.of(
                CLAIM_TOKEN_ID, getId(),
                CLAIM_CREATION_DATE, creationDate.format(DateTimeFormatter.ISO_DATE_TIME)
        );
    }
}
