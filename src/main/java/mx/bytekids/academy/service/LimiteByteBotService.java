package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Cuantos mensajes le quedan hoy a un alumno con ByteBot.
 *
 * Cada mensaje le cuesta a la plataforma, y con cuentas gratuitas hace falta
 * un tope. Es por MENSAJES y no por tokens: cada mensaje ya viene acotado en
 * largo (AiTutorService.MAX_CARACTERES) y en historial (MAX_TURNOS), asi que
 * el costo por mensaje ya tiene techo. Y un nino entiende "mensajes"; tokens
 * no.
 *
 * Solo alumnos. Maestros, familias y coordinacion no tienen tope.
 *
 * El nino NO ve un contador: el tope no se presenta como restriccion. Cuando
 * se acaba, ByteBot se lo dice en el mismo chat, con carino (ver
 * AiTutorService.SIN_BATERIA). Se recarga a medianoche, hora del centro de
 * Mexico.
 *
 * VA EN LA BASE Y NO EN MEMORIA
 * Las maquinas de Fly se suspenden y son dos: un contador en memoria se
 * reiniciaria cada vez que la maquina se duerme y contaria distinto en cada
 * una.
 *
 * SI LA TABLA NO EXISTE, NO ESTORBA
 * Si ai_uso_diario todavia no se crea (sql/2026-09-30_...), ByteBot sigue
 * funcionando sin tope y se deja un aviso en el log. Un tope que tumba a
 * ByteBot es peor que no tener tope.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LimiteByteBotService {

    private static final ZoneId MEXICO = ZoneId.of("America/Mexico_City");

    private final JdbcTemplate jdbc;

    @Value("${app.ai.limite-diario-alumno:25}")
    private int limiteDiario;

    private static boolean tieneTope(User u) {
        return u != null && u.getRole() == UserRole.student;
    }

    private static LocalDate hoy() { return LocalDate.now(MEXICO); }

    /** ¿Le queda al menos un mensaje hoy? */
    public boolean puedeHablar(User u) {
        if (!tieneTope(u) || limiteDiario <= 0) return true;
        try {
            List<Integer> usados = jdbc.queryForList(
                    "SELECT mensajes FROM ai_uso_diario WHERE user_id = ? AND fecha = ?",
                    Integer.class, u.getId(), hoy());
            return usados.isEmpty() || usados.get(0) < limiteDiario;
        } catch (DataAccessException e) {
            log.warn("Limite de ByteBot sin tabla ai_uso_diario (se deja pasar): {}", e.getMessage());
            return true;
        }
    }

    /** Cuenta un mensaje que ByteBot SI contesto. Los errores no le gastan al nino. */
    public void contar(User u) {
        if (!tieneTope(u)) return;
        try {
            jdbc.update("""
                    INSERT INTO ai_uso_diario (user_id, fecha, mensajes) VALUES (?, ?, 1)
                    ON CONFLICT (user_id, fecha) DO UPDATE SET mensajes = ai_uso_diario.mensajes + 1
                    """, u.getId(), hoy());
        } catch (DataAccessException e) {
            log.warn("No se pudo contar el mensaje de ByteBot: {}", e.getMessage());
        }
    }
}
