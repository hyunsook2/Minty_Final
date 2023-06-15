package com.Reboot.Minty.review.service;

import com.Reboot.Minty.member.entity.User;
import com.Reboot.Minty.member.repository.UserRepository;
import com.Reboot.Minty.review.dto.ReviewDto;
import com.Reboot.Minty.review.entity.Review;
import com.Reboot.Minty.review.repository.ReviewRepository;
import com.Reboot.Minty.trade.entity.Trade;
import com.Reboot.Minty.tradeBoard.entity.TradeBoard;
import com.oracle.wls.shaded.org.apache.xpath.operations.Bool;
import jakarta.persistence.EntityExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;

    private UserRepository userRepository;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository,UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;

    }

    //내가 작성한 후기
    public List<Review> getReviewsByUserId(Long userId) {
        return reviewRepository.findByWriterIdIdOrderByCreatedAtDesc(userId);
    }

    //상대방이 작성한 후기
    public List<Review> getReceivedReviews(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return reviewRepository.findByWriterIdNotOrderByCreatedAtDesc(user);
    }


    public void createReview(ReviewDto reviewDto) {
        System.out.println("Creating review: " + reviewDto.getContents());
        // ReviewDto를 Review 엔티티로 변환합니다
        Review review = convertToEntity(reviewDto);

        LocalDateTime currentTime = LocalDateTime.now();
        review.setCreatedAt(currentTime);

        // 리뷰를 리포지토리에 저장합니다
        review = reviewRepository.save(review);
        // 이미지 파일을 저장하고 이미지 파일의 경로를 엔티티에 저장합니다
        String imageUrl = storeImageFile(review.getId(), reviewDto.getImageFile());
        review.setImageUrl(imageUrl);

        // 리뷰를 업데이트합니다 (이미지 파일 경로를 업데이트하기 위해)
        reviewRepository.save(review);
    }


    public void updateReview(Long id, ReviewDto reviewDto) {
        // 리포지토리에서 기존 리뷰를 가져옵니다
        Review existingReview = reviewRepository.findById(id).orElse(null);
        if (existingReview == null) {
            // 리뷰가 존재하지 않는 경우 처리합니다
            return;
        }

        // 기존 리뷰의 속성을 업데이트합니다
//        existingReview.setTitle(reviewDto.getTitle());
        existingReview.setContents(reviewDto.getContents());
        existingReview.setRating(reviewDto.getRating());

        // 이미지 파일을 저장하고 이미지 파일의 경로를 엔티티에 저장합니다
        String imageUrl = storeImageFile(id, reviewDto.getImageFile());
        existingReview.setImageUrl(imageUrl);

        // 업데이트된 리뷰를 리포지토리에 저장합니다
        reviewRepository.save(existingReview);
    }


    public void deleteReview(Long id) {
        // 리포지토리에서 리뷰를 가져옵니다
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) {
            // 리뷰가 존재하지 않는 경우 처리합니다
            return;
        }

        // 리포지토리에서 리뷰를 삭제합니다
        reviewRepository.delete(review);

    }

    private Review convertToEntity(ReviewDto reviewDto) {
        User buyer = reviewDto.getBuyerId();
        User seller = reviewDto.getSellerId();
        User writer = reviewDto.getWriterId();

        if (buyer == null || seller == null || writer == null) {
            // buyerId, sellerId, writerId에 해당하는 User가 존재하지 않는 경우 처리할 로직을 작성하세요.
        }

        LocalDateTime createdAt = LocalDateTime.parse(reviewDto.getCreatedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return Review.builder()
                .contents(reviewDto.getContents())
                .rating(reviewDto.getRating())
                .createdAt(createdAt)
                .buyerId(buyer)
                .sellerId(seller)
                .writerId(writer)
                .nickname(reviewDto.getNickname())
                .trade(reviewDto.getTrade()) // tradeId 설정
                .tradeBoard(reviewDto.getTradeBoard()) // tradeBoard 설정
                .build();
    }


    private ReviewDto convertToDto(Review review) {
        ReviewDto reviewDto = new ReviewDto();
        reviewDto.setContents(review.getContents());
        reviewDto.setNickname(review.getNickname());
        reviewDto.setCreatedAt(review.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        reviewDto.setRating(review.getRating());
        reviewDto.setSellerId(review.getSellerId());
        reviewDto.setBuyerId(review.getBuyerId());
        reviewDto.setWriterId(review.getWriterId());
        reviewDto.setTrade(review.getTrade()); // tradeId 설정

        User buyer = review.getBuyerId();
        if (buyer != null) {
            reviewDto.setBuyerId(buyer);
        } else {
            reviewDto.setBuyerId(null);
        }

        User seller = review.getSellerId();
        if (seller != null) {
            reviewDto.setSellerId(seller);
        } else {
            reviewDto.setSellerId(null);
        }

        User writer = review.getWriterId();
        if (writer != null) {
            reviewDto.setWriterId(writer);
        } else {
            reviewDto.setWriterId(null);
        }

        TradeBoard tradeBoard = review.getTradeBoard();
        if (tradeBoard != null) {
            reviewDto.setTradeBoard(tradeBoard);
        } else {
            reviewDto.setTradeBoard(null);
        }

        String imageUrl = review.getImageUrl();
        reviewDto.setImageUrl(imageUrl);

        return reviewDto;
    }





    private String storeImageFile(Long reviewId, MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }

        try {
            // 이미지 파일을 저장할 디렉토리를 정의합니다
            String storageDirectory = "D:/Reboot/Minty/src/main/resources/static/image/review/";
            File directory = new File(storageDirectory);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 리뷰 ID를 사용하여 고유한 파일명을 생성합니다
            String originalFilename = imageFile.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String uniqueFilename = reviewId + fileExtension;

            // 이미지 파일을 저장 디렉토리에 저장합니다
            File file = new File(storageDirectory + uniqueFilename);
            imageFile.transferTo(file);

            // 저장된 파일 경로를 반환합니다
            return "/image/review/" + uniqueFilename;
        } catch (IOException e) {
            // 파일 저장 중에 오류가 발생한 경우 예외를 처리합니다
            e.printStackTrace();
            return null;
        }
    }

    public boolean existsByIdAndWriterId(Trade tradeId, User writerId){
        return reviewRepository.existsByTradeAndWriterId(tradeId, writerId);
    }
}

