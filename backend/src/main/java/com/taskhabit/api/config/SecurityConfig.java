package com.taskhabit.api.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad del API: toda petición debe traer un token de Firebase válido.
 *
 * Flujo:
 *  1. La app inicia sesión en Firebase y obtiene un "ID token" (un JWT firmado por Google).
 *  2. AuthInterceptor (en Android) lo manda en cada petición: "Authorization: Bearer <token>".
 *  3. Este backend verifica la FIRMA con las llaves públicas de Google y revisa que el token
 *     sea de nuestro proyecto y no esté vencido. Si algo falla -> 401 Unauthorized.
 *  4. Si es válido, el controlador recibe el objeto Jwt; jwt.getSubject() es el uid del usuario.
 *
 * Ventaja: no necesitamos guardar contraseñas ni la llave privada de Firebase en el servidor.
 */
@Configuration
public class SecurityConfig {

    /** URL pública donde Google publica las llaves para verificar los tokens de Firebase. */
    private static final String FIREBASE_JWKS =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

    /** Define qué rutas se protegen y cómo. */
    @Bean
    SecurityFilterChain cadenaDeFiltros(HttpSecurity http) throws Exception {
        http
                // CSRF protege formularios web con cookies; un API con tokens Bearer no lo necesita.
                .csrf(csrf -> csrf.disable())
                // STATELESS: el servidor no guarda sesión; cada petición se autentica con su token.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Todas las rutas exigen estar autenticado.
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                // Activa la validación de tokens JWT usando el JwtDecoder definido abajo.
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * Decodificador de JWT configurado para Firebase.
     * @Value lee el project-id desde application.properties.
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${taskhabit.firebase.project-id}") String projectId) {
        // Descarga (y cachea) las llaves públicas de Google para comprobar la firma RS256.
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(FIREBASE_JWKS).build();

        // Validación 1 ("iss"): el emisor debe ser exactamente nuestro proyecto de Firebase.
        // createDefaultWithIssuer también revisa las fechas de expiración (exp) y emisión.
        OAuth2TokenValidator<Jwt> emisor =
                JwtValidators.createDefaultWithIssuer("https://securetoken.google.com/" + projectId);

        // Validación 2 ("aud"): la audiencia del token debe ser nuestro project-id.
        // Evita aceptar tokens válidos de OTRO proyecto de Firebase.
        OAuth2TokenValidator<Jwt> audiencia = new JwtClaimValidator<List<String>>(
                "aud", aud -> aud != null && aud.contains(projectId));

        // Ambas validaciones deben pasar.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(emisor, audiencia));
        return decoder;
    }
}
