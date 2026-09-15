package mx.bytekids.academy.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Primera prueba automatizada del proyecto.
 *
 * Empieza por aqui y no por otro lado porque este filtro es el unico codigo
 * que puede dejar a un usuario legitimo fuera de la plataforma. Un error de
 * mas en el contador y una maestra no entra a dar su clase; uno de menos y el
 * freno no sirve. Ademas es de las pocas piezas que se pueden probar sin base
 * de datos: no toca repositorios ni red.
 */
class LoginRateLimitFilterTest {

    private static final int MAX = 3;

    private LoginRateLimitFilter filtro;

    @BeforeEach
    void setUp() {
        filtro = new LoginRateLimitFilter();
        // Los valores vienen de application.yml via @Value, que no se inyecta
        // al construir la clase a mano.
        ReflectionTestUtils.setField(filtro, "maxIntentos", MAX);
        ReflectionTestUtils.setField(filtro, "ventanaMinutos", 15);
    }

    /** Un POST a /auth/login desde una IP, con el estado que devolvera el chain. */
    private MockHttpServletResponse intento(String ip, int estadoDeRespuesta) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRequestURI("/api/auth/login");
        req.addHeader("Fly-Client-IP", ip);

        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = (a, b) -> ((MockHttpServletResponse) b).setStatus(estadoDeRespuesta);

        filtro.doFilter(req, res, chain);
        return res;
    }

    @Test
    @DisplayName("Deja pasar hasta el tope y bloquea el siguiente con 429")
    void bloqueaDespuesDelTope() throws Exception {
        for (int i = 0; i < MAX; i++) {
            assertThat(intento("10.0.0.1", 401).getStatus())
                    .as("intento fallido %d: todavia debe llegar al backend", i + 1)
                    .isEqualTo(401);
        }

        MockHttpServletResponse bloqueado = intento("10.0.0.1", 401);
        assertThat(bloqueado.getStatus()).isEqualTo(429);
        assertThat(bloqueado.getHeader("Retry-After")).isNotNull();
        assertThat(bloqueado.getContentAsString()).contains("Demasiados intentos");
    }

    @Test
    @DisplayName("Un login correcto borra el contador")
    void elExitoLimpiaElContador() throws Exception {
        intento("10.0.0.2", 401);
        intento("10.0.0.2", 401);

        assertThat(intento("10.0.0.2", 200).getStatus()).isEqualTo(200);

        // Si el acierto no hubiera limpiado, estos tres fallos sumarian cinco
        // y el ultimo saldria 429.
        for (int i = 0; i < MAX; i++) {
            assertThat(intento("10.0.0.2", 401).getStatus()).isEqualTo(401);
        }
    }

    @Test
    @DisplayName("El castigo es por IP y no arrastra a los demas")
    void elBloqueoNoSeContagia() throws Exception {
        for (int i = 0; i <= MAX; i++) intento("10.0.0.3", 401);
        assertThat(intento("10.0.0.3", 401).getStatus()).isEqualTo(429);

        // Este es el escenario que hace importante leer Fly-Client-IP: sin esa
        // cabecera todos compartirian la IP del proxy y el bloqueo de uno
        // sacaria al salon entero.
        assertThat(intento("10.0.0.4", 401).getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Un 500 no cuenta como intento fallido")
    void losErroresDelServidorNoCastiganAlUsuario() throws Exception {
        for (int i = 0; i < MAX + 2; i++) {
            assertThat(intento("10.0.0.5", 500).getStatus()).isEqualTo(500);
        }
    }

    @Test
    @DisplayName("Solo se vigila POST /auth/login")
    void noEstorbaAlResto() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/content/feed");
        req.setRequestURI("/api/content/feed");
        assertThat(filtro.shouldNotFilter(req)).isTrue();

        MockHttpServletRequest login = new MockHttpServletRequest("POST", "/api/auth/login");
        login.setRequestURI("/api/auth/login");
        assertThat(filtro.shouldNotFilter(login)).isFalse();
    }
}
