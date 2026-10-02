package com.starshop.service;

import com.starshop.dto.OptionDto;
import com.starshop.dto.admin.RoleAssignmentRequest;
import com.starshop.dto.admin.UserAdminDto;
import com.starshop.dto.admin.UserStatusFilter;
import com.starshop.entity.enums.RoleName;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Quản lý tài khoản người dùng (Admin).
 */
public interface AdminUserService {

    int PAGE_SIZE = 10;

    /**
     * @param page trang, bắt đầu từ 0
     */
    Page<UserAdminDto> search(String keyword, RoleName role, UserStatusFilter status, int page);

    /** @throws com.starshop.exception.NotFoundException không có user */
    UserAdminDto get(Long id);

    /** @throws com.starshop.exception.BusinessException tự khóa chính mình */
    void lock(Long userId, Long actorId);

    void unlock(Long userId);

    /**
     * Gán role. MANAGER phải kèm chi nhánh, SHIPPER phải kèm nhà vận chuyển; VENDOR giữ nguyên (do duyệt shop quyết định).
     *
     * @throws com.starshop.exception.BusinessException dữ liệu không hợp lệ hoặc tự bỏ quyền ADMIN của mình
     */
    void assignRoles(Long userId, RoleAssignmentRequest request, Long actorId);

    List<OptionDto> activeStores();

    List<OptionDto> activeCarriers();
}
