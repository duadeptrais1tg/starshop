package com.starshop.service.impl;

import com.starshop.dto.cart.CartChangeResult;
import com.starshop.dto.cart.CartGroupDto;
import com.starshop.dto.cart.CartLineDto;
import com.starshop.dto.cart.CartView;
import com.starshop.dto.promotion.AutoPricing;
import com.starshop.entity.Cart;
import com.starshop.entity.CartItem;
import com.starshop.entity.Product;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.CartItemRepository;
import com.starshop.repository.CartRepository;
import com.starshop.repository.ProductImageRepository;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.UserRepository;
import com.starshop.service.CartService;
import com.starshop.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final PromotionService promotionService;

    @Override
    @Transactional(readOnly = true)
    public CartView getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByCartUserIdOrderByIdDesc(userId);
        if (items.isEmpty()) {
            return new CartView(List.of());
        }
        Map<Long, String> images = new HashMap<>();
        for (Object[] row : productImageRepository.findImageUrlsByProductIds(
                items.stream().map(i -> i.getProduct().getId()).toList())) {
            images.putIfAbsent((Long) row[0], (String) row[1]);
        }
        AutoPricing pricing = promotionService.autoPricing();

        // Nhóm theo shop, giữ thứ tự shop có sản phẩm thêm gần nhất lên trước
        Map<Long, List<CartLineDto>> byShop = new LinkedHashMap<>();
        Map<Long, Product> shopSample = new HashMap<>();
        for (CartItem item : items) {
            Product p = item.getProduct();
            byShop.computeIfAbsent(p.getShop().getId(), k -> new ArrayList<>()).add(toLine(item, images.get(p.getId()), pricing));
            shopSample.putIfAbsent(p.getShop().getId(), p);
        }
        List<CartGroupDto> groups = new ArrayList<>();
        byShop.forEach((shopId, lines) -> {
            Product sample = shopSample.get(shopId);
            groups.add(new CartGroupDto(shopId, sample.getShop().getName(), sample.getShop().getSlug(), lines));
        });
        return new CartView(groups);
    }

    @Override
    @Transactional(readOnly = true)
    public long countLines(Long userId) {
        return cartItemRepository.countByCartUserId(userId);
    }

    @Override
    @Transactional
    public CartChangeResult addItem(Long userId, Long productId, int quantity) {
        if (quantity < 1) {
            throw new BusinessException("Số lượng phải từ 1 trở lên.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm"));
        String reason = unavailableReason(product, 1);
        if (reason != null) {
            throw new BusinessException(reason);
        }
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder().user(userRepository.getReferenceById(userId)).build()));
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId).orElse(null);

        int current = item == null ? 0 : item.getQuantity();
        int newQuantity = current + quantity;
        int max = Math.min(product.getStock(), MAX_QUANTITY_PER_ITEM);
        if (newQuantity > max) {
            throw new BusinessException(current > 0
                    ? "Chỉ có thể mua tối đa " + max + " sản phẩm này (giỏ của bạn đã có " + current + ")."
                    : "Chỉ có thể mua tối đa " + max + " sản phẩm này.");
        }
        if (item == null) {
            item = cartItemRepository.save(CartItem.builder().cart(cart).product(product).quantity(newQuantity).build());
        } else {
            item.setQuantity(newQuantity);
        }
        return new CartChangeResult(cartItemRepository.countByCartUserId(userId), newQuantity,
                unitPrice(product, promotionService.autoPricing()).multiply(BigDecimal.valueOf(newQuantity)),
                "Đã thêm vào giỏ hàng.");
    }

    @Override
    @Transactional
    public CartChangeResult updateQuantity(Long userId, Long itemId, int quantity) {
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm trong giỏ"));
        Product product = item.getProduct();
        if (quantity < 1) {
            throw new BusinessException("Số lượng phải từ 1 trở lên. Muốn bỏ sản phẩm, hãy bấm xóa.");
        }
        int max = Math.min(product.getStock(), MAX_QUANTITY_PER_ITEM);
        if (quantity > max) {
            throw new BusinessException(max == 0 ? "Sản phẩm đã hết hàng." : "Chỉ còn " + max + " sản phẩm.");
        }
        item.setQuantity(quantity);
        return new CartChangeResult(cartItemRepository.countByCartUserId(userId), quantity,
                unitPrice(product, promotionService.autoPricing()).multiply(BigDecimal.valueOf(quantity)),
                "Đã cập nhật số lượng.");
    }

    @Override
    @Transactional
    public int removeItems(Long userId, Collection<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return 0;
        }
        List<CartItem> own = cartItemRepository.findByIdInAndCartUserId(itemIds, userId);
        cartItemRepository.deleteAll(own);
        return own.size();
    }

    // ------------------------------------------------------------------ helpers

    private CartLineDto toLine(CartItem item, String imageUrl, AutoPricing pricing) {
        Product p = item.getProduct();
        BigDecimal unit = unitPrice(p, pricing);
        BigDecimal compare = unit.compareTo(p.getPrice()) < 0 ? p.getPrice() : p.getOriginalPrice();
        return CartLineDto.builder()
                .itemId(item.getId())
                .productId(p.getId())
                .productName(p.getName())
                .productSlug(p.getSlug())
                .imageUrl(imageUrl)
                .unitPrice(unit)
                .compareAtPrice(compare != null && compare.compareTo(unit) > 0 ? compare : null)
                .quantity(item.getQuantity())
                .stock(p.getStock())
                .unavailableReason(unavailableReason(p, item.getQuantity()))
                .build();
    }

    /** Giá khách trả cho 1 sản phẩm: giá bán, hoặc giá sau khuyến mãi tự áp dụng nếu có. */
    private static BigDecimal unitPrice(Product p, AutoPricing pricing) {
        BigDecimal sale = pricing.salePrice(p.getShop().getId(), p.getCategory().getId(), p.getPrice());
        return sale != null ? sale : p.getPrice();
    }

    /**
     * Lý do không mua được (null = mua được): ngừng bán (sản phẩm tắt / shop không hoạt động / danh mục ẩn),
     * hết hàng, hoặc số lượng trong giỏ vượt tồn kho hiện tại.
     */
    static String unavailableReason(Product p, int quantity) {
        if (!p.isActive() || p.getShop().getStatus() != ShopStatus.APPROVED || !p.getCategory().isActive()) {
            return "Sản phẩm đã ngừng bán";
        }
        if (p.getStock() <= 0) {
            return "Sản phẩm đã hết hàng";
        }
        if (quantity > p.getStock()) {
            return "Chỉ còn " + p.getStock() + " sản phẩm, vui lòng giảm số lượng";
        }
        return null;
    }
}
