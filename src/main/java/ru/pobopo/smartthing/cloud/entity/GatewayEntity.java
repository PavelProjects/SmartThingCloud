package ru.pobopo.smartthing.cloud.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = GatewayEntity.TYPE)
@Data
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

    @Column(name = "queue_in_name")
    private String queueIn;

    @Column(name = "queue_out_name")
    private String queueOut;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinTable(
        name = "smt_gateway_owner",
        joinColumns = @JoinColumn(
            name = "gateway_id", referencedColumnName = "id"),
        inverseJoinColumns = @JoinColumn(
            name = "user_id", referencedColumnName = "id"))
    private UserEntity owner;
}
