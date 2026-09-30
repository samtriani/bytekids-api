package mx.bytekids.academy.service;

import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * El tope diario de ByteBot. Lo que no se puede romper: que un alumno con
 * mensajes pueda hablar, que al llegar al tope se detenga, que maestros y
 * familias nunca tengan tope, y que si la tabla no existe ByteBot siga vivo.
 */
class LimiteByteBotServiceTest {

    private JdbcTemplate jdbc;
    private LimiteByteBotService limite;

    private static User con(UserRole rol) {
        return User.builder().id(UUID.randomUUID()).username("u").role(rol).build();
    }

    @BeforeEach
    void preparar() {
        jdbc = mock(JdbcTemplate.class);
        limite = new LimiteByteBotService(jdbc);
        ReflectionTestUtils.setField(limite, "limiteDiario", 25);
    }

    private void usados(Integer... n) {
        when(jdbc.queryForList(anyString(), eq(Integer.class), any(), any())).thenReturn(List.of(n));
    }

    @Test
    void elPrimerMensajeDelDiaPasa() {
        usados();
        assertThat(limite.puedeHablar(con(UserRole.student))).isTrue();
    }

    @Test
    void conVeinticuatroTodaviaPuedeYConVeinticincoYaNo() {
        usados(24);
        assertThat(limite.puedeHablar(con(UserRole.student))).isTrue();
        usados(25);
        assertThat(limite.puedeHablar(con(UserRole.student))).isFalse();
    }

    @Test
    void maestrosYFamiliasNoTienenTopeNiSeCuentan() {
        User maestra = con(UserRole.teacher);
        User papa = con(UserRole.parent);
        assertThat(limite.puedeHablar(maestra)).isTrue();
        assertThat(limite.puedeHablar(papa)).isTrue();
        limite.contar(maestra);
        limite.contar(papa);
        verifyNoInteractions(jdbc);
    }

    @Test
    void sinLaTablaByteBotSigueVivo() {
        when(jdbc.queryForList(anyString(), eq(Integer.class), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("relation ai_uso_diario does not exist"));
        when(jdbc.update(anyString(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("relation ai_uso_diario does not exist"));
        User nino = con(UserRole.student);
        assertThat(limite.puedeHablar(nino)).isTrue();
        limite.contar(nino);   // no truena
    }
}
