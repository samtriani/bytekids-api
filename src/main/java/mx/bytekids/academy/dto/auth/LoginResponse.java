package mx.bytekids.academy.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data @Builder
public class LoginResponse {
    private String token;
    private String tokenType;
    private UUID userId;
    private String username;
    private String displayName;
    private String role;
    /** true si puede crear/modificar cuentas de coordinador y director. */
    private boolean owner;
}
