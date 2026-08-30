package press.mizhifei.parking.web;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import press.mizhifei.parking.security.SecurityUtils;
import press.mizhifei.parking.service.AuthService;
import press.mizhifei.parking.web.dto.AuthDtos.LoginRequest;
import press.mizhifei.parking.web.dto.AuthDtos.LoginResponse;
import press.mizhifei.parking.web.dto.AuthDtos.PasswordChangeRequest;
import press.mizhifei.parking.web.dto.AuthDtos.UserCreateRequest;
import press.mizhifei.parking.web.dto.AuthDtos.UserView;
import press.mizhifei.parking.web.envelope.R;
import press.mizhifei.parking.web.envelope.TableData;

/**
 * 登录与管理员管理 — the rebuilt admin backend's auth surface.
 * Only {@code POST /auth/login} is anonymous (rate-limited).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request.username(), request.password()));
    }

    @GetMapping("/me")
    public R<UserView> me() {
        return R.ok(authService.me(SecurityUtils.currentUsername()));
    }

    @PostMapping("/password")
    public R<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(SecurityUtils.currentUsername(),
                request.oldPassword(), request.newPassword());
        return R.ok();
    }

    @GetMapping("/users")
    public TableData users() {
        List<UserView> rows = authService.listUsers();
        return new TableData(rows.size(), rows);
    }

    @PostMapping("/users")
    public R<Void> addUser(@Valid @RequestBody UserCreateRequest request) {
        authService.createUser(request);
        return R.ok();
    }

    @DeleteMapping("/users/{id}")
    public R<Void> deleteUser(@PathVariable Long id) {
        authService.deleteUser(SecurityUtils.currentUsername(), id);
        return R.ok();
    }
}
