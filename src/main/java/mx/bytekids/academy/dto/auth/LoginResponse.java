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

    /**
     * El roboticito que escogio, o null si nunca escogio. Viaja en el login
     * para que el front no tenga que pedir /users/me en cada pantalla solo
     * para saber que dibujar en la barra.
     */
    private String avatarUrl;
}
