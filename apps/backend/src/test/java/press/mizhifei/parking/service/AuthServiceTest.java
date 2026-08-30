package press.mizhifei.parking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import press.mizhifei.parking.common.ServiceException;
import press.mizhifei.parking.domain.User;
import press.mizhifei.parking.repository.UserRepository;
import press.mizhifei.parking.security.JwtService;
import press.mizhifei.parking.web.dto.AuthDtos.LoginResponse;
import press.mizhifei.parking.web.dto.AuthDtos.UserCreateRequest;
import press.mizhifei.parking.web.dto.AuthDtos.UserView;

/**
 * Unit tests for {@link AuthService} (Admin Identity & Access Deep Module):
 * authentication flows, session creation, password lifecycle, user provisioning,
 * and admin safety invariants (self-deletion & last-admin guards).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @InjectMocks
    AuthService authService;

    private static final String ADMIN_USER = "admin";
    private static final String RAW_PASS = "admin123";
    private static final String ENCODED_PASS = "$2a$10$encodedPasswordHash";

    private User sampleUser(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setPassword(ENCODED_PASS);
        u.setNickname("管理员");
        u.setRemark("系统管理员");
        u.setCreateTime(LocalDateTime.now());
        return u;
    }

    // ─── Authentication & Session ────────────────────────────────────────────

    @Test
    void login_success_returnsTokenAndUserInfo() {
        User user = sampleUser(1L, ADMIN_USER);
        when(userRepository.findByUsername(ADMIN_USER)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASS, ENCODED_PASS)).thenReturn(true);
        when(jwtService.createToken(ADMIN_USER)).thenReturn("mock.jwt.token");

        LoginResponse response = authService.login(ADMIN_USER, RAW_PASS);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("mock.jwt.token");
        assertThat(response.username()).isEqualTo(ADMIN_USER);
        assertThat(response.nickname()).isEqualTo("管理员");
    }

    @Test
    void login_userNotFound_throwsGenericServiceExceptionWithoutOracleLeak() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nonexistent", RAW_PASS))
                .isInstanceOf(ServiceException.class)
                .hasMessage("用户名或密码错误");

        verify(jwtService, never()).createToken(any());
    }

    @Test
    void login_wrongPassword_throwsGenericServiceException() {
        User user = sampleUser(1L, ADMIN_USER);
        when(userRepository.findByUsername(ADMIN_USER)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", ENCODED_PASS)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(ADMIN_USER, "wrongpass"))
                .isInstanceOf(ServiceException.class)
                .hasMessage("用户名或密码错误");

        verify(jwtService, never()).createToken(any());
    }

    // ─── User Profile & Password Lifecycle ───────────────────────────────────

    @Test
    void me_existingUser_returnsUserView() {
        User user = sampleUser(1L, ADMIN_USER);
        when(userRepository.findByUsername(ADMIN_USER)).thenReturn(Optional.of(user));

        UserView view = authService.me(ADMIN_USER);

        assertThat(view).isNotNull();
        assertThat(view.id()).isEqualTo(1L);
        assertThat(view.username()).isEqualTo(ADMIN_USER);
        assertThat(view.nickname()).isEqualTo("管理员");
    }

    @Test
    void me_userNotFound_throwsServiceException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me("ghost"))
                .isInstanceOf(ServiceException.class)
                .hasMessage("用户不存在");
    }

    @Test
    void changePassword_success_updatesAndSavesEncodedPassword() {
        User user = sampleUser(1L, ADMIN_USER);
        when(userRepository.findByUsername(ADMIN_USER)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", ENCODED_PASS)).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("$2a$10$newEncodedPassword");

        authService.changePassword(ADMIN_USER, "oldPass", "newPass");

        assertThat(user.getPassword()).isEqualTo("$2a$10$newEncodedPassword");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_wrongOldPassword_throwsServiceException() {
        User user = sampleUser(1L, ADMIN_USER);
        when(userRepository.findByUsername(ADMIN_USER)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongOldPass", ENCODED_PASS)).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(ADMIN_USER, "wrongOldPass", "newPass"))
                .isInstanceOf(ServiceException.class)
                .hasMessage("旧密码错误");

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_userNotFound_throwsServiceException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.changePassword("ghost", "oldPass", "newPass"))
                .isInstanceOf(ServiceException.class)
                .hasMessage("用户不存在");

        verify(userRepository, never()).save(any());
    }

    // ─── Administrator Account Provisioning ──────────────────────────────────

    @Test
    void listUsers_returnsAllUsersAsViews() {
        User u1 = sampleUser(1L, "admin1");
        User u2 = sampleUser(2L, "admin2");
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<UserView> users = authService.listUsers();

        assertThat(users).hasSize(2);
        assertThat(users.get(0).username()).isEqualTo("admin1");
        assertThat(users.get(1).username()).isEqualTo("admin2");
    }

    @Test
    void createUser_success_encodesPasswordAndPersists() {
        UserCreateRequest request = new UserCreateRequest("operator", "pass123", "操作员", "备注");
        when(userRepository.existsByUsername("operator")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("$2a$10$encodedOperatorPass");

        authService.createUser(request);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_duplicateUsername_throwsServiceException() {
        UserCreateRequest request = new UserCreateRequest(ADMIN_USER, "pass123", "操作员", "备注");
        when(userRepository.existsByUsername(ADMIN_USER)).thenReturn(true);

        assertThatThrownBy(() -> authService.createUser(request))
                .isInstanceOf(ServiceException.class)
                .hasMessage("用户名已存在");

        verify(userRepository, never()).save(any());
    }

    // ─── Admin Safety Invariants (Deletion Guards) ───────────────────────────

    @Test
    void deleteUser_success_deletesTargetAdmin() {
        User targetUser = sampleUser(2L, "operator");
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(userRepository.count()).thenReturn(2L);

        authService.deleteUser(ADMIN_USER, 2L);

        verify(userRepository).delete(targetUser);
    }

    @Test
    void deleteUser_userNotFound_throwsServiceException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.deleteUser(ADMIN_USER, 99L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("用户不存在");

        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteUser_selfDeletionAttempt_throwsServiceException() {
        User currentUser = sampleUser(1L, ADMIN_USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> authService.deleteUser(ADMIN_USER, 1L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("不能删除当前登录的管理员");

        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteUser_lastRemainingAdminAttempt_throwsServiceException() {
        User targetUser = sampleUser(2L, "operator");
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(userRepository.count()).thenReturn(1L);

        assertThatThrownBy(() -> authService.deleteUser(ADMIN_USER, 2L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("不能删除最后一个管理员");

        verify(userRepository, never()).delete(any());
    }
}
