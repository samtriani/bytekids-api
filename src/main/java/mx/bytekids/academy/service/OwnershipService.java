package mx.bytekids.academy.service;

import lombok.extern.slf4j.Slf4j;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.UserRole;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controla quien puede crear o modificar cuentas privilegiadas (admin/director).
 *
 * Sin esto, cualquier coordinador podia crear mas coordinadores y —peor— cambiar
 * la contraseña de otro coordinador via PUT /users/{id}, es decir apropiarse de
 * su cuenta. La lista de dueños se configura con la variable de entorno
 * OWNER_USERNAMES (separada por comas), asi que se cambia sin recompilar.
 */
@Slf4j
@Service
public class OwnershipService {

    private static final Set<UserRole> PRIVILEGED = Set.of(UserRole.admin, UserRole.director);

    private final Set<String> owners;

    public OwnershipService(@Value("${app.security.owner-usernames:}") String configured) {
        this.owners = Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
    }

    @PostConstruct
    void logConfig() {
        if (owners.isEmpty()) {
            log.warn("⚠️ app.security.owner-usernames esta vacio: NADIE podra crear "
                   + "cuentas de coordinador o director. Configura OWNER_USERNAMES.");
        } else {
            log.info("✅ Dueños autorizados para crear cuentas privilegiadas: {}", owners);
        }
    }

    public boolean isPrivileged(UserRole role) {
        return role != null && PRIVILEGED.contains(role);
    }

    public boolean isOwner(String username) {
        return username != null && owners.contains(username.trim().toLowerCase());
    }

    public boolean isCurrentUserOwner() {
        try {
            return isOwner(SecurityUtils.currentUsername());
        } catch (RuntimeException e) {
            return false;   // sin sesion activa
        }
    }

    /** Exige ser dueño para dar de alta una cuenta con el rol indicado. */
    public void requireOwnerToCreate(UserRole targetRole) {
        if (isPrivileged(targetRole) && !isCurrentUserOwner()) {
            throw new BusinessException(
                    "Solo un usuario dueño puede dar de alta cuentas de coordinador o director.");
        }
    }

    /**
     * Exige ser dueño para tocar una cuenta privilegiada, ya sea porque la cuenta
     * actual lo es (evita robo de cuenta cambiandole la contraseña) o porque se le
     * quiere asignar un rol privilegiado (evita escalar un maestro a coordinador).
     */
    public void requireOwnerToModify(User existing, UserRole targetRole) {
        boolean touchesPrivileged = isPrivileged(existing.getRole()) || isPrivileged(targetRole);
        if (touchesPrivileged && !isCurrentUserOwner()) {
            throw new BusinessException(
                    "Solo un usuario dueño puede modificar cuentas de coordinador o director.");
        }
    }
}
