package com.starshop.service.impl;

import com.starshop.dto.admin.ShopAdminDto;
import com.starshop.entity.Shop;
import com.starshop.entity.Store;
import com.starshop.entity.enums.RoleName;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.mapper.AdminMapper;
import com.starshop.repository.RoleRepository;
import com.starshop.repository.ShopRepository;
import com.starshop.repository.StoreRepository;
import com.starshop.repository.spec.ShopSpecifications;
import com.starshop.service.AdminShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminShopServiceImpl implements AdminShopService {

    private static final int MAX_REASON_LENGTH = 255;

    private final ShopRepository shopRepository;
    private final StoreRepository storeRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ShopAdminDto> search(ShopStatus status, String keyword, int page) {
        // Chờ duyệt: cũ nhất lên đầu (xử lý theo thứ tự); các tab khác: mới nhất lên đầu
        Sort sort = status == ShopStatus.PENDING ? Sort.by("id") : Sort.by(Sort.Direction.DESC, "id");
        return shopRepository.findAll(
                        Specification.allOf(ShopSpecifications.status(status), ShopSpecifications.keyword(keyword)),
                        PageRequest.of(Math.max(page, 0), PAGE_SIZE, sort))
                .map(AdminMapper::toShopDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ShopStatus, Long> countByStatus() {
        Map<ShopStatus, Long> counts = new EnumMap<>(ShopStatus.class);
        for (ShopStatus status : ShopStatus.values()) {
            counts.put(status, shopRepository.countByStatus(status));
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public ShopAdminDto get(Long id) {
        return AdminMapper.toShopDto(find(id));
    }

    @Override
    @Transactional
    public void approve(Long shopId, Long storeId) {
        Shop shop = find(shopId);
        requireStatus(shop, ShopStatus.PENDING, "duyệt");
        if (storeId == null) {
            throw new BusinessException("Vui lòng chọn chi nhánh quản lý shop.");
        }
        Store store = storeRepository.findById(storeId)
                .filter(Store::isActive)
                .orElseThrow(() -> new BusinessException("Chi nhánh không tồn tại hoặc đã ngừng hoạt động."));

        shop.setStore(store);
        shop.setStatus(ShopStatus.APPROVED);
        shop.setStatusReason(null);
        if (!shop.getOwner().hasRole(RoleName.VENDOR)) {
            shop.getOwner().getRoles().add(roleRepository.findByName(RoleName.VENDOR)
                    .orElseThrow(() -> new IllegalStateException("Thiếu role VENDOR trong CSDL")));
        }
    }

    @Override
    @Transactional
    public void reject(Long shopId, String reason) {
        Shop shop = find(shopId);
        requireStatus(shop, ShopStatus.PENDING, "từ chối");
        String validReason = requireReason(reason);
        shop.setStatus(ShopStatus.REJECTED);
        shop.setStatusReason(validReason);
    }

    @Override
    @Transactional
    public void suspend(Long shopId, String reason) {
        Shop shop = find(shopId);
        requireStatus(shop, ShopStatus.APPROVED, "đình chỉ");
        String validReason = requireReason(reason);
        shop.setStatus(ShopStatus.SUSPENDED);
        shop.setStatusReason(validReason);
    }

    @Override
    @Transactional
    public void reactivate(Long shopId) {
        Shop shop = find(shopId);
        requireStatus(shop, ShopStatus.SUSPENDED, "mở lại");
        shop.setStatus(ShopStatus.APPROVED);
        shop.setStatusReason(null);
    }

    private Shop find(Long id) {
        return shopRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy shop #" + id));
    }

    private static void requireStatus(Shop shop, ShopStatus expected, String action) {
        if (shop.getStatus() != expected) {
            throw new BusinessException("Không thể " + action + " shop đang ở trạng thái \""
                    + shop.getStatus().getLabel() + "\".");
        }
    }

    private static String requireReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("Vui lòng nhập lý do.");
        }
        String trimmed = reason.trim();
        if (trimmed.length() > MAX_REASON_LENGTH) {
            throw new BusinessException("Lý do tối đa " + MAX_REASON_LENGTH + " ký tự.");
        }
        return trimmed;
    }
}
