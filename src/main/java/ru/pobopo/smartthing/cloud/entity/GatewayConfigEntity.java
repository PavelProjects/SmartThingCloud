package ru.pobopo.smartthing.cloud.entity;

import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = GatewayConfigEntity.TABLE_NAME)
@Data
@ToString
public class GatewayConfigEntity {
    public static final String TABLE_NAME = "smt_gateway_config";

    @Id
    @GenericGenerator(name = "entity_id", strategy = "ru.pobopo.smartthing.cloud.entity.EntityIdGenerator")
    @GeneratedValue(generator = "entity_id")
    @Column
    private String id;

    @Column(name = "broker_ip")
    private String brokerIp;

    @Column(name = "broker_port")
    private int brokerPort;

    @Column(name = "queue_in")
    private String queueIn;

    @Column(name = "queue_out")
    private String queueOut;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gateway_id", referencedColumnName = "id")
    private GatewayEntity gateway;
}
