package ru.pobopo.smartthing.cloud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = GatewayTokenEntity.TYPE)
@Getter
@Setter
public class GatewayTokenEntity {
    public static final String TYPE = "smt_gateway_token";

    @Id
    @GenericGenerator(name = "entity_id", strategy = "ru.pobopo.smartthing.cloud.entity.EntityIdGenerator")
    @GeneratedValue(generator = "entity_id")
    @Column
    private String id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gateway_id", nullable = false)
    private GatewayEntity gateway;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;
}
