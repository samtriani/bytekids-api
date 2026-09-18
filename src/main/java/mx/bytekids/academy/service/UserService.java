package mx.bytekids.academy.service;

import lombok.RequiredArgsConstructor;
import mx.bytekids.academy.dto.user.CambioContrasenaRequest;
import mx.bytekids.academy.dto.user.UserRequest;
import mx.bytekids.academy.dto.user.UserResponse;
import mx.bytekids.academy.entity.User;
import mx.bytekids.academy.entity.enums.UserRole;
import mx.bytekids.academy.exception.BusinessException;
import mx.bytekids.academy.exception.ResourceNotFoundException;
import mx.bytekids.academy.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository   userRepository;
    private final PasswordEncoder  passwordEncoder;
    private final OwnershipService ownershipService;

    public User findByUsername(String username) {
        return userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", username));
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    public List<UserResponse> findByRole(UserRole role) {
        return userRepository.findByRoleAndIsActiveTrue(role).stream().map(UserResponse::from).toList();
    }

    /**
     * Normaliza y valida el correo. Se guarda en minusculas porque
     * "Ana@x.com" y "ana@x.com" son el mismo buzon: sin esto, dos cuentas
     * distintas podrian reclamar el mismo correo y el dia que se mande un
     * "olvide mi contrasena" no se sabria a cual pertenece.
     *
     * @param actual el correo que ya tiene el usuario, para que editarlo
     *               sin cambiarlo no choque consigo mismo. Null al crear.
     */
    private String normalizarEmail(String crudo, String actual) {
        if (crudo == null || crudo.isBlank()) return null;
        String email = crudo.trim().toLowerCase();
        if (!email.equalsIgnoreCase(actual) && userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("El correo '" + email + "' ya esta registrado");
        }
        return email;
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        ownershipService.requireOwnerToCreate(request.getRole());
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("La contraseña es requerida para crear un usuario");
        }
        String normalizedUsername = request.getUsername().trim().toLowerCase();
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new BusinessException("El usuario '" + normalizedUsername + "' ya existe");
        }
        User user = User.builder()
                .username(normalizedUsername)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .email(normalizarEmail(request.getEmail(), null))
                .role(request.getRole())
                .initials(request.getInitials())
                .avatarUrl(request.getAvatarUrl())
                .age(request.getAge())
                .address(request.getAddress())
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(UUID id, UserRequest request) {
        User user = findById(id);
        ownershipService.requireOwnerToModify(user, request.getRole());
        String normalizedUsername = request.getUsername().trim().toLowerCase();
        if (!user.getUsername().equals(normalizedUsername) && userRepository.existsByUsername(normalizedUsername)) {
            throw new BusinessException("El usuario '" + normalizedUsername + "' ya existe");
        }
        user.setUsername(normalizedUsername);
        user.setDisplayName(request.getDisplayName());
        user.setEmail(normalizarEmail(request.getEmail(), user.getEmail()));
        user.setRole(request.getRole());
        user.setInitials(request.getInitials());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setAge(request.getAge());
        user.setAddress(request.getAddress());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deactivate(UUID id) {
        User user = findById(id);
        ownershipService.requireOwnerToModify(user, user.getRole());
        user.setIsActive(false);
        userRepository.save(user);
    }

    /**
     * Cambiar la contrasena PROPIA.
     *
     * Es el unico camino por el que un usuario que no es coordinador puede
     * tocar su contrasena: update() esta detras de hasRole('ADMIN'), asi que
     * hasta hoy un maestro, un papa o un nino dependian de que alguien de
     * coordinacion se la cambiara a mano.
     *
     * Se resuelve por el username del token y NO por un id recibido, para que
     * no exista forma de apuntar a otra cuenta.
     */
    @Transactional
    public void cambiarMiContrasena(String username, CambioContrasenaRequest peticion) {
        User user = findByUsername(username);

        if (!passwordEncoder.matches(peticion.getActual(), user.getPasswordHash())) {
            throw new BusinessException("Tu contrasena actual no es correcta");
        }
        if (passwordEncoder.matches(peticion.getNueva(), user.getPasswordHash())) {
            throw new BusinessException("La nueva contrasena tiene que ser distinta de la actual");
        }

        user.setPasswordHash(passwordEncoder.encode(peticion.getNueva()));
        userRepository.save(user);
    }

    /**
     * Los avatares que existen. La lista vive AQUI, en el servidor.
     *
     * Que la pantalla solo ofrezca doce no es una regla: avatar_url se pinta
     * en las pantallas de otros usuarios --el muro del salon, la lista de
     * contactos-- asi que sin validar, cualquiera con una sesion podria
     * meter ahi lo que quisiera y hacerselo llegar a un menor.
     *
     * Tiene que coincidir con ROBOTICITOS en el front
     * (bytekids-ui/src/app/shared/roboticitos.ts).
     */
    private static final Set<String> AVATARES = Set.of(
            "bot-chip",
            "bot-nova",
            "bot-pixel",
            "bot-volt",
            "bot-domo",
            "bot-hex",
            "bot-bit",
            "bot-radar",
            "bot-luna",
            "bot-mecha",
            "bot-tuerca",
            "bot-byte");

    /**
     * Escoger el roboticito propio. Sin rol: lo usan los cinco.
     *
     * Se resuelve por el username del token y no por un id recibido, igual
     * que el cambio de contrasena: no debe existir forma de cambiarle el
     * avatar a otra persona.
     *
     * Un avatar vacio o nulo lo quita, y la pantalla vuelve a las iniciales.
     */
    @Transactional
    public UserResponse cambiarMiAvatar(String username, String avatar) {
        User user = findByUsername(username);
        String elegido = (avatar == null || avatar.isBlank()) ? null : avatar.trim();

        if (elegido != null && !AVATARES.contains(elegido)) {
            throw new BusinessException("Ese avatar no existe");
        }

        user.setAvatarUrl(elegido);
        return UserResponse.from(userRepository.save(user));
    }
}
