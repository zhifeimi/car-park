package press.mizhifei.parking.service;

import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import press.mizhifei.parking.common.ServiceException;
import press.mizhifei.parking.domain.User;
import press.mizhifei.parking.repository.UserRepository;
import press.mizhifei.parking.security.JwtService;
import press.mizhifei.parking.web.dto.AuthDtos.LoginResponse;
import press.mizhifei.parking.web.dto.AuthDtos.UserCreateRequest;
import press.mizhifei.parking.web.dto.AuthDtos.UserView;

/**
 * 管理员身份与认证管理 (Admin Identity & Access Deep Module).
 * <p>
 * Consolidates credential verification, stateless HS256 JWT sessions, password changes,
 * and administrator account lifecycle management with built-in safety invariants.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(String username, String password) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new ServiceException("用户名或密码错误");
        }
        String token = jwtService.createToken(user.getUsername());
        return new LoginResponse(token, user.getUsername(), user.getNickname());
    }

    public UserView me(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ServiceException("用户不存在"));
        return toView(user);
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ServiceException("用户不存在"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new ServiceException("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public List<UserView> listUsers() {
        return userRepository.findAll().stream()
                .map(AuthService::toView)
                .toList();
    }

    @Transactional
    public void createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ServiceException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setRemark(request.remark());
        userRepository.save(user);
    }

    /** Guards: cannot delete yourself, and cannot delete the last remaining admin. */
    @Transactional
    public void deleteUser(String currentUsername, Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ServiceException("用户不存在"));
        if (user.getUsername().equals(currentUsername)) {
            throw new ServiceException("不能删除当前登录的管理员");
        }
        if (userRepository.count() <= 1) {
            throw new ServiceException("不能删除最后一个管理员");
        }
        userRepository.delete(user);
    }

    public static UserView toView(User user) {
        return new UserView(user.getId(), user.getUsername(), user.getNickname(),
                user.getRemark(), user.getCreateTime());
    }
}
