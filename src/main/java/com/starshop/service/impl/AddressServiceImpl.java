package com.starshop.service.impl;

import com.starshop.dto.RegisterRequest;
import com.starshop.dto.account.AddressDto;
import com.starshop.dto.account.AddressForm;
import com.starshop.entity.Address;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.AddressRepository;
import com.starshop.repository.UserRepository;
import com.starshop.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressDto> list(Long userId) {
        return addressRepository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(userId).stream()
                .map(AddressServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressForm getForm(Long userId, Long addressId) {
        Address a = findOwned(userId, addressId);
        AddressForm form = new AddressForm();
        form.setReceiverName(a.getReceiverName());
        form.setPhone(a.getPhone());
        form.setProvince(a.getProvince());
        form.setDistrict(a.getDistrict());
        form.setWard(a.getWard());
        form.setDetail(a.getDetail());
        form.setDefaultAddress(a.isDefaultAddress());
        return form;
    }

    @Override
    @Transactional
    public Long create(Long userId, AddressForm form) {
        validate(form);
        long count = addressRepository.countByUserId(userId);
        if (count >= MAX_ADDRESSES) {
            throw new BusinessException("Bạn chỉ lưu được tối đa " + MAX_ADDRESSES + " địa chỉ.");
        }
        Address address = new Address();
        address.setUser(userRepository.getReferenceById(userId));
        apply(address, form);
        // Địa chỉ đầu tiên luôn là mặc định
        address.setDefaultAddress(count == 0 || form.isDefaultAddress());
        Address saved = addressRepository.save(address);
        if (saved.isDefaultAddress()) {
            addressRepository.clearDefaultExcept(userId, saved.getId());
        }
        return saved.getId();
    }

    @Override
    @Transactional
    public void update(Long userId, Long addressId, AddressForm form) {
        validate(form);
        Address address = findOwned(userId, addressId);
        apply(address, form);
        // Bỏ tick "mặc định" ở địa chỉ đang mặc định thì giữ nguyên (luôn phải có 1 địa chỉ mặc định)
        if (form.isDefaultAddress() && !address.isDefaultAddress()) {
            address.setDefaultAddress(true);
            addressRepository.clearDefaultExcept(userId, addressId);
        }
    }

    @Override
    @Transactional
    public void delete(Long userId, Long addressId) {
        Address address = findOwned(userId, addressId);
        boolean wasDefault = address.isDefaultAddress();
        addressRepository.delete(address);
        addressRepository.flush();
        if (wasDefault) {
            addressRepository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(userId).stream()
                    .findFirst()
                    .ifPresent(next -> next.setDefaultAddress(true));
        }
    }

    @Override
    @Transactional
    public void setDefault(Long userId, Long addressId) {
        Address address = findOwned(userId, addressId);
        address.setDefaultAddress(true);
        addressRepository.clearDefaultExcept(userId, addressId);
    }

    private Address findOwned(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy địa chỉ"));
    }

    /** Kiểm tra lại ở service (không chỉ dựa vào @Valid ở controller). */
    private static void validate(AddressForm form) {
        if (!StringUtils.hasText(form.getReceiverName()) || !StringUtils.hasText(form.getProvince())
                || !StringUtils.hasText(form.getDistrict()) || !StringUtils.hasText(form.getWard())
                || !StringUtils.hasText(form.getDetail())) {
            throw new BusinessException("Vui lòng nhập đầy đủ thông tin địa chỉ.");
        }
        if (form.getPhone() == null || !form.getPhone().trim().matches(RegisterRequest.PHONE_REGEX)) {
            throw new BusinessException("Số điện thoại không hợp lệ.");
        }
    }

    private static void apply(Address address, AddressForm form) {
        address.setReceiverName(form.getReceiverName().trim());
        String phone = form.getPhone().trim();
        address.setPhone(phone.startsWith("+84") ? "0" + phone.substring(3) : phone);
        address.setProvince(form.getProvince().trim());
        address.setDistrict(form.getDistrict().trim());
        address.setWard(form.getWard().trim());
        address.setDetail(form.getDetail().trim());
    }

    private static AddressDto toDto(Address a) {
        return AddressDto.builder()
                .id(a.getId())
                .receiverName(a.getReceiverName())
                .phone(a.getPhone())
                .province(a.getProvince())
                .district(a.getDistrict())
                .ward(a.getWard())
                .detail(a.getDetail())
                .defaultAddress(a.isDefaultAddress())
                .build();
    }
}
