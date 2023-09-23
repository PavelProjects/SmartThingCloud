package ru.pobopo.smartthing.cloud.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

@Data
@Entity
@Table(name = GatewayRequestEntity.TYPE)
public class GatewayRequestEntity {
    public static final String TYPE = "smt_gateway_request";

    @Id
    @GenericGenerator(name = "entity_id", strategy = "ru.pobopo.smartthing.cloud.entity.EntityIdGenerator")
    @GeneratedValue(generator = "entity_id")
    @Column
    private String id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gateway_id", nullable = false)
    private GatewayEntity gateway;

    @Column(name = "sent_date", nullable = false)
    private LocalDateTime sentDate = LocalDateTime.now();

    @Column(name = "receive_date")
    private LocalDateTime receiveDate;

    @Column
    private String message;

    @Column
    private String result;

    @Column
    private boolean finished = false;

    @Column
    private boolean success;
}
