package com.starshop.controller.api;

import com.starshop.exception.NotFoundException;
import com.starshop.security.UserPrincipal;
import com.starshop.service.FavoriteService;
import com.starshop.service.FavoriteService.ToggleResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Nút tim (AJAX). Khách chưa đăng nhập nhận 401 -> starshop.js chuyển sang trang đăng nhập.
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteApiController {

    /** Số sản phẩm tối đa trong một lần hỏi trạng thái tim. */
    private static final int MAX_IDS = 100;

    private final FavoriteService favoriteService;

    @PostMapping("/{productId}/toggle")
    public ToggleResult toggle(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long productId) {
        return favoriteService.toggle(user.getId(), productId);
    }

    /** Các sản phẩm (trong danh sách đang hiển thị) mà user đã thích. productIds=1,2,3 */
    @GetMapping("/ids")
    public Map<String, Set<Long>> likedIds(@AuthenticationPrincipal UserPrincipal user,
                                           @RequestParam(defaultValue = "") String productIds) {
        List<Long> ids = Arrays.stream(productIds.split(","))
                .map(FavoriteApiController::parseId)
                .filter(Objects::nonNull)
                .distinct()
                .limit(MAX_IDS)
                .toList();
        return Map.of("ids", favoriteService.likedIds(user.getId(), ids));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    private static Long parseId(String value) {
        try {
            long id = Long.parseLong(value.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
