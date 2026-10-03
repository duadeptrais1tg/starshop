package com.starshop.service.impl;

import com.starshop.dto.account.AddressForm;
import com.starshop.entity.Address;
import com.starshop.entity.User;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.AddressRepository;
import com.starshop.repository.UserRepository;
import com.starshop.service.AddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddressServiceImplTest {

    private static final Long ME = 5L;
    private static final Long OTHER = 6L;

    private final AddressRepository repo = mock(AddressRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AddressServiceImpl service = new AddressServiceImpl(repo, userRepository);

    @BeforeEach
    void setUp() {
        when(userRepository.getReferenceById(ME)).thenReturn(new User());
        when(repo.save(any(Address.class))).thenAnswer(inv -> {
            Address a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });
        when(repo.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());
    }

    @Test
    void firstAddress_isAlwaysDefault() {
        when(repo.countByUserId(ME)).thenReturn(0L);
        ArgumentCaptor<Address> saved = ArgumentCaptor.forClass(Address.class);

        service.create(ME, form(false, "+84912345678"));

        verify(repo).save(saved.capture());
        assertThat(saved.getValue().isDefaultAddress()).isTrue();
        assertThat(saved.getValue().getPhone()).isEqualTo("0912345678");
        verify(repo).clearDefaultExcept(ME, 100L);
    }

    @Test
    void laterAddress_isDefaultOnlyWhenTicked() {
        when(repo.countByUserId(ME)).thenReturn(2L);
        ArgumentCaptor<Address> saved = ArgumentCaptor.forClass(Address.class);

        service.create(ME, form(false, "0912345678"));

        verify(repo).save(saved.capture());
        assertThat(saved.getValue().isDefaultAddress()).isFalse();
        verify(repo, never()).clearDefaultExcept(anyLong(), anyLong());
    }

    @Test
    void cannotExceedMaxAddresses() {
        when(repo.countByUserId(ME)).thenReturn((long) AddressService.MAX_ADDRESSES);

        assertThatThrownBy(() -> service.create(ME, form(false, "0912345678")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("tối đa");
    }

    @Test
    void otherUsersAddress_isNotFound_forEveryAction() {
        Address othersAddress = address(9L, true);
        when(repo.findByIdAndUserId(9L, OTHER)).thenReturn(Optional.of(othersAddress));

        assertThatThrownBy(() -> service.getForm(ME, 9L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.update(ME, 9L, form(false, "0912345678"))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(ME, 9L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.setDefault(ME, 9L)).isInstanceOf(NotFoundException.class);
        verify(repo, never()).delete(any(Address.class));
    }

    @Test
    void setDefault_clearsOthers() {
        Address a = address(3L, false);
        when(repo.findByIdAndUserId(3L, ME)).thenReturn(Optional.of(a));

        service.setDefault(ME, 3L);

        assertThat(a.isDefaultAddress()).isTrue();
        verify(repo).clearDefaultExcept(ME, 3L);
    }

    @Test
    void untickingDefault_onTheDefaultAddress_keepsItDefault() {
        Address a = address(3L, true);
        when(repo.findByIdAndUserId(3L, ME)).thenReturn(Optional.of(a));

        service.update(ME, 3L, form(false, "0912345678"));

        assertThat(a.isDefaultAddress()).isTrue();
    }

    @Test
    void deletingDefault_promotesNextAddress() {
        Address def = address(3L, true);
        Address next = address(4L, false);
        when(repo.findByIdAndUserId(3L, ME)).thenReturn(Optional.of(def));
        when(repo.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(ME)).thenReturn(List.of(next));

        service.delete(ME, 3L);

        verify(repo).delete(def);
        assertThat(next.isDefaultAddress()).isTrue();
    }

    @Test
    void invalidPhone_rechecked_inService() {
        when(repo.countByUserId(ME)).thenReturn(0L);
        assertThatThrownBy(() -> service.create(ME, form(false, "123"))).hasMessageContaining("Số điện thoại");
    }

    private static Address address(Long id, boolean isDefault) {
        Address a = Address.builder().receiverName("An").phone("0912345678").province("HCM").district("Q1")
                .ward("Bến Nghé").detail("12 Nguyễn Huệ").defaultAddress(isDefault).build();
        a.setId(id);
        return a;
    }

    private static AddressForm form(boolean isDefault, String phone) {
        AddressForm f = new AddressForm();
        f.setReceiverName(" Nguyễn Văn An ");
        f.setPhone(phone);
        f.setProvince("TP. Hồ Chí Minh");
        f.setDistrict("Quận 1");
        f.setWard("Phường Bến Nghé");
        f.setDetail("12 Nguyễn Huệ");
        f.setDefaultAddress(isDefault);
        return f;
    }
}
