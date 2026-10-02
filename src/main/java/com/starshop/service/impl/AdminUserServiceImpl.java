package com.starshop.service.impl;

import com.starshop.dto.OptionDto;
import com.starshop.dto.admin.RoleAssignmentRequest;
import com.starshop.dto.admin.UserAdminDto;
import com.starshop.dto.admin.UserStatusFilter;
import com.starshop.entity.Carrier;
import com.starshop.entity.Role;
import com.starshop.entity.Store;
import com.starshop.entity.User;
import com.starshop.entity.enums.RoleName;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.mapper.AdminMapper;
import com.starshop.repository.CarrierRepository;
import com.starshop.repository.RoleRepository;
import com.starshop.repository.StoreRepository;
import com.starshop.repository.UserRepository;
import com.starshop.repository.spec.UserSpecifications;
import com.starshop.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StoreRepository storeRepository;
    private final CarrierRepository carrierRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserAdminDto> search(String keyword, RoleName role, UserStatusFilter status, int page) {
        Specification<User> spec = Specification.allOf(
                UserSpecifications.keyword(keyword),
                UserSpecifications.hasRole(role),
                UserSpecifications.status(status));
        PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"));
        return userRepository.findAll(spec, pageable).map(AdminMapper::toUserDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAdminDto get(Long id) {
        return AdminMapper.toUserDto(find(id));
    }

    @Override
    @Transactional
    public void lock(Long userId, Long actorId) {
        if (Objects.equals(userId, actorId)) {
            throw new BusinessException("Bạn không thể tự khóa tài khoản của mình.");
        }
        find(userId).setLocked(true);
    }

    @Override
    @Transactional
    public void unlock(Long userId) {
        find(userId).setLocked(false);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, RoleAssignmentRequest request, Long actorId) {
        User user = find(userId);

        Set<RoleName> requested = EnumSet.noneOf(RoleName.class);
        if (request.getRoles() != null) {
            requested.addAll(request.getRoles());
        }
        // VENDOR do việc duyệt shop quyết định, không thêm/bớt bằng form này
        requested.remove(RoleName.VENDOR);
        if (user.hasRole(RoleName.VENDOR)) {
            requested.add(RoleName.VENDOR);
        }
        if (requested.isEmpty()) {
            throw new BusinessException("Phải chọn ít nhất một vai trò.");
        }
        if (Objects.equals(userId, actorId) && !requested.contains(RoleName.ADMIN)) {
            throw new BusinessException("Bạn không thể tự bỏ quyền Quản trị viên của mình.");
        }

        user.setStore(requested.contains(RoleName.MANAGER) ? requireStore(request.getStoreId()) : null);
        user.setCarrier(requested.contains(RoleName.SHIPPER) ? requireCarrier(request.getCarrierId()) : null);

        Set<Role> roles = new HashSet<>();
        for (RoleName name : requested) {
            roles.add(roleRepository.findByName(name)
                    .orElseThrow(() -> new IllegalStateException("Thiếu role " + name + " trong CSDL")));
        }
        user.setRoles(roles);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OptionDto> activeStores() {
        return storeRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(s -> new OptionDto(s.getId(), s.getName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OptionDto> activeCarriers() {
        return carrierRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(c -> new OptionDto(c.getId(), c.getName()))
                .toList();
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng #" + id));
    }

    private Store requireStore(Long storeId) {
        if (storeId == null) {
            throw new BusinessException("Quản lý chi nhánh phải được gán một chi nhánh.");
        }
        return storeRepository.findById(storeId)
                .filter(Store::isActive)
                .orElseThrow(() -> new BusinessException("Chi nhánh không tồn tại hoặc đã ngừng hoạt động."));
    }

    private Carrier requireCarrier(Long carrierId) {
        if (carrierId == null) {
            throw new BusinessException("Người giao hàng phải thuộc một nhà vận chuyển.");
        }
        return carrierRepository.findById(carrierId)
                .filter(Carrier::isActive)
                .orElseThrow(() -> new BusinessException("Nhà vận chuyển không tồn tại hoặc đã ngừng hoạt động."));
    }
}
