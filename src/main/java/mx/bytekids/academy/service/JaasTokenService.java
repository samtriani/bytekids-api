package mx.bytekids.academy.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JaasTokenService {

    @Value("${app.jaas.app-id}")   private String appId;
    @Value("${app.jaas.key-id}")   private String keyId;
    @Value("${app.jaas.private-key-path:}") private String keyPath;
    /** PEM completo en base64. En produccion viene del secret JAAS_PRIVATE_KEY. */
    @Value("${app.jaas.private-key:}")      private String privateKeyB64;

    private RSAPrivateKey privateKey;

    /**
     * Carga la llave privada de JaaS.
     *
     * Prioridad: la variable de entorno JAAS_PRIVATE_KEY (el PEM completo en
     * base64). El archivo en el classpath es solo respaldo para desarrollo
     * local: estuvo versionado en un repo publico, asi que en produccion la
     * llave debe venir siempre de un secret, nunca del JAR.
     */
    @PostConstruct
    public void init() {
        try {
            String pem = leerPem();
            if (pem == null) {
                log.error("❌ No hay llave de JaaS. Configura el secret JAAS_PRIVATE_KEY "
                        + "(el PEM completo codificado en base64). Las videollamadas no funcionaran.");
                return;
            }

            String cuerpo = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(cuerpo);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privateKey = (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(decoded));
            log.info("✅ Llave de JaaS cargada desde {}", origenLlave);
        } catch (Exception e) {
            log.error("❌ No se pudo cargar la llave de JaaS: {}", e.getMessage());
        }
    }

    private String origenLlave = "?";

    private String leerPem() throws Exception {
        if (privateKeyB64 != null && !privateKeyB64.isBlank()) {
            origenLlave = "la variable JAAS_PRIVATE_KEY";
            return new String(Base64.getDecoder().decode(privateKeyB64.trim()));
        }
        if (keyPath != null && !keyPath.isBlank()) {
            ClassPathResource resource = new ClassPathResource(keyPath);
            if (resource.exists()) {
                origenLlave = "el classpath (solo desarrollo local)";
                return new String(resource.getInputStream().readAllBytes());
            }
        }
        return null;
    }

    public String generateToken(User user, String roomName) {
        if (privateKey == null) throw new IllegalStateException("JaaS private key not loaded");

        boolean isModerator = user.getRole() == UserRole.teacher
                || user.getRole() == UserRole.director
                || user.getRole() == UserRole.admin;

        Map<String, Object> userContext = new java.util.HashMap<>();
        userContext.put("moderator", isModerator);          // boolean, NOT String
        userContext.put("name",      user.getDisplayName());
        userContext.put("id",        user.getId().toString());
        userContext.put("avatar",    "");
        userContext.put("email",     user.getUsername() + "@bytekids.mx");

        Map<String, Object> features = Map.of(
                "livestreaming",      "false",
                "outbound-call",      "false",
                "sip-outbound-call",  "false",
                "transcription",      "false",
                "recording",          "false"
        );

        Algorithm algorithm = Algorithm.RSA256(null, privateKey);
        long now = System.currentTimeMillis();

        return JWT.create()
                .withKeyId(appId + "/" + keyId)
                .withIssuer("chat")
                .withAudience("jitsi")
                .withSubject(appId)
                .withExpiresAt(new Date(now + 3_600_000))   // 1 hora
                .withNotBefore(new Date(now - 10_000))
                .withClaim("room",    roomName)
                .withClaim("context", Map.of("user", userContext, "features", features))
                .sign(algorithm);
    }
}
