package com.orquidea.api.security;

import com.orquidea.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error" 
    };

    /**
     * Los 401/403 de Spring Security se delegan al GlobalExceptionHandler
     * para que tengan el mismo formato que el resto de errores.
     */
    @Bean
    public SecurityFilterChain cadenaFiltrosSeguridad(
            HttpSecurity http,
            JwtService jwtService,
            UserRepository userRepository,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolutorExcepciones) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/autenticacion/iniciar-sesion").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/fichas-taxonomicas", "/api/fichas-taxonomicas/**").permitAll()
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/autenticacion/registro").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/anuncios").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/autenticacion/recuperar-contrasena",
                                "/api/autenticacion/restablecer-contrasena").permitAll()
                        .anyRequest().authenticated()
                )
                        
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint((request, response, ex) ->
                                resolutorExcepciones.resolveException(request, response, null, ex))
                        .accessDeniedHandler((request, response, ex) ->
                                resolutorExcepciones.resolveException(request, response, null, ex)))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userRepository), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * HU-6: el superadministrador hereda todos los permisos de administrador,
     * así basta con @PreAuthorize("hasRole('ADMINISTRADOR')").
     */
    @Bean
    public static RoleHierarchy jerarquiaRoles() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_SUPERADMINISTRADOR > ROLE_ADMINISTRADOR");
    }

    @Bean
    public PasswordEncoder codificadorContrasenas() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOriginPatterns(corsProperties.origenesPermitidos());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}
