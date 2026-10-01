package com.starshop.service.impl;

import com.starshop.exception.AccountNotActivatedException;
import com.starshop.exception.BusinessException;
import com.starshop.security.JwtService;
import com.starshop.security.UserPrincipal;
import com.starshop.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** Cùng một câu cho "sai mật khẩu" và "email không tồn tại" để không dò được email. */
    static final String BAD_CREDENTIALS = "Email hoặc mật khẩu không đúng.";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    public LoginResult login(String email, String password) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        try {
            Authentication auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(normalized, password));
            UserPrincipal user = (UserPrincipal) auth.getPrincipal();
            return new LoginResult(user, jwtService.generateToken(user));
        } catch (DisabledException e) {
            // Chỉ tới được đây khi mật khẩu ĐÚNG (xem SecurityConfig.authenticationManager)
            throw new AccountNotActivatedException(normalized);
        } catch (LockedException e) {
            throw new BusinessException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        } catch (BadCredentialsException e) {
            log.info("Đăng nhập thất bại: {}", normalized);
            throw new BusinessException(BAD_CREDENTIALS);
        } catch (AuthenticationException e) {
            throw new BusinessException(BAD_CREDENTIALS);
        }
    }
}
