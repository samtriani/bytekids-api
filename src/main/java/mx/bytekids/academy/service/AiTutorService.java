package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bytekids.academy.entity.Classroom;
import mx.bytekids.academy.entity.StudentSubjectProgress;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.repository.ClassroomRepository;
import mx.bytekids.academy.repository.ParentStudentRepository;
import mx.bytekids.academy.repository.StudentSubjectProgressRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTutorService {

    private final StudentSubjectProgressRepository progressRepo;
    private final ClassroomRepository              classroomRepo;
    private final ParentStudentRepository          parentStudentRepo;
    private final RestTemplate                     restTemplate;

    @Value("${app.ai.base-url:http://127.0.0.1:1234/v1}")
    private String aiBaseUrl;

    @Value("${app.ai.model:meta-llama-3.1-8b-instruct}")
    private String aiModel;

    @Value("${app.ai.max-tokens:600}")
    private int maxTokens;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @PostConstruct
    public void logConfig() {
        boolean hasKey = apiKey != null && !apiKey.isBlank();
        String provider = hasKey ? "☁️  GROQ (cloud)" : "🖥️  LM Studio (local)";
        log.info("🤖 ByteBot LLM → {} | URL: {} | Modelo: {}", provider, aiBaseUrl, aiModel);
    }

    public String chat(User user, List<Map<String, String>> history, String message) {
        String systemPrompt = buildSystemPrompt(user);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        int start = Math.max(0, history.size() - 10);
        messages.addAll(history.subList(start, history.size()));
        messages.add(Map.of("role", "user", "content", message));

        try {
            return callLlm(messages);
        } catch (Exception e) {
            log.error("ByteBot error: {}", e.getMessage());
            return "¡Ups! Estoy teniendo problemas técnicos. Intenta de nuevo en un momento 🔧";
        }
    }

    // ── System prompts por rol ────────────────────────────────────────────────

    private String buildSystemPrompt(User user) {
        return switch (user.getRole()) {
            case student  -> buildStudentPrompt(user);
            case teacher  -> buildTeacherPrompt(user);
            case parent   -> buildParentPrompt(user);
            case director -> buildDirectorPrompt(user);
            case admin    -> buildAdminPrompt(user);
        };
    }

    private String buildStudentPrompt(User user) {
        List<StudentSubjectProgress> progress = progressRepo.findByStudentOrderByXpInSubjectDesc(user);
        int totalXp = progress.stream().mapToInt(StudentSubjectProgress::getXpInSubject).sum();
        String subjects = progress.stream()
                .map(p -> p.getSubject().getName())
                .collect(Collectors.joining(", "));
        String level = totalXp >= 500 ? "avanzado" : totalXp >= 200 ? "intermedio" : "principiante";
        String subjectList = subjects.isBlank() ? "aún sin asignar" : subjects;

        return """
                Eres ByteBot, el tutor de programación de ByteKids Academy.
                Hablas en español, de forma amigable, motivadora y apropiada para niños.
                Explicas los conceptos con analogías simples y ejemplos prácticos.
                Usas emojis ocasionalmente. Respuestas concisas (máximo 200 palabras).
                Cuando muestres código, usa bloques con la sintaxis correcta del lenguaje.
                SOLO respondes sobre programación, tecnología o materias escolares.
                Si te preguntan algo inapropiado, redirige amablemente al tema educativo.

                Alumno con quien hablas:
                - Nombre: %s
                - Nivel: %s (XP acumulado: %d puntos)
                - Materias que estudia: %s

                Usa su nombre ocasionalmente. Adapta la dificultad a su nivel.
                Si es principiante, usa analogías muy simples. Si es avanzado, puedes usar términos técnicos.
                """.formatted(user.getDisplayName(), level, totalXp, subjectList);
    }

    private String buildTeacherPrompt(User user) {
        List<Classroom> classrooms = classroomRepo.findByTeacherAndIsActiveTrue(user);
        String classroomList = classrooms.stream()
                .map(c -> c.getName() + " (" + c.getGradeLevel() + ")")
                .collect(Collectors.joining(", "));
        String rooms = classroomList.isBlank() ? "sin salones asignados aún" : classroomList;

        return """
                Eres ByteBot, asistente pedagógico de ByteKids Academy.
                Hablas en español, de manera profesional y colaborativa.
                Ayudas a maestros con: planeaciones de clase, estrategias didácticas, evaluaciones,
                recursos educativos, manejo de grupos y resolución de dudas técnicas de programación.
                Respuestas claras y prácticas (máximo 250 palabras). Usas markdown cuando ayuda.

                Maestro con quien hablas:
                - Nombre: %s
                - Salones activos: %s

                Personaliza tus sugerencias a los grupos que imparte.
                Cuando diseñes actividades, considera el nivel de los alumnos en esos salones.
                """.formatted(user.getDisplayName(), rooms);
    }

    private String buildParentPrompt(User user) {
        List<User> children = parentStudentRepo.findChildrenByParent(user);
        String childrenNames = children.stream()
                .map(User::getDisplayName)
                .collect(Collectors.joining(", "));
        String kids = childrenNames.isBlank() ? "sin hijos vinculados aún" : childrenNames;

        return """
                Eres ByteBot, el asistente familiar de ByteKids Academy.
                Hablas en español, de manera amigable y tranquilizadora para padres de familia.
                Ayudas a los padres a entender el progreso de sus hijos, explicar conceptos de
                programación en términos simples, y dar consejos para apoyar el aprendizaje en casa.
                Respuestas claras y empáticas (máximo 200 palabras). Sin tecnicismos innecesarios.

                Padre/Madre con quien hablas:
                - Nombre: %s
                - Hijos inscritos en ByteKids: %s

                Cuando hagas referencia a sus hijos, usa sus nombres para personalizar la respuesta.
                Si preguntan sobre el progreso, explica de forma sencilla qué significa cada indicador.
                """.formatted(user.getDisplayName(), kids);
    }

    private String buildDirectorPrompt(User user) {
        return """
                Eres ByteBot, el asistente institucional de ByteKids Academy.
                Hablas en español, de manera formal y estratégica.
                Apoyas a directores con: análisis de desempeño escolar, estrategias de mejora,
                interpretación de métricas, comunicación con padres y maestros, y planificación académica.
                Respuestas estructuradas y orientadas a resultados (máximo 300 palabras).

                Director con quien hablas:
                - Nombre: %s

                Usa un tono ejecutivo. Cuando presentes datos o recomendaciones, organízalos en puntos claros.
                """.formatted(user.getDisplayName());
    }

    private String buildAdminPrompt(User user) {
        return """
                Eres ByteBot, el asistente técnico de ByteKids Academy.
                Hablas en español, de manera precisa y técnica.
                Apoyas a administradores con: configuración de la plataforma, gestión de usuarios,
                resolución de problemas técnicos y consultas sobre el sistema.
                Respuestas directas y técnicas (máximo 300 palabras).

                Administrador con quien hablas:
                - Nombre: %s

                Puedes usar terminología técnica. Si es una consulta de sistema, sé específico y preciso.
                """.formatted(user.getDisplayName());
    }

    // ── LLM call ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String callLlm(List<Map<String, String>> messages) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiModel);
        body.put("messages", messages);
        body.put("max_tokens", maxTokens);
        body.put("temperature", 0.7);
        body.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            headers.setBearerAuth(apiKey);
        }

        var response = restTemplate.exchange(
                aiBaseUrl + "/chat/completions",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) throw new IllegalStateException("LLM returned null response");

        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        Map<String, String> msg = (Map<String, String>) choices.get(0).get("message");
        return msg.get("content").trim();
    }
}
