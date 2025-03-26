package ru.pobopo.smartthing.cloud.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = GatewayTokenEntity.TYPE)
@Getter
@Setter
public class GatewayTokenEntity extends BaseEntity {
    public static final String TYPE = "smt_gateway_token";

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gateway_id", nullable = false)
    private GatewayEntity gateway;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;
}
