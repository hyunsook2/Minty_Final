package com.Reboot.Minty.review.dto;

import com.Reboot.Minty.member.entity.User;
import com.Reboot.Minty.trade.entity.Trade;
import com.Reboot.Minty.tradeBoard.entity.TradeBoard;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;


@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto {
    private Long id;
    private String writer;
    private String imageUrl; // 이미지 파일 URL 추가
    private String contents;
    private int rating;
    private TradeBoard tradeBoard;
    private MultipartFile imageFile;
    private String nickname;
    private User buyerId;
    private User writerId;
    private User sellerId;
    private String createdAt;
    private Trade trade;

    public TradeBoard getTradeBoard() {
        if (tradeBoard == null) {
            return null; // 또는 다른 값 또는 처리 로직을 작성하세요.
        }
        return tradeBoard;
    }

}