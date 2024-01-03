package ru.pobopo.smartthing.cloud.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Data
@Entity
@Table(name = RequestTemplateEntity.TABLE_NAME)
public class RequestTemplateEntity {
    public static final String TABLE_NAME = "smt_device_request_template";

    @Id
    @GenericGenerator(name = "entity_id", strategy = "ru.pobopo.smartthing.cloud.entity.EntityIdGenerator")
    @GeneratedValue(generator = "entity_id")
    @Column
    private String id;

    @Column
    private String name;

    @Column(nullable = false)
    private String path;

    @Column
    private String method;

    @Column
    private String payload;

    @Column(name = "supported_version")
    private String supportedVersion;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", referencedColumnName = "id")
    private UserEntity owner;
}
