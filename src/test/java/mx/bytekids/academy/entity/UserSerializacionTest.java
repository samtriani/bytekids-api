package mx.bytekids.academy.entity;

import mx.bytekids.academy.dto.message.MessageResponse;
import mx.bytekids.academy.entity.enums.UserRole;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El hash de la contrasena nunca sale por la API.
 *
 * Varias rutas devuelven entidades crudas en vez de DTOs --mensajes,
 * notificaciones, intentos de quiz-- y casi todas traen un User adentro. Los
 * mensajes son los peores porque el User de adentro es OTRA persona: un
 * alumno que abria su bandeja recibia el hash de su maestra.
 *
 * Se prueba con el mismo Jackson 3 que usa Spring MVC (tools.jackson), no con
 * el 2 que viene de jjwt: probar con otro serializador no probaria nada.
 */
class UserSerializacionTest {

    private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();

    private User maestra() {
        return User.builder()
                .id(UUID.randomUUID())
                .username("laura")
                .passwordHash("$2a$10$hashDePruebaQueNoDebeSalirNuncaaaaaaaaaaaaaaaaaaaa")
                .displayName("Laura Maestra")
                .role(UserRole.teacher)
                .initials("LM")
                .avatarUrl("bot-luna")
                .build();
    }

    @Test
    void unUsuarioSerializadoNoLlevaSuHash() throws Exception {
        String json = mapper.writeValueAsString(maestra());

        assertThat(json).doesNotContain("passwordHash");
        assertThat(json).doesNotContain("$2a$10$");
        // Lo que si tiene que seguir saliendo: sin esto los avatares no pintan.
        assertThat(json).contains("\"avatarUrl\":\"bot-luna\"");
    }

    @Test
    void unMensajeNoLlevaElHashDeNingunoDeLosDos() throws Exception {
        User alumno = User.builder()
                .id(UUID.randomUUID()).username("victoria")
                .passwordHash("$2a$10$otroHashQueTampocoaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .displayName("Victoria").role(UserRole.student).build();

        Message m = Message.builder()
                .id(UUID.randomUUID())
                .sender(alumno)
                .recipient(maestra())
                .body("Maestra, tengo una duda")
                .build();

        String json = mapper.writeValueAsString(m);

        assertThat(json).doesNotContain("passwordHash");
        assertThat(json).doesNotContain("$2a$10$");
    }

    @Test
    void laRespuestaDeUnMensajeSoloTraeLoQueSePinta() throws Exception {
        User alumno = User.builder()
                .id(UUID.randomUUID()).username("victoria").passwordHash("x")
                .displayName("Victoria").role(UserRole.student)
                .email("papa.de.victoria@correo.com").age((short) 10)
                .address("Calle Falsa 123").build();

        Message m = Message.builder()
                .id(UUID.randomUUID()).sender(alumno).recipient(maestra())
                .body("Maestra, tengo una duda").build();

        String json = mapper.writeValueAsString(MessageResponse.from(m));

        // Lo de un menor no le llega a nadie por un mensaje.
        assertThat(json).doesNotContain("papa.de.victoria", "Calle Falsa", "\"age\"", "\"address\"", "\"email\"");
        // Ni media credencial de acceso.
        assertThat(json).doesNotContain("\"username\"");
        // Lo que la conversacion si necesita.
        assertThat(json).contains("\"displayName\":\"Victoria\"", "\"avatarUrl\":\"bot-luna\"", "\"role\":\"teacher\"");
    }
}
