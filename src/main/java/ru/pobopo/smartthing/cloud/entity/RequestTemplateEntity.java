package ru.pobopo.smartthing.cloud.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

@Data
@Entity
@Table(name = RequestTemplateEntity.TYPE)
public class RequestTemplateEntity {
    public static final String TYPE = "smt_device_request_template";

    @Id
    @GenericGenerator(name = "entity_id", strategy = "ru.pobopo.smartthing.cloud.entity.EntityIdGenerator")
    @GeneratedValue(generator = "entity_id")
    @Column
    private String id;

    @Column(nullable = false)
    private String path;

    @Column
    private String method;

    @Column
    private String payload;

    @Column(name = "supported_version")
    private String supportedVersion;
}
