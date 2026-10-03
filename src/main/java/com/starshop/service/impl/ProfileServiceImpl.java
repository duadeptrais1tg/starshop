package com.starshop.service.impl;

import com.starshop.dto.RegisterRequest;
import com.starshop.dto.UploadResult;
import com.starshop.dto.account.ChangePasswordForm;
import com.starshop.dto.account.ProfileDto;
import com.starshop.dto.account.ProfileForm;
import com.starshop.entity.Role;
import com.starshop.entity.User;
import com.starshop.entity.enums.MediaType;
import com.starshop.entity.enums.RoleName;
import com.starshop.exception.BusinessException;
import com.starshop.exception.FileStorageException;
import com.starshop.exception.InvalidFileException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.UserRepository;
import com.starshop.service.FileStorageService;
import com.starshop.service.ProfileService;
import com.starshop.util.DateFormats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final String AVATAR_FOLDER = "avatars";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public ProfileDto getProfile(Long userId) {
        User user = find(userId);
        return ProfileDto.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .roleLabels(user.getRoles().stream()
                        .map(Role::getName)
                        .sorted(Comparator.naturalOrder())
                        .map(RoleName::getLabel)
                        .toList())
                .createdAt(DateFormats.dateTime(user.getCreatedAt()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileForm getForm(Long userId) {
        User user = find(userId);
        ProfileForm form = new ProfileForm();
        form.setFullName(user.getFullName());
        form.setPhone(user.getPhone());
        return form;
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, ProfileForm form) {
        if (!StringUtils.hasText(form.getFullName())) {
            throw new BusinessException("Vui lòng nhập họ tên.");
        }
        if (form.getPhone() == null || !form.getPhone().trim().matches(RegisterRequest.PHONE_REGEX)) {
            throw new BusinessException("Số điện thoại không hợp lệ.");
        }
        User user = find(userId);
        user.setFullName(form.getFullName().trim());
        user.setPhone(normalizePhone(form.getPhone()));
    }

    @Override
    @Transactional
    public void changeAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Vui lòng chọn ảnh.");
        }
        User user = find(userId);
        String oldPublicId = user.getAvatarPublicId();
        UploadResult uploaded = fileStorageService.uploadImage(file, AVATAR_FOLDER);
        user.setAvatarUrl(uploaded.url());
        user.setAvatarPublicId(uploaded.publicId());
        deleteAfterCommit(oldPublicId);
    }

    @Override
    @Transactional
    public void removeAvatar(Long userId) {
        User user = find(userId);
        String oldPublicId = user.getAvatarPublicId();
        user.setAvatarUrl(null);
        user.setAvatarPublicId(null);
        deleteAfterCommit(oldPublicId);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordForm form) {
        User user = find(userId);
        if (form.getCurrentPassword() == null || !passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng.");
        }
        if (form.getPassword() == null || !form.getPassword().matches(RegisterRequest.PASSWORD_REGEX)) {
            throw new BusinessException("Mật khẩu mới tối thiểu 8 ký tự, gồm cả chữ và số.");
        }
        if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu nhập lại không khớp.");
        }
        if (passwordEncoder.matches(form.getPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        user.setPassword(passwordEncoder.encode(form.getPassword()));
    }

    private User find(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản"));
    }

    /** +84912345678 -> 0912345678 */
    private static String normalizePhone(String phone) {
        String trimmed = phone.trim();
        return trimmed.startsWith("+84") ? "0" + trimmed.substring(3) : trimmed;
    }

    /** Xóa ảnh cũ trên Cloudinary sau khi lưu DB thành công; lỗi khi xóa chỉ ghi log. */
    private void deleteAfterCommit(String publicId) {
        if (!StringUtils.hasText(publicId)) {
            return;
        }
        Runnable delete = () -> {
            try {
                fileStorageService.delete(publicId, MediaType.IMAGE);
            } catch (FileStorageException e) {
                log.warn("Không xóa được ảnh đại diện cũ {}: {}", publicId, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delete.run();
                }
            });
        } else {
            delete.run();
        }
    }
}
