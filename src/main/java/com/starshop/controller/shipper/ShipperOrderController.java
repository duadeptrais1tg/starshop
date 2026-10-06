package com.starshop.controller.shipper;

import com.starshop.entity.enums.AssignmentStatus;
import com.starshop.exception.BusinessException;
import com.starshop.security.UserPrincipal;
import com.starshop.service.ShipperOrderService;
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

import java.util.Map;

/**
 * Shipper: đơn được phân công (lọc theo trạng thái), chi tiết, bắt đầu giao / đã giao / giao thất bại / giao lại.
 * URL dùng id lần phân công; quyền sở hữu kiểm tra ở ShipperOrderService.
 */
@Controller
@RequestMapping("/shipper/orders")
@RequiredArgsConstructor
public class ShipperOrderController {

    private static final String LIST_VIEW = "shipper/orders/list";
    private static final String DETAIL_VIEW = "shipper/orders/detail";
    private static final Map<String, String> MESSAGES = Map.of(
            "started", "Đã bắt đầu giao đơn.",
            "delivered", "Đã xác nhận giao thành công.",
            "failed", "Đã ghi nhận giao thất bại.",
            "retry", "Đã chuyển sang giao lại.");

    private final ShipperOrderService orderService;

    @GetMapping
    public String list(@AuthenticationPrincipal UserPrincipal user,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        AssignmentStatus selected = EnumParams.parse(AssignmentStatus.class, status);
        if (selected == null) {
            selected = AssignmentStatus.ASSIGNED;
        }
        model.addAttribute("page", orderService.search(user.getId(), selected, page - 1));
        model.addAttribute("counts", orderService.countByStatus(user.getId()));
        model.addAttribute("statuses", AssignmentStatus.values());
        model.addAttribute("status", selected);
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

    @PostMapping("/{id}/start")
    public String start(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id, Model model) {
        return act(user.getId(), id, "started", model, () -> orderService.startDelivery(user.getId(), id));
    }

    @PostMapping("/{id}/delivered")
    public String delivered(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id, Model model) {
        return act(user.getId(), id, "delivered", model, () -> orderService.markDelivered(user.getId(), id));
    }

    @PostMapping("/{id}/failed")
    public String failed(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id,
                         @RequestParam(required = false) String reason, Model model) {
        return act(user.getId(), id, "failed", model, () -> orderService.markFailed(user.getId(), id, reason));
    }

    @PostMapping("/{id}/retry")
    public String retry(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id, Model model) {
        return act(user.getId(), id, "retry", model, () -> orderService.retry(user.getId(), id));
    }

    /** Lỗi nghiệp vụ (sai trạng thái, đơn đã giao shipper khác, thiếu lý do) hiện ngay trên trang chi tiết. */
    private String act(Long shipperId, Long assignmentId, String successMsg, Model model, Runnable action) {
        try {
            action.run();
            return "redirect:/shipper/orders/" + assignmentId + "?msg=" + successMsg;
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return show(shipperId, assignmentId, model);
        }
    }

    private String show(Long shipperId, Long assignmentId, Model model) {
        model.addAttribute("order", orderService.detail(shipperId, assignmentId));
        return DETAIL_VIEW;
    }
}
