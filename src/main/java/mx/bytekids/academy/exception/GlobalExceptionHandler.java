package mx.bytekids.academy.exception;

import lombok.extern.slf4j.Slf4j;
import mx.bytekids.academy.dto.common.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Credenciales inválidas"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tienes permisos para realizar esta acción"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error de validación", errors));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String msg = "Registro duplicado o violación de restricción de base de datos";
        String detail = ex.getMostSpecificCause().getMessage();
        if (detail != null && detail.contains("duplicate key")) {
            if (detail.contains("username"))  msg = "El nombre de usuario ya existe";
            else if (detail.contains("name")) msg = "Ya existe un registro con ese nombre";
            else                              msg = "Ya existe un registro con esos datos";
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(msg));
    }

    /** Lo que se le dice a quien llega cuando la base todavia no despierta. */
    public static final String DESPERTANDO =
            "Estamos despertando el servidor. Vuelve a intentarlo en unos segundos, por favor.";

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        // El primer acceso del dia despierta a Fly y a Neon. Si la base no
        // alcanza a conectar, antes salia "Error interno del servidor" (500),
        // que el front no reintenta. Es transitorio: 503, que si reintenta solo.
        if (esBaseDespertando(ex)) {
            log.warn("La base no respondio a tiempo (despertando): {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(DESPERTANDO));
        }
        log.error("Error no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error interno del servidor"));
    }

    /**
     * Si en la cadena de causas hay un fallo de CONEXION a la base (no un
     * error de SQL ni de datos). Se compara por nombre para no amarrarse a
     * Hikari ni a Hibernate.
     */
    static boolean esBaseDespertando(Throwable ex) {
        java.util.Set<String> conexion = java.util.Set.of(
                "org.springframework.transaction.CannotCreateTransactionException",
                "org.springframework.dao.DataAccessResourceFailureException",
                "org.springframework.jdbc.CannotGetJdbcConnectionException",
                "org.hibernate.exception.JDBCConnectionException",
                "java.sql.SQLTransientConnectionException",
                "java.net.ConnectException",
                "java.net.SocketTimeoutException");
        for (Throwable t = ex; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (conexion.contains(t.getClass().getName())) return true;
        }
        return false;
    }
}
