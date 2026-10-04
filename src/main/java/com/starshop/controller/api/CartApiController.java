package com.starshop.controller.api;

import com.starshop.dto.cart.CartChangeResult;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.security.UserPrincipal;
import com.starshop.service.CartService;
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

import java.util.Map;

/**
 * API giỏ hàng cho AJAX (yêu cầu đăng nhập; POST phải kèm CSRF token ở header).
 * Chưa đăng nhập -> 401 JSON (xem LoginRedirectEntryPoint), JS sẽ chuyển sang trang đăng nhập.
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartApiController {

    private final CartService cartService;

    @GetMapping("/count")
    public Map<String, Long> count(@AuthenticationPrincipal UserPrincipal user) {
        return Map.of("cartCount", cartService.countLines(user.getId()));
    }

    @PostMapping("/items")
    public CartChangeResult add(@AuthenticationPrincipal UserPrincipal user,
                                @RequestParam Long productId, @RequestParam(defaultValue = "1") int quantity) {
        return cartService.addItem(user.getId(), productId, quantity);
    }

    @PostMapping("/items/{id}/quantity")
    public CartChangeResult updateQuantity(@AuthenticationPrincipal UserPrincipal user,
                                           @PathVariable Long id, @RequestParam int quantity) {
        return cartService.updateQuantity(user.getId(), id, quantity);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> businessError(BusinessException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }
}
