package com.starshop.controller.vendor;

import com.starshop.dto.order.VendorOrderTab;
import com.starshop.exception.BusinessException;
import com.starshop.security.UserPrincipal;
import com.starshop.service.VendorOrderService;
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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Vendor quản lý đơn hàng: danh sách theo tab trạng thái, chi tiết, xác nhận, hủy, giao shipper, duyệt trả hàng.
 * Kiểm tra quyền sở hữu và luồng trạng thái nằm ở VendorOrderService / OrderService.
 */
@Controller
@RequestMapping("/vendor/orders")
@RequiredArgsConstructor
public class VendorOrderController {

    private static final String LIST_VIEW = "vendor/orders/list";
    private static final String DETAIL_VIEW = "vendor/orders/detail";
    private static final Map<String, String> MESSAGES = Map.of(
            "confirmed", "Đã xác nhận đơn.",
            "cancelled", "Đã hủy đơn, tồn kho đã được hoàn lại.",
            "assigned", "Đã giao đơn cho shipper.",
            "returnApproved", "Đã chấp nhận trả hàng.",
            "returnRejected", "Đã từ chối yêu cầu trả hàng.");

    private final VendorOrderService orderService;

    @GetMapping
    public String list(@AuthenticationPrincipal UserPrincipal user,
                       @RequestParam(required = false) String tab,
                       @RequestParam(required = false) String code,
                       @RequestParam(required = false) String from,
                       @RequestParam(required = false) String to,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        VendorOrderTab selected = EnumParams.parse(VendorOrderTab.class, tab);
        if (selected == null) {
            selected = VendorOrderTab.NEW;
        }
        String trimmed = code == null || code.isBlank() ? null : code.trim();
        if (trimmed != null && trimmed.length() > 30) {
            trimmed = trimmed.substring(0, 30);
        }
        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }
        model.addAttribute("page", orderService.search(user.getId(), selected, trimmed, fromDate, toDate, page - 1));
        model.addAttribute("counts", orderService.countByTab(user.getId(), trimmed, fromDate, toDate));
        model.addAttribute("tabs", VendorOrderTab.values());
        model.addAttribute("tab", selected);
        model.addAttribute("code", trimmed);
        model.addAttribute("from", fromDate);
        model.addAttribute("to", toDate);
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

    @PostMapping("/{id}/confirm")
    public String confirm(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id, Model model) {
        return act(user.getId(), id, "confirmed", model, () -> orderService.confirm(user.getId(), id));
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id,
                         @RequestParam(required = false) String reason, Model model) {
        return act(user.getId(), id, "cancelled", model, () -> orderService.cancel(user.getId(), id, reason));
    }

    @PostMapping("/{id}/assign")
    public String assign(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id,
                         @RequestParam(required = false) Long shipperId, Model model) {
        return act(user.getId(), id, "assigned", model, () -> orderService.assignShipper(user.getId(), id, shipperId));
    }

    @PostMapping("/{id}/return/approve")
    public String approveReturn(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id, Model model) {
        return act(user.getId(), id, "returnApproved", model, () -> orderService.approveReturn(user.getId(), id));
    }

    @PostMapping("/{id}/return/reject")
    public String rejectReturn(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id,
                               @RequestParam(required = false) String reason, Model model) {
        return act(user.getId(), id, "returnRejected", model, () -> orderService.rejectReturn(user.getId(), id, reason));
    }

    /** Thực hiện hành động; lỗi nghiệp vụ (sai luồng, thiếu lý do...) hiện ngay trên trang chi tiết. */
    private String act(Long ownerId, Long orderId, String successMsg, Model model, Runnable action) {
        try {
            action.run();
            return "redirect:/vendor/orders/" + orderId + "?msg=" + successMsg;
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return show(ownerId, orderId, model);
        }
    }

    private String show(Long ownerId, Long orderId, Model model) {
        model.addAttribute("order", orderService.detail(ownerId, orderId));
        return DETAIL_VIEW;
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
