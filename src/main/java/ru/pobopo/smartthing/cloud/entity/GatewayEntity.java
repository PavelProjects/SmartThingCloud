package ru.pobopo.smartthing.cloud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = GatewayEntity.TYPE)
@Getter
@Setter
public class GatewayEntity {
    public static final String TYPE = "smt_gateway";

    @Id
    @GenericGenerator(name = "entity_id", strategy = "ru.pobopo.smartthing.cloud.entity.EntityIdGenerator")
    @GeneratedValue(generator = "entity_id")
    @Column
    private String id;

    @Column
    private String name;

    @Column
    private String description;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinTable(
        name = "smt_gateway_owner",
        joinColumns = @JoinColumn(
            name = "gateway_id", referencedColumnName = "id"),
        inverseJoinColumns = @JoinColumn(
            name = "user_id", referencedColumnName = "id")
    )
    private UserEntity owner;

    @Override
    public String toString() {
        return String.format(
                "(GatewayEntity id=%s, name=%s, description=%s)",
                id,
                name,
                description
        );
    }
}
