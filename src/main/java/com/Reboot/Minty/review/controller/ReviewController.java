package com.Reboot.Minty.review.controller;

import com.Reboot.Minty.member.entity.User;
import com.Reboot.Minty.member.service.UserService;
import com.Reboot.Minty.review.dto.ReviewDto;
import com.Reboot.Minty.review.entity.Review;
import com.Reboot.Minty.review.service.ReviewService;
import com.Reboot.Minty.trade.entity.Trade;
import com.Reboot.Minty.trade.repository.TradeRepository;
import com.Reboot.Minty.trade.service.TradeService;
import jakarta.persistence.EntityExistsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@Controller
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;
    private final TradeService tradeService;
    private final TradeRepository tradeRepository;
    @Autowired
    public ReviewController(ReviewService reviewService, UserService userService, TradeService tradeService, TradeRepository tradeRepository) {
        this.reviewService = reviewService;
        this.userService = userService;
        this.tradeService = tradeService;
        this.tradeRepository = tradeRepository;
    }

    // 리뷰 작성 폼을 보여줌
    @GetMapping("/review/{tradeId}")
    public String showReviewForm(@PathVariable("tradeId") Long tradeId, ReviewDto reviewDto, HttpServletRequest request, Model model) {
        HttpSession session = request.getSession();
        String userEmail = (String) session.getAttribute("userEmail");
        Long userId = (Long)session.getAttribute("userId");
        Trade trade = tradeRepository.findById(tradeId).orElseThrow(EntityExistsException::new);
        User user = userService.getUserInfo(userEmail);

        reviewDto.setNickname(user.getNickName());
        reviewDto.setId(user.getId());
        reviewDto.setTrade(trade);

        model.addAttribute("trade", trade);
        model.addAttribute("reviewDto", reviewDto);

        return "review/review-form";
    }

    // 리뷰를 생성함
    @PostMapping("/")
    @Transactional
    public String createReview(@ModelAttribute("reviewDto") @Valid ReviewDto reviewDto, BindingResult bindingResult, Principal principal, HttpServletRequest request, Model model) {
        HttpSession session = request.getSession();
        String userEmail = (String) session.getAttribute("userEmail");
        User user = userService.getUserInfo(userEmail);
        reviewDto.setNickname(user.getNickName());

        if (bindingResult.hasErrors()) {
            // 유효성 검사 실패 시 처리 로직을 작성하세요.
            model.addAttribute("reviewDto", reviewDto); // 입력된 데이터를 유지하기 위해 reviewDto를 다시 모델에 추가합니다.
            return "error"; // 예: 유효성 검사 실패 시 에러 페이지로 이동하도록 "error"로 변경합니다.
        }

        MultipartFile imageFile = reviewDto.getImageFile();
        if (imageFile != null && !imageFile.isEmpty()) {
            String originalFilename = imageFile.getOriginalFilename();
            // 파일 저장 로직을 여기에 구현하세요. (예: Amazon S3, 로컬 디렉토리 등)
            // reviewDto.setImageUrl(저장된 파일 경로 또는 파일명);
        }

        Trade trade = tradeRepository.findById(reviewDto.getTrade().getId()).orElseThrow(() -> new IllegalArgumentException("Trade not found"));

        reviewDto.setTrade(trade); // Trade 객체를 ReviewDto에 설정

        reviewService.createReview(reviewDto);
        userService.increaseExp(userEmail, 10);

        return "redirect:/";
    }



    // 특정 ID에 해당하는 리뷰를 삭제함
    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return "redirect:/";
    }

    // 내가 쓴 후기만 조회
    @GetMapping("/my-review")
    public String showMyReview(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession();
        String userEmail = (String) session.getAttribute("userEmail");
        User user = userService.getUserInfo(userEmail);
        Long userId = user.getId();

        List<Review> myReviews = reviewService.getReviewsByUserId(userId);

        model.addAttribute("myReviews", myReviews);
        return "review/my-review";
    }

    // 내가 받은 후기 조회
    @GetMapping("/reviews-received")
    public String showReceivedReviews(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession();
        String userEmail = (String) session.getAttribute("userEmail");
        User user = userService.getUserInfo(userEmail);
        Long userId = user.getId();

        List<Review> receivedReviews = reviewService.getReceivedReviews(userId);

        model.addAttribute("receivedReviews", receivedReviews);
        return "review/reviews-received";
    }

}
