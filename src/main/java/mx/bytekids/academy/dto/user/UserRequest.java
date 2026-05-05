package mx.bytekids.academy.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import mx.bytekids.academy.entity.enums.UserRole;

@Data
public class UserRequest {
    @NotBlank(message = "El nombre de usuario es requerido")
    @Size(min = 3, max = 50)
    private String username;

    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @NotBlank(message = "El nombre es requerido")
    private String displayName;

    @NotNull(message = "El rol es requerido")
    private UserRole role;

    private String initials;
    private String avatarUrl;
    private Short age;
    private String address;
}
