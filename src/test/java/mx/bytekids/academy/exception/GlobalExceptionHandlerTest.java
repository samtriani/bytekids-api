package mx.bytekids.academy.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.sql.SQLTransientConnectionException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El primer acceso del dia: si la base todavia no despierta, la respuesta es
 * 503 (el front reintenta solo) y no "Error interno del servidor".
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void laBaseDespertandoEnElLoginEs503ConMensajeParaVolverAIntentar() {
        // Asi llega desde el login: Spring Security envuelve el fallo de conexion.
        Exception ex = new InternalAuthenticationServiceException("no se pudo cargar el usuario",
                new CannotCreateTransactionException("Could not open JPA EntityManager",
                        new SQLTransientConnectionException("HikariPool-1 - Connection is not available")));

        var r = handler.handleGeneral(ex);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(r.getBody().getMessage()).contains("Vuelve a intentarlo");
    }

    @Test
    void unErrorDeVerdadSigueSiendo500() {
        var r = handler.handleGeneral(new IllegalStateException("algo se rompio"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
