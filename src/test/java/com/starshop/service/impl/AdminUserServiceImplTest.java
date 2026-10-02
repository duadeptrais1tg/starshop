package com.starshop.service.impl;

import com.starshop.dto.admin.RoleAssignmentRequest;
import com.starshop.entity.Carrier;
import com.starshop.entity.Role;
import com.starshop.entity.Store;
import com.starshop.entity.User;
import com.starshop.entity.enums.RoleName;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.CarrierRepository;
import com.starshop.repository.RoleRepository;
import com.starshop.repository.StoreRepository;
import com.starshop.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static com.starshop.security.TestUsers.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminUserServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long TARGET_ID = 5L;

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final CarrierRepository carrierRepository = mock(CarrierRepository.class);
    private final AdminUserServiceImpl service =
            new AdminUserServiceImpl(userRepository, roleRepository, storeRepository, carrierRepository);

    private User admin;
    private User target;
    private Store store;
    private Carrier carrier;

    @BeforeEach
    void setUp() {
        admin = user(ADMIN_ID, "admin@starshop.vn", true, false, RoleName.ADMIN);
        target = user(TARGET_ID, "an@gmail.com", true, false, RoleName.USER);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        when(roleRepository.findByName(any())).thenAnswer(inv -> Optional.of(Role.builder().name(inv.getArgument(0)).build()));

        store = Store.builder().name("StarShop Quận 1").address("x").active(true).build();
        store.setId(10L);
        carrier = Carrier.builder().name("GHN").active(true).build();
        carrier.setId(20L);
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
        when(carrierRepository.findById(20L)).thenReturn(Optional.of(carrier));
    }

    @Test
    void lock_otherUser_works_butNotSelf() {
        service.lock(TARGET_ID, ADMIN_ID);
        assertThat(target.isLocked()).isTrue();

        assertThatThrownBy(() -> service.lock(ADMIN_ID, ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tự khóa");
        assertThat(admin.isLocked()).isFalse();

        service.unlock(TARGET_ID);
        assertThat(target.isLocked()).isFalse();
    }

    @Test
    void unknownUser_isNotFound() {
        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void assignManager_requiresStore() {
        assertThatThrownBy(() -> service.assignRoles(TARGET_ID, form(null, null, RoleName.MANAGER), ADMIN_ID))
                .hasMessageContaining("chi nhánh");

        service.assignRoles(TARGET_ID, form(10L, null, RoleName.MANAGER), ADMIN_ID);
        assertThat(target.hasRole(RoleName.MANAGER)).isTrue();
        assertThat(target.getStore()).isSameAs(store);
        assertThat(target.getCarrier()).isNull();
    }

    @Test
    void assignShipper_requiresCarrier_andClearsStoreWhenNoLongerManager() {
        target.setStore(store);
        assertThatThrownBy(() -> service.assignRoles(TARGET_ID, form(null, null, RoleName.SHIPPER), ADMIN_ID))
                .hasMessageContaining("nhà vận chuyển");

        service.assignRoles(TARGET_ID, form(10L, 20L, RoleName.SHIPPER), ADMIN_ID);
        assertThat(target.getCarrier()).isSameAs(carrier);
        assertThat(target.getStore()).as("không còn là MANAGER thì bỏ gán chi nhánh").isNull();
    }

    @Test
    void inactiveStore_isRejected() {
        store.setActive(false);
        assertThatThrownBy(() -> service.assignRoles(TARGET_ID, form(10L, null, RoleName.MANAGER), ADMIN_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void vendorRole_cannotBeAddedOrRemovedByForm() {
        service.assignRoles(TARGET_ID, form(null, null, RoleName.USER, RoleName.VENDOR), ADMIN_ID);
        assertThat(target.hasRole(RoleName.VENDOR)).as("không tự thêm VENDOR").isFalse();

        User vendor = user(6L, "vendor@starshop.vn", true, false, RoleName.USER, RoleName.VENDOR);
        when(userRepository.findById(6L)).thenReturn(Optional.of(vendor));
        service.assignRoles(6L, form(null, null, RoleName.USER), ADMIN_ID);
        assertThat(vendor.hasRole(RoleName.VENDOR)).as("không bỏ VENDOR").isTrue();
    }

    @Test
    void cannotRemoveOwnAdminRole_orSaveEmptyRoles() {
        assertThatThrownBy(() -> service.assignRoles(ADMIN_ID, form(null, null, RoleName.USER), ADMIN_ID))
                .hasMessageContaining("tự bỏ quyền");
        assertThat(admin.hasRole(RoleName.ADMIN)).isTrue();

        assertThatThrownBy(() -> service.assignRoles(TARGET_ID, form(null, null), ADMIN_ID))
                .hasMessageContaining("ít nhất một vai trò");
    }

    private static RoleAssignmentRequest form(Long storeId, Long carrierId, RoleName... roles) {
        RoleAssignmentRequest form = new RoleAssignmentRequest();
        form.setRoles(Set.of(roles));
        form.setStoreId(storeId);
        form.setCarrierId(carrierId);
        return form;
    }
}
