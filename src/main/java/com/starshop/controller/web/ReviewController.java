package com.starshop.controller.web;

import com.starshop.dto.review.ReviewForm;
import com.starshop.exception.BusinessException;
import com.starshop.exception.FileStorageException;
import com.starshop.exception.InvalidFileException;
import com.starshop.security.UserPrincipal;
import com.starshop.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Khách viết đánh giá cho sản phẩm trong đơn đã giao (mở từ trang chi tiết đơn).
 */
@Controller
@RequestMapping("/user/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private static final String FORM_VIEW = "web/user/review-form";

    private final ReviewService reviewService;

    @GetMapping("/new")
    public String form(@AuthenticationPrincipal UserPrincipal user, @RequestParam("item") Long orderItemId, Model model) {
        ReviewForm form = new ReviewForm();
        form.setOrderItemId(orderItemId);
        form.setRating(5);
        try {
            return show(user.getId(), form, model);
        } catch (BusinessException e) {
            // Đơn chưa giao / đã đánh giá: báo lỗi trên trang
            model.addAttribute("blockedError", e.getMessage());
            return FORM_VIEW;
        }
    }

    @PostMapping
    public String create(@AuthenticationPrincipal UserPrincipal user,
                         @Valid @ModelAttribute("form") ReviewForm form, BindingResult result,
                         @RequestParam(value = "images", required = false) List<MultipartFile> images,
                         @RequestParam(value = "video", required = false) MultipartFile video,
                         Model model) {
        if (!result.hasErrors()) {
            try {
                Long orderId = reviewService.create(user.getId(), form, images, video);
                return "redirect:/user/orders/" + orderId + "?msg=reviewed";
            } catch (BusinessException | InvalidFileException | FileStorageException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        try {
            return show(user.getId(), form, model);
        } catch (BusinessException e) {
            model.addAttribute("blockedError", e.getMessage());
            return FORM_VIEW;
        }
    }

    private String show(Long userId, ReviewForm form, Model model) {
        model.addAttribute("target", reviewService.reviewTarget(userId, form.getOrderItemId()));
        model.addAttribute("form", form);
        model.addAttribute("minContent", ReviewForm.MIN_CONTENT);
        model.addAttribute("maxContent", ReviewForm.MAX_CONTENT);
        model.addAttribute("maxImages", ReviewService.MAX_IMAGES);
        return FORM_VIEW;
    }
}
