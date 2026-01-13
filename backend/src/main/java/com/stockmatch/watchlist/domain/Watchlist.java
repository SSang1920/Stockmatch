package com.stockmatch.watchlist.domain;

import com.stockmatch.common.BaseEntity;
import com.stockmatch.user.member.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "watchlist")
public class Watchlist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name ="watchlist_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 30, nullable = false)
    private String name;

    @Builder.Default
    @OneToMany(mappedBy = "watchlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WatchlistItem> watchlistItems = new ArrayList<>();

    @Column(name = "order_no")
    private Integer orderNo;

    // ===== 편의 메서드 =====
    public void updateName(String name) {
        this.name = name;
    }

    public void updateOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    // ===== 연관관계 편의 메서드 =====
    public void addItem(WatchlistItem item) {
        this.watchlistItems.add(item);
        item.setWatchlist(this);
    }
}
