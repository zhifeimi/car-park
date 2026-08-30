package press.mizhifei.parking.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 管理员 — table {@code cp_user} (new table; replaces the legacy {@code sys_user} RBAC stack).
 *
 * <p>Single implicit admin role: whoever can log in can use the whole admin console.
 * Passwords are BCrypt hashes; the {@link #password} field never leaves the server
 * ({@link JsonIgnore} + explicit {@code UserView} DTOs).
 */
@Entity
@Table(name = "cp_user")
public class User extends BaseEntity {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 登录名（唯一） */
    @Column(name = "username", length = 64, nullable = false, unique = true)
    private String username;

    /** BCrypt 密码 — never serialized */
    @JsonIgnore
    @Column(name = "password", length = 100, nullable = false)
    private String password;

    /** 昵称 */
    @Column(name = "nickname", length = 64)
    private String nickname;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
