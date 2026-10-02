package com.starshop.dto.admin;

import com.starshop.entity.enums.RoleName;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Form gán role cho user. MANAGER cần storeId, SHIPPER cần carrierId (kiểm tra ở service).
 * VENDOR không gán tay: được cấp khi Admin duyệt shop.
 */
@Getter
@Setter
@NoArgsConstructor
public class RoleAssignmentRequest {

    @NotEmpty(message = "Phải chọn ít nhất một vai trò")
    private Set<RoleName> roles = new HashSet<>();

    private Long storeId;

    private Long carrierId;
}
