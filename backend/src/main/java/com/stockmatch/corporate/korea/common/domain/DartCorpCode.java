package com.stockmatch.corporate.korea.common.domain;

import com.stockmatch.stock.domain.Security;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DartCorpCode {

    @Id
    @Column(name = "security_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String ticker; //securiy의 ticker

    @Column(nullable = false, length = 8)
    private String corpCode; //DART 8자리 고유번호

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId //ticker를 PK&FK로 이용
    @JoinColumn(name = "security_id")
    private Security security;
}
