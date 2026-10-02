package com.starshop.service.impl;

import com.starshop.entity.Role;
import com.starshop.entity.Shop;
import com.starshop.entity.Store;
import com.starshop.entity.User;
import com.starshop.entity.enums.RoleName;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.exception.BusinessException;
import com.starshop.repository.RoleRepository;
import com.starshop.repository.ShopRepository;
import com.starshop.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.starshop.security.TestUsers.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminShopServiceImplTest {

    private final ShopRepository shopRepository = mock(ShopRepository.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final AdminShopServiceImpl service = new AdminShopServiceImpl(shopRepository, storeRepository, roleRepository);

    private User owner;
    private Shop shop;
    private Store store;

    @BeforeEach
    void setUp() {
        owner = user(7L, "chushop@gmail.com", true, false, RoleName.USER);
        shop = Shop.builder().owner(owner).name("Hoa Mai").slug("hoa-mai").pickupAddress("x").phone("0912345678").build();
        shop.setId(3L);
        store = Store.builder().name("StarShop Quận 1").address("x").active(true).build();
        store.setId(10L);
        when(shopRepository.findById(3L)).thenReturn(Optional.of(shop));
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
        when(roleRepository.findByName(any())).thenAnswer(inv -> Optional.of(Role.builder().name(inv.getArgument(0)).build()));
    }

    @Test
    void approve_assignsStore_andGrantsVendorRole() {
        service.approve(3L, 10L);

        assertThat(shop.getStatus()).isEqualTo(ShopStatus.APPROVED);
        assertThat(shop.getStore()).isSameAs(store);
        assertThat(owner.hasRole(RoleName.VENDOR)).isTrue();
        assertThat(owner.hasRole(RoleName.USER)).isTrue();
    }

    @Test
    void approve_requiresActiveStore() {
        assertThatThrownBy(() -> service.approve(3L, null)).hasMessageContaining("chọn chi nhánh");
        store.setActive(false);
        assertThatThrownBy(() -> service.approve(3L, 10L)).isInstanceOf(BusinessException.class);
        assertThat(shop.getStatus()).isEqualTo(ShopStatus.PENDING);
        assertThat(owner.hasRole(RoleName.VENDOR)).isFalse();
    }

    @Test
    void reject_requiresReason() {
        assertThatThrownBy(() -> service.reject(3L, "   ")).hasMessageContaining("lý do");
        assertThatThrownBy(() -> service.reject(3L, "x".repeat(256))).hasMessageContaining("tối đa");

        service.reject(3L, "  Địa chỉ chưa rõ ràng  ");
        assertThat(shop.getStatus()).isEqualTo(ShopStatus.REJECTED);
        assertThat(shop.getStatusReason()).isEqualTo("Địa chỉ chưa rõ ràng");
        assertThat(owner.hasRole(RoleName.VENDOR)).isFalse();
    }

    @Test
    void suspend_thenReactivate() {
        service.approve(3L, 10L);
        service.suspend(3L, "Nhiều khiếu nại");
        assertThat(shop.getStatus()).isEqualTo(ShopStatus.SUSPENDED);
        assertThat(shop.getStatusReason()).isEqualTo("Nhiều khiếu nại");

        service.reactivate(3L);
        assertThat(shop.getStatus()).isEqualTo(ShopStatus.APPROVED);
        assertThat(shop.getStatusReason()).isNull();
    }

    @Test
    void wrongTransitions_areRejected() {
        assertThatThrownBy(() -> service.suspend(3L, "x")).hasMessageContaining("Chờ duyệt");
        assertThatThrownBy(() -> service.reactivate(3L)).isInstanceOf(BusinessException.class);

        service.reject(3L, "Thiếu thông tin");
        assertThatThrownBy(() -> service.approve(3L, 10L)).hasMessageContaining("Bị từ chối");
    }
}
