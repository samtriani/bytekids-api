package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.user.UserRequest;
import mx.bytekids.academy.dto.user.UserResponse;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.UserRole;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User findByUsername(String username) {
        return userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", username));
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    public List<UserResponse> findByRole(UserRole role) {
        return userRepository.findByRoleAndIsActiveTrue(role).stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("La contraseña es requerida para crear un usuario");
        }
        String normalizedUsername = request.getUsername().trim().toLowerCase();
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new BusinessException("El usuario '" + normalizedUsername + "' ya existe");
        }
        User user = User.builder()
                .username(normalizedUsername)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .role(request.getRole())
                .initials(request.getInitials())
                .avatarUrl(request.getAvatarUrl())
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(UUID id, UserRequest request) {
        User user = findById(id);
        String normalizedUsername = request.getUsername().trim().toLowerCase();
        if (!user.getUsername().equals(normalizedUsername) && userRepository.existsByUsername(normalizedUsername)) {
            throw new BusinessException("El usuario '" + normalizedUsername + "' ya existe");
        }
        user.setUsername(normalizedUsername);
        user.setDisplayName(request.getDisplayName());
        user.setRole(request.getRole());
        user.setInitials(request.getInitials());
        user.setAvatarUrl(request.getAvatarUrl());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deactivate(UUID id) {
        User user = findById(id);
        user.setIsActive(false);
        userRepository.save(user);
    }
}
