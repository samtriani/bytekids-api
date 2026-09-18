package mx.bytekids.academy.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Cambiar la contrasena propia.
 *
 * Pide la ACTUAL a proposito, aunque la sesion ya este abierta. No es para
 * saber quien eres --eso ya lo dice el token-- sino para que una sesion
 * olvidada en una tablet del salon no le sirva a otro nino para dejar a su
 * companero fuera de su cuenta.
 *
 * Por eso tampoco lleva el id del usuario: se cambia la del dueno del token y
 * de nadie mas. Un id en el cuerpo seria una puerta para cambiarle la
 * contrasena a otro.
 */
@Data
public class CambioContrasenaRequest {

    @NotBlank(message = "Escribe tu contraseña actual")
    private String actual;

    @NotBlank(message = "Escribe tu nueva contraseña")
    @Size(min = 6, message = "La nueva contraseña debe tener al menos 6 caracteres")
    private String nueva;
}
