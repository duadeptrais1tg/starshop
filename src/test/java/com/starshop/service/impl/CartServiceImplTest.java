package com.starshop.service.impl;

import com.starshop.dto.cart.CartChangeResult;
import com.starshop.dto.cart.CartLineDto;
import com.starshop.dto.cart.CartView;
import com.starshop.dto.promotion.AutoPricing;
import com.starshop.entity.Cart;
import com.starshop.entity.CartItem;
import com.starshop.entity.Category;
import com.starshop.entity.Product;
import com.starshop.entity.Shop;
import com.starshop.entity.enums.PromotionScope;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.CartItemRepository;
import com.starshop.repository.CartRepository;
import com.starshop.repository.ProductImageRepository;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.UserRepository;
import com.starshop.service.PromotionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceImplTest {

    private static final Long USER_ID = 7L;

    private final CartRepository cartRepository = mock(CartRepository.class);
    private final CartItemRepository cartItemRepository = mock(CartItemRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductImageRepository imageRepository = mock(ProductImageRepository.class);
    private final PromotionService promotionService = mock(PromotionService.class);
    private final CartServiceImpl service = new CartServiceImpl(cartRepository, cartItemRepository, productRepository,
            imageRepository, mock(UserRepository.class), promotionService);

    private Shop shop;
    private Category category;
    private Cart cart;

    @BeforeEach
    void setUp() {
        shop = Shop.builder().id(1L).name("Hoa Xinh").slug("hoa-xinh").status(ShopStatus.APPROVED).build();
        category = Category.builder().id(2L).name("Hoa hồng").active(true).build();
        cart = Cart.builder().id(3L).build();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(promotionService.autoPricing()).thenReturn(AutoPricing.none());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void addItem_newProduct_createsLine() {
        product(10L, 5);
        when(cartItemRepository.findByCartIdAndProductId(3L, 10L)).thenReturn(Optional.empty());
        when(cartItemRepository.countByCartUserId(USER_ID)).thenReturn(1L);

        CartChangeResult result = service.addItem(USER_ID, 10L, 2);

        assertThat(result.quantity()).isEqualTo(2);
        assertThat(result.cartCount()).isEqualTo(1L);
        assertThat(result.lineTotal()).isEqualByComparingTo("400000");
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addItem_sameProduct_cumulatesQuantity() {
        Product p = product(10L, 5);
        CartItem existing = CartItem.builder().id(20L).cart(cart).product(p).quantity(2).build();
        when(cartItemRepository.findByCartIdAndProductId(3L, 10L)).thenReturn(Optional.of(existing));

        CartChangeResult result = service.addItem(USER_ID, 10L, 3);

        assertThat(existing.getQuantity()).isEqualTo(5);
        assertThat(result.quantity()).isEqualTo(5);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addItem_overStockIncludingCart_rejected() {
        Product p = product(10L, 5);
        CartItem existing = CartItem.builder().id(20L).cart(cart).product(p).quantity(4).build();
        when(cartItemRepository.findByCartIdAndProductId(3L, 10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.addItem(USER_ID, 10L, 2))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tối đa 5").hasMessageContaining("đã có 4");
        assertThat(existing.getQuantity()).isEqualTo(4);
    }

    @Test
    void addItem_invalidQuantityOrUnavailableProduct_rejected() {
        assertThatThrownBy(() -> service.addItem(USER_ID, 10L, 0)).isInstanceOf(BusinessException.class);

        product(11L, 0);
        assertThatThrownBy(() -> service.addItem(USER_ID, 11L, 1)).hasMessage("Sản phẩm đã hết hàng");

        Product off = product(12L, 5);
        off.setActive(false);
        assertThatThrownBy(() -> service.addItem(USER_ID, 12L, 1)).hasMessage("Sản phẩm đã ngừng bán");

        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addItem(USER_ID, 99L, 1)).isInstanceOf(NotFoundException.class);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addItem_firstTime_createsCart() {
        product(10L, 5);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> {
            Cart c = inv.getArgument(0);
            c.setId(30L);
            return c;
        });
        when(cartItemRepository.findByCartIdAndProductId(30L, 10L)).thenReturn(Optional.empty());

        service.addItem(USER_ID, 10L, 1);

        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void updateQuantity_validatesRangeAndStock() {
        Product p = product(10L, 3);
        CartItem item = CartItem.builder().id(20L).cart(cart).product(p).quantity(1).build();
        when(cartItemRepository.findByIdAndCartUserId(20L, USER_ID)).thenReturn(Optional.of(item));

        assertThat(service.updateQuantity(USER_ID, 20L, 3).lineTotal()).isEqualByComparingTo("600000");
        assertThat(item.getQuantity()).isEqualTo(3);

        assertThatThrownBy(() -> service.updateQuantity(USER_ID, 20L, 0)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.updateQuantity(USER_ID, 20L, 4)).hasMessage("Chỉ còn 3 sản phẩm.");
        assertThat(item.getQuantity()).isEqualTo(3);
    }

    @Test
    void updateQuantity_otherUsersItem_notFound() {
        when(cartItemRepository.findByIdAndCartUserId(anyLong(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateQuantity(USER_ID, 20L, 1)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeItems_deletesOnlyOwnItems() {
        CartItem own = CartItem.builder().id(20L).cart(cart).product(product(10L, 5)).quantity(1).build();
        when(cartItemRepository.findByIdInAndCartUserId(List.of(20L, 21L), USER_ID)).thenReturn(List.of(own));

        assertThat(service.removeItems(USER_ID, List.of(20L, 21L))).isEqualTo(1);
        verify(cartItemRepository).deleteAll(List.of(own));
        assertThat(service.removeItems(USER_ID, List.of())).isZero();
    }

    @Test
    void getCart_groupsByShop_appliesSalePrice_andFlagsUnavailable() {
        Shop other = Shop.builder().id(5L).name("Shop Khác").slug("shop-khac").status(ShopStatus.APPROVED).build();
        Product a = product(10L, 5);
        Product b = product(11L, 0);
        Product c = product(12L, 9);
        c.setShop(other);
        when(cartItemRepository.findByCartUserIdOrderByIdDesc(USER_ID)).thenReturn(List.of(
                CartItem.builder().id(1L).product(a).quantity(2).build(),
                CartItem.builder().id(2L).product(c).quantity(1).build(),
                CartItem.builder().id(3L).product(b).quantity(1).build()));
        when(imageRepository.findImageUrlsByProductIds(any())).thenReturn(List.<Object[]>of(new Object[]{10L, "a.jpg"}));
        // Giảm 10% toàn sàn
        when(promotionService.autoPricing()).thenReturn(new AutoPricing(List.of(
                new AutoPricing.Rule(PromotionScope.PLATFORM, null, null, BigDecimal.TEN, null)), Map.of()));

        CartView view = service.getCart(USER_ID);

        assertThat(view.getGroups()).hasSize(2);
        assertThat(view.getGroups().get(0).getShopName()).isEqualTo("Hoa Xinh");
        List<CartLineDto> first = view.getGroups().get(0).getLines();
        assertThat(first).extracting(CartLineDto::getItemId).containsExactly(1L, 3L);
        assertThat(first.get(0).getUnitPrice()).isEqualByComparingTo("180000");
        assertThat(first.get(0).getCompareAtPrice()).isEqualByComparingTo("200000");
        assertThat(first.get(0).getLineTotal()).isEqualByComparingTo("360000");
        assertThat(first.get(0).getImageUrl()).isEqualTo("a.jpg");
        assertThat(first.get(0).isAvailable()).isTrue();
        assertThat(first.get(1).getUnavailableReason()).isEqualTo("Sản phẩm đã hết hàng");
    }

    @Test
    void unavailableReason_coversAllCases() {
        Product p = product(10L, 3);
        assertThat(CartServiceImpl.unavailableReason(p, 3)).isNull();
        assertThat(CartServiceImpl.unavailableReason(p, 4)).isEqualTo("Chỉ còn 3 sản phẩm, vui lòng giảm số lượng");

        shop.setStatus(ShopStatus.SUSPENDED);
        assertThat(CartServiceImpl.unavailableReason(p, 1)).isEqualTo("Sản phẩm đã ngừng bán");
        shop.setStatus(ShopStatus.APPROVED);

        category.setActive(false);
        assertThat(CartServiceImpl.unavailableReason(p, 1)).isEqualTo("Sản phẩm đã ngừng bán");
    }

    private Product product(Long id, int stock) {
        Product p = Product.builder().id(id).shop(shop).category(category).name("Bó hoa " + id).slug("bo-hoa-" + id)
                .price(BigDecimal.valueOf(200000)).stock(stock).active(true).build();
        when(productRepository.findById(id)).thenReturn(Optional.of(p));
        return p;
    }
}
