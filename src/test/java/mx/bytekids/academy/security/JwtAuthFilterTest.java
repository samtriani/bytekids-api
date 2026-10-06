package mx.bytekids.academy.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.sql.SQLTransientConnectionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Un fallo de la base al validar la sesion NO cierra la sesion. Antes se
 * tragaba el error, la peticion seguia sin autenticar, salia 401 y el front
 * mandaba al nino al login justo al enviar su quiz.
 */
class JwtAuthFilterTest {

    private final JwtUtil jwt = mock(JwtUtil.class);
    private final UserDetailsService usuarios = mock(UserDetailsService.class);
    private final JwtAuthFilter filtro = new JwtAuthFilter(jwt, usuarios);

    @AfterEach
    void limpiar() { SecurityContextHolder.clearContext(); }

    private MockHttpServletRequest conToken() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/quizzes/x/attempts");
        req.addHeader("Authorization", "Bearer token-valido");
        when(jwt.extractUsername("token-valido")).thenReturn("pedro");
        return req;
    }

    @Test
    void siLaBaseNoRespondeEs503YNoSigueSinSesion() throws Exception {
        when(usuarios.loadUserByUsername("pedro")).thenThrow(new CannotCreateTransactionException("sin conexion",
                new SQLTransientConnectionException("ByteKidsPool - Connection is not available")));
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain cadena = mock(FilterChain.class);

        filtro.doFilter(conToken(), res, cadena);

        assertThat(res.getStatus()).isEqualTo(503);
        assertThat(res.getContentAsString()).contains("Vuelve a intentarlo");
        verify(cadena, never()).doFilter(any(), any());
    }

    @Test
    void unUsuarioQueYaNoExisteSigueSinSesion() throws Exception {
        when(usuarios.loadUserByUsername("pedro")).thenThrow(new UsernameNotFoundException("no existe"));
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain cadena = mock(FilterChain.class);

        filtro.doFilter(conToken(), res, cadena);

        // Sigue la cadena sin autenticar: la seguridad respondera 401.
        verify(cadena).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
