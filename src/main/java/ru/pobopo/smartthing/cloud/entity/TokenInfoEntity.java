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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = TokenInfoEntity.TYPE)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TokenInfoEntity {
    public static final String TYPE = "smt_token_info";

    @Id
    @GenericGenerator(name = "entity_id", strategy = "ru.pobopo.smartthing.cloud.entity.EntityIdGenerator")
    @GeneratedValue(generator = "entity_id")
    @Column
    private String id;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate = LocalDateTime.now();

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "deactivation_date")
    private LocalDateTime deactivationDate;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", referencedColumnName = "id")
    private UserEntity owner;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gateway_id", referencedColumnName = "id")
    private GatewayEntity gateway;
}
