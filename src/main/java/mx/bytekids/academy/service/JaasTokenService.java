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
    @Value("${app.jaas.private-key-path}") private String keyPath;

    private RSAPrivateKey privateKey;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource(keyPath);
            String pem = new String(resource.getInputStream().readAllBytes())
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privateKey = (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(decoded));
            log.info("✅ JaaS private key loaded successfully");
        } catch (Exception e) {
            log.error("❌ Could not load JaaS private key: {}", e.getMessage());
        }
    }

    public String generateToken(User user, String roomName) {
        if (privateKey == null) throw new IllegalStateException("JaaS private key not loaded");

        boolean isModerator = user.getRole() == UserRole.teacher
                || user.getRole() == UserRole.director
                || user.getRole() == UserRole.admin;

        Map<String, Object> userContext = Map.of(
                "moderator", String.valueOf(isModerator),
                "name",      user.getDisplayName(),
                "id",        user.getId().toString(),
                "avatar",    "",
                "email",     user.getUsername() + "@bytekids.mx"
        );

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
