package ru.pobopo.smartthing.cloud.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = GatewayTokenEntity.TABLE_NAME)
public class GatewayTokenEntity {
    public static final String TABLE_NAME = "smt_gateway_token";

    @Id
    @GenericGenerator(name = "entity_id", strategy = "ru.pobopo.smartthing.cloud.entity.EntityIdGenerator")
    @GeneratedValue(generator = "entity_id")
    @Column
    private String id;

    @Column(name = "token")
    private String token;

    @Column(name = "ttl")
    private Long ttl;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gateway_id", nullable = false)
    private GatewayEntity gateway;

}
