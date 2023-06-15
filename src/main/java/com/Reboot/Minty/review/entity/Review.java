package com.Reboot.Minty.review.entity;

import com.Reboot.Minty.member.entity.User;
import com.Reboot.Minty.trade.entity.Trade;
import com.Reboot.Minty.tradeBoard.entity.TradeBoard;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "review")
@Getter
@Setter
@Builder
@AllArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "writer_id")
    private User writerId;

    @Column(name = "contents", nullable = false)
    private String contents;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "image_url") // 이미지 파일 URL 저장하는 필드 추가
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "buyer_id")
    private User buyerId;

    @ManyToOne
    @JoinColumn(name = "seller_id")
    private User sellerId;

    @Column(name = "nickname")
    private String nickname;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "trade_id")
    private Trade trade;

    @ManyToOne
    @JoinColumn(name = "tradeBoard_id", referencedColumnName = "id")
    private TradeBoard tradeBoard;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Review() {
    }
}
