package mx.bytekids.academy.dto.user;

import lombok.Builder;
import lombok.Data;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.UserRole;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder
public class UserResponse {
    private UUID id;
    private String username;
    private String displayName;
    private UserRole role;
    private String initials;
    private String avatarUrl;
    private Boolean isActive;
    private Short age;
    private String address;
    private OffsetDateTime createdAt;

    public static UserResponse from(User u) {
        return UserResponse.builder()
                .id(u.getId()).username(u.getUsername())
                .displayName(u.getDisplayName()).role(u.getRole())
                .initials(u.getInitials()).avatarUrl(u.getAvatarUrl())
                .isActive(u.getIsActive()).age(u.getAge()).address(u.getAddress())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
