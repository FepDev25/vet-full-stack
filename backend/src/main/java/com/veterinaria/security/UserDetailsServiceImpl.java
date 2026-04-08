package com.veterinaria.security;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinaria.domain.entity.UserCredentials;
import com.veterinaria.domain.repository.ClientRepository;
import com.veterinaria.domain.repository.StaffRepository;
import com.veterinaria.domain.repository.UserCredentialsRepository;

@Service
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService {

    private final StaffRepository           staffRepo;
    private final ClientRepository          clientRepo;
    private final UserCredentialsRepository credRepo;

    public UserDetailsServiceImpl(StaffRepository staffRepo,
                                  ClientRepository clientRepo,
                                  UserCredentialsRepository credRepo) {
        this.staffRepo  = staffRepo;
        this.clientRepo = clientRepo;
        this.credRepo   = credRepo;
    }

    // Busca primero en staff, luego en clients (deleted_at IS NULL)
    // Carga las credenciales asociadas y construye el UserDetails con el role JWT
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Buscar en staff
        Optional<com.veterinaria.domain.entity.Staff> staffOpt = staffRepo.findByEmail(email);
        if (staffOpt.isPresent()) {
            var s = staffOpt.get();
            UserCredentials creds = credRepo
                    .findByEntityIdAndEntityType(s.getId(), "STAFF")
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Sin credenciales para staff: " + email));
            String role = toJwtRole(s.getRole().name());
            return buildUserDetails(email, creds.getPasswordHash(), role);
        }

        // 2. Buscar en clients
        Optional<com.veterinaria.domain.entity.Client> clientOpt =
                clientRepo.findByEmailAndDeletedAtIsNull(email);
        if (clientOpt.isPresent()) {
            var c = clientOpt.get();
            UserCredentials creds = credRepo
                    .findByEntityIdAndEntityType(c.getId(), "CLIENT")
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Sin credenciales para cliente: " + email));
            return buildUserDetails(email, creds.getPasswordHash(), "CLIENT");
        }

        throw new UsernameNotFoundException("Usuario no encontrado: " + email);
    }

    private UserDetails buildUserDetails(String email, String passwordHash, String role) {
        return new User(email, passwordHash,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    // Mapea StaffRole → JwtRole. ASSISTANT → RECEPTIONIST (sin rol ASSISTANT en JWT)
    private String toJwtRole(String staffRole) {
        return switch (staffRole) {
            case "VETERINARIAN" -> "VETERINARIAN";
            case "RECEPTIONIST" -> "RECEPTIONIST";
            case "ASSISTANT"    -> "RECEPTIONIST";
            default             -> "RECEPTIONIST";
        };
    }
}
