package com.starshop.service.impl;

import com.starshop.dto.UploadResult;
import com.starshop.dto.account.ChangePasswordForm;
import com.starshop.dto.account.ProfileForm;
import com.starshop.entity.User;
import com.starshop.entity.enums.MediaType;
import com.starshop.exception.BusinessException;
import com.starshop.exception.InvalidFileException;
import com.starshop.repository.UserRepository;
import com.starshop.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final FileStorageService storage = mock(FileStorageService.class);
    private final ProfileServiceImpl service = new ProfileServiceImpl(userRepository, encoder, storage);

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().fullName("An").email("an@gmail.com").phone("0912345678")
                .password(encoder.encode("CuMatKhau1")).build();
        user.setId(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
    }

    @Test
    void updateProfile_changesNameAndNormalizesPhone_butNotEmail() {
        ProfileForm form = new ProfileForm();
        form.setFullName("  Nguyễn Văn An ");
        form.setPhone("+84987654321");

        service.updateProfile(5L, form);

        assertThat(user.getFullName()).isEqualTo("Nguyễn Văn An");
        assertThat(user.getPhone()).isEqualTo("0987654321");
        assertThat(user.getEmail()).isEqualTo("an@gmail.com");
    }

    @Test
    void changePassword_requiresCorrectCurrentPassword() {
        assertThatThrownBy(() -> service.changePassword(5L, pw("SaiMatKhau1", "MoiMatKhau2", "MoiMatKhau2")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("hiện tại không đúng");
        assertThat(encoder.matches("CuMatKhau1", user.getPassword())).isTrue();

        service.changePassword(5L, pw("CuMatKhau1", "MoiMatKhau2", "MoiMatKhau2"));
        assertThat(encoder.matches("MoiMatKhau2", user.getPassword())).isTrue();
    }

    @Test
    void changePassword_newMustDifferAndMatchRules() {
        assertThatThrownBy(() -> service.changePassword(5L, pw("CuMatKhau1", "CuMatKhau1", "CuMatKhau1")))
                .hasMessageContaining("khác mật khẩu hiện tại");
        assertThatThrownBy(() -> service.changePassword(5L, pw("CuMatKhau1", "yeu", "yeu")))
                .hasMessageContaining("tối thiểu 8");
        assertThatThrownBy(() -> service.changePassword(5L, pw("CuMatKhau1", "MoiMatKhau2", "Khac12345")))
                .hasMessageContaining("không khớp");
    }

    @Test
    void changeAvatar_uploadsAndDeletesOldOne() {
        user.setAvatarPublicId("starshop/avatars/old");
        when(storage.uploadImage(any(), eq("avatars")))
                .thenReturn(new UploadResult("https://img/new.jpg", "starshop/avatars/new", MediaType.IMAGE));

        service.changeAvatar(5L, new MockMultipartFile("avatar", "a.jpg", "image/jpeg", new byte[]{1}));

        assertThat(user.getAvatarUrl()).isEqualTo("https://img/new.jpg");
        assertThat(user.getAvatarPublicId()).isEqualTo("starshop/avatars/new");
        verify(storage).delete("starshop/avatars/old", MediaType.IMAGE);
    }

    @Test
    void changeAvatar_withoutFile_isRejected() {
        assertThatThrownBy(() -> service.changeAvatar(5L, null)).isInstanceOf(InvalidFileException.class);
        verify(storage, never()).uploadImage(any(), anyString());
    }

    @Test
    void removeAvatar_clearsAndDeletes() {
        user.setAvatarUrl("https://img/old.jpg");
        user.setAvatarPublicId("starshop/avatars/old");

        service.removeAvatar(5L);

        assertThat(user.getAvatarUrl()).isNull();
        verify(storage).delete("starshop/avatars/old", MediaType.IMAGE);
    }

    private static ChangePasswordForm pw(String current, String password, String confirm) {
        ChangePasswordForm f = new ChangePasswordForm();
        f.setCurrentPassword(current);
        f.setPassword(password);
        f.setConfirmPassword(confirm);
        return f;
    }
}
