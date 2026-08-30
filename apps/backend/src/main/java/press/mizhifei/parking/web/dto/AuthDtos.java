package press.mizhifei.parking.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Auth/user request + view DTOs.
 *
 * <p>Hardening vs legacy: the legacy login challenge/response flow is gone — HTTPS plus per-IP login
 * rate limiting replace it. Passwords must be 8–64 chars.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password) {
    }

    public record LoginResponse(String token, String username, String nickname) {
    }

    public record PasswordChangeRequest(
            @NotBlank(message = "旧密码不能为空") String oldPassword,
            @NotBlank(message = "新密码不能为空") @Size(min = 8, max = 64, message = "新密码长度必须在8到64个字符之间") String newPassword) {
    }

    public record UserCreateRequest(
            @NotBlank(message = "用户名不能为空")
            @Pattern(regexp = "[a-zA-Z0-9_]{3,30}", message = "用户名只能包含字母、数字和下划线，长度3到30") String username,
            @NotBlank(message = "密码不能为空")
            @Size(min = 8, max = 64, message = "密码长度必须在8到64个字符之间") String password,
            String nickname,
            String remark) {
    }

    /** Admin view of a user — never exposes the password hash. */
    public record UserView(
            Long id,
            String username,
            String nickname,
            String remark,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime) {
    }
}
