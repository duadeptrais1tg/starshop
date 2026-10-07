package com.starshop.controller.web;

import com.starshop.dto.order.CancelReason;
import com.starshop.dto.order.UserOrderTab;
import com.starshop.exception.BusinessException;
import com.starshop.exception.FileStorageException;
import com.starshop.exception.InvalidFileException;
import com.starshop.security.UserPrincipal;
import com.starshop.service.UserOrderService;
import com.starshop.util.EnumParams;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * "Đơn hàng của tôi": lịch sử mua hàng theo trạng thái, chi tiết đơn, hủy đơn, yêu cầu trả hàng.
 */
@Controller
@RequestMapping("/user/orders")
@RequiredArgsConstructor
public class UserOrderController {

    private static final String LIST_VIEW = "web/user/orders";
    private static final String DETAIL_VIEW = "web/user/order-detail";
    private static final Map<String, String> MESSAGES = Map.of(
            "cancelled", "Đã hủy đơn hàng.",
            "returnRequested", "Đã gửi yêu cầu trả hàng, shop sẽ phản hồi sớm.",
            "reviewed", "Cảm ơn bạn đã đánh giá sản phẩm!");

    private final UserOrderService orderService;

    @GetMapping
    public String list(@AuthenticationPrincipal UserPrincipal user,
                       @RequestParam(required = false) String tab,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        UserOrderTab selected = EnumParams.parse(UserOrderTab.class, tab);
        if (selected == null) {
            selected = UserOrderTab.NEW;
        }
        model.addAttribute("page", orderService.search(user.getId(), selected, page - 1));
        model.addAttribute("counts", orderService.countByTab(user.getId()));
        model.addAttribute("tabs", UserOrderTab.values());
        model.addAttribute("tab", selected);
        return LIST_VIEW;
    }

    @GetMapping("/{id}")
    public String detail(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id,
                         @RequestParam(required = false) String msg, Model model) {
        if (msg != null && MESSAGES.containsKey(msg)) {
            model.addAttribute("message", MESSAGES.get(msg));
        }
        return show(user.getId(), id, model);
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         @RequestParam(required = false) String otherReason, Model model) {
        try {
            orderService.cancel(user.getId(), id, EnumParams.parse(CancelReason.class, reason), otherReason);
            return "redirect:/user/orders/" + id + "?msg=cancelled";
        } catch (BusinessException e) {
            model.addAttribute("cancelError", e.getMessage());
            model.addAttribute("selectedReason", reason);
            model.addAttribute("otherReason", otherReason);
            return show(user.getId(), id, model);
        }
    }

    @PostMapping("/{id}/return")
    public String requestReturn(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id,
                                @RequestParam(required = false) String reason,
                                @RequestParam(value = "images", required = false) List<MultipartFile> images,
                                Model model) {
        try {
            orderService.requestReturn(user.getId(), id, reason, images);
            return "redirect:/user/orders/" + id + "?msg=returnRequested";
        } catch (BusinessException | InvalidFileException | FileStorageException e) {
            model.addAttribute("returnError", e.getMessage());
            model.addAttribute("returnReason", reason);
            return show(user.getId(), id, model);
        }
    }

    private String show(Long userId, Long orderId, Model model) {
        model.addAttribute("order", orderService.detail(userId, orderId));
        model.addAttribute("cancelReasons", CancelReason.values());
        model.addAttribute("maxReturnImages", UserOrderService.MAX_RETURN_IMAGES);
        return DETAIL_VIEW;
    }
}
