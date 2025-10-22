package com.stockmatch.WatchItem;

import com.stockmatch.common.BaseEntity;
import com.stockmatch.securityasset.domain.Security;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "watchItem")
public class WatchItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "watchItem_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watchlist_id")
    private Watchlist watchlist;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "security_id")
    private Security security;

    private String note;

    private String orderNo;

}
