package mx.bytekids.academy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Freno de fuerza bruta para /auth/login.
 *
 * Era el unico endpoint publico que acepta credenciales y no tenia ningun
 * limite: se le podian tirar miles de combinaciones por minuto sin costo. En
 * una plataforma donde los usuarios son ninos el riesgo es peor de lo normal,
 * porque las contrasenas de alumno las asigna coordinacion y tienden a ser
 * cortas y parecidas entre si.
 *
 * <h3>Como cuenta</h3>
 * Solo cuentan los intentos <b>fallidos</b> (401). Un login correcto borra el
 * contador, asi que quien sabe su contrasena nunca ve este filtro, por mucho
 * que se equivoque antes. La ventana es deslizante por reloj: pasados
 * {@code VENTANA} minutos sin fallar, el contador se reinicia solo.
 *
 * <h3>Por que en memoria y no en Redis</h3>
 * Hoy corre una sola maquina en Fly ({@code min_machines_running = 0}, una
 * instancia). Un contador en memoria la cubre entera. El dia que haya dos
 * maquinas esto seguira funcionando pero con el doble de margen efectivo, que
 * es una degradacion aceptable y no un agujero: hay que moverlo a un almacen
 * compartido cuando se escale, no antes.
 *
 * <h3>La IP detras del proxy</h3>
 * Fly termina TLS y reenvia, asi que {@code getRemoteAddr()} devuelve siempre
 * la IP del proxy: sin leer las cabeceras, todos los usuarios del mundo
 * compartirian un solo contador y el primero en fallar cinco veces dejaria
 * fuera a los demas. {@code Fly-Client-IP} la pone el proxy de Fly y el
 * cliente no la puede falsificar.
 */
@Component
@Slf4j
public class LoginRateLimitFilter extends OncePerRequestFilter {

    /** Fallos tolerados por IP dentro de la ventana. */
    @Value("${app.security.login.max-intentos:10}")
    private int maxIntentos;

    /** Minutos que dura el castigo y tambien la ventana de conteo. */
    @Value("${app.security.login.ventana-minutos:15}")
    private int ventanaMinutos;

    private final Map<String, Intentos> porIp = new ConcurrentHashMap<>();

    /** Fallos acumulados de una IP y el momento del ultimo. */
    private static final class Intentos {
        final AtomicInteger fallos = new AtomicInteger();
        volatile Instant ultimo = Instant.now();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                 && request.getRequestURI().endsWith("/auth/login"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String ip = ipDelCliente(request);
        final Duration ventana = Duration.ofMinutes(ventanaMinutos);

        limpiarVencidos(ventana);

        Intentos intentos = porIp.get(ip);
        if (intentos != null && vigente(intentos, ventana) && intentos.fallos.get() >= maxIntentos) {
            long faltan = ventana.minus(Duration.between(intentos.ultimo, Instant.now())).toSeconds();
            log.warn("Login bloqueado por demasiados intentos fallidos (ip={}, fallos={})",
                    ip, intentos.fallos.get());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(Math.max(faltan, 1)));
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Demasiados intentos fallidos. "
                    + "Espera unos minutos antes de volver a intentar.\"}");
            return;
        }

        filterChain.doFilter(request, response);

        // El resultado se lee del estado de la respuesta: 401 es credencial
        // incorrecta. Un 400 (peticion mal formada) o un 500 no cuentan, para
        // no castigar a alguien por un error nuestro.
        if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
            Intentos registro = porIp.computeIfAbsent(ip, k -> new Intentos());
            if (!vigente(registro, ventana)) registro.fallos.set(0);
            registro.fallos.incrementAndGet();
            registro.ultimo = Instant.now();
        } else if (response.getStatus() < 400) {
            porIp.remove(ip);
        }
    }

    private boolean vigente(Intentos intentos, Duration ventana) {
        return Duration.between(intentos.ultimo, Instant.now()).compareTo(ventana) < 0;
    }

    /**
     * Sin esto el mapa solo creceria: cada IP que falla una vez se quedaria
     * dentro para siempre y bastaria con rotar direcciones para llenar la
     * memoria de la maquina.
     */
    private void limpiarVencidos(Duration ventana) {
        if (porIp.size() < 1000) return;
        porIp.entrySet().removeIf(e -> !vigente(e.getValue(), ventana));
    }

    private String ipDelCliente(HttpServletRequest request) {
        String fly = request.getHeader("Fly-Client-IP");
        if (fly != null && !fly.isBlank()) return fly.trim();

        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            // Es una lista "cliente, proxy1, proxy2": la primera es el cliente.
            return reenviada.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
