package com.starshop.service;

import com.starshop.dto.account.ChangePasswordForm;
import com.starshop.dto.account.ProfileDto;
import com.starshop.dto.account.ProfileForm;
import org.springframework.web.multipart.MultipartFile;

/**
 * Hồ sơ của người đang đăng nhập. Mọi hàm nhận userId lấy từ phiên đăng nhập, không lấy từ URL.
 */
public interface ProfileService {

    ProfileDto getProfile(Long userId);

    ProfileForm getForm(Long userId);

    void updateProfile(Long userId, ProfileForm form);

    /**
     * Đổi ảnh đại diện (upload lên Cloudinary), ảnh cũ bị xóa sau khi lưu thành công.
     *
     * @throws com.starshop.exception.InvalidFileException  file sai định dạng / quá 5MB
     * @throws com.starshop.exception.FileStorageException lỗi Cloudinary
     */
    void changeAvatar(Long userId, MultipartFile file);

    void removeAvatar(Long userId);

    /**
     * @throws com.starshop.exception.BusinessException sai mật khẩu hiện tại / mật khẩu mới không hợp lệ
     */
    void changePassword(Long userId, ChangePasswordForm form);
}
