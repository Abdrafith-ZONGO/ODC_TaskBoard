package com.example.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.Security.JwtAuthenticationFilter;
import com.example.Security.UserDetailService;

import lombok.RequiredArgsConstructor;

/**
 * Classe principale de configuration de Spring Security.
 * C'est ici que l'on définit les règles globales de sécurité, notamment l'intégration du JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Notre filtre personnalisé qui va vérifier les tokens JWT
    private final JwtAuthenticationFilter jwtAuthFilter;
    
    // Service pour récupérer les infos de l'utilisateur en base de données
    private final UserDetailService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Fournisseur d'authentification classique (DAO).
     * Il va utiliser le UserDetailsService pour vérifier les mots de passe.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Le gestionnaire d'authentification (utilisé par notre AuthController pour valider les logins).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Définition de la chaîne de filtres de sécurité.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // On désactive CSRF car JWT n'utilise pas de cookies de session vulnérables
                .csrf(csrf -> csrf.disable())
                
                // On indique que l'application est STATELESS (sans état).
                // Spring Security ne créera plus de session HTTP, chaque requête doit être authentifiée par le token.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                .authorizeHttpRequests(auth -> auth

                        // Swagger / OpenAPI autorisés sans token
                        .requestMatchers(
                                "/api-docs/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()

                        // H2 console autorisée sans token
                        .requestMatchers("/h2-console/**").permitAll()

                        // POUR LE COURS : On autorise cette route pour le test microservice avec Taskboard
                        .requestMatchers(HttpMethod.GET, "/api/users").permitAll()
                        // Pour tester la recuperation d'utilisateurs
                        .requestMatchers("/test-feigh/**").permitAll()
                        // Authentification et création de compte (pas de token requis ici, on vient pour s'authentifier)
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()
                        // .requestMatchers(HttpMethod.GET, "/employees").permitAll()
                        // TOUTES LES AUTRES REQUÊTES nécessitent un token valide
                        .anyRequest().authenticated())
                        
                .exceptionHandling(exc -> exc
                        // Si une requête non authentifiée arrive sur une route protégée, on renvoie une belle erreur 401
                        .authenticationEntryPoint(
                                (request, response, authException) -> response.sendError(401, "Unauthorized")))
                                
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                
                .authenticationProvider(authenticationProvider())
                
                // L'étape cruciale : On insère notre JwtAuthenticationFilter AVANT le filtre de base de Spring.
                // Ainsi, notre filtre interceptera le token et préparera le contexte de sécurité.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}