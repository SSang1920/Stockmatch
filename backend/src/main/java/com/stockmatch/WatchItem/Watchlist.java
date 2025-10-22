package com.stockmatch.WatchItem;

import com.stockmatch.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "watchlist")
public class Watchlist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name ="watchlist_id")
    private long id;

    @Column(length = 30)
    private String name;

    @OneToMany(mappedBy = "watchlist",fetch = FetchType.LAZY)
    private List<WatchItem> watchItems = new ArrayList<>();
}
