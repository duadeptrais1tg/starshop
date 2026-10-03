package com.starshop.service;

import com.starshop.dto.account.AddressDto;
import com.starshop.dto.account.AddressForm;

import java.util.List;

/**
 * Sổ địa chỉ nhận hàng. Mọi thao tác chỉ trên địa chỉ của chính userId truyền vào
 * (id địa chỉ của người khác -> NotFoundException, không lộ là địa chỉ đó tồn tại).
 * Luôn có đúng 1 địa chỉ mặc định nếu user có ít nhất 1 địa chỉ.
 */
public interface AddressService {

    int MAX_ADDRESSES = 10;

    /** Địa chỉ mặc định lên đầu. */
    List<AddressDto> list(Long userId);

    AddressForm getForm(Long userId, Long addressId);

    /** Địa chỉ đầu tiên tự thành mặc định. */
    Long create(Long userId, AddressForm form);

    void update(Long userId, Long addressId, AddressForm form);

    /** Xóa địa chỉ mặc định thì địa chỉ cập nhật gần nhất còn lại thành mặc định. */
    void delete(Long userId, Long addressId);

    void setDefault(Long userId, Long addressId);
}
