package com.example.TaskBoard.Security;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.Security.JwtService;

/**
 * =============================================================================
 * TESTS UNITAIRES : JwtServiceTest
 * =============================================================================
 *
 * OBJECTIF PÉDAGOGIQUE :
 * - Démontrer le fonctionnement d'un JSON Web Token (JWT) :
 *     1. Création et signature avec un algorithme HMAC-SHA256 (SecretKey).
 *     2. Extraction du 'Subject' (qui contient l'email de l'utilisateur).
 *     3. Extraction de revendications personnalisées ('Claims').
 *     4. Vérification de la validité temporelle et de la cohérence de l'utilisateur.
 * - Utilisation de `ReflectionTestUtils.setField(...)` :
 *     Permet d'injecter des valeurs annotées `@Value("${jwt.secret}")` dans une classe
 *     sans avoir besoin de démarrer le contexte Spring complet.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    // Clé secrète de test encodée en Base64 (256 bits minimum requis pour HMAC-SHA256)
    private static final String SECRET_KEY = "krw/rif3mEgx0M+QLcU2sdtB9e14o7o2G7iMOr/R+CA=";
    // Durée de validité simulée pour les tests (1 heure en millisecondes)
    private static final long EXPIRATION_MS = 3600000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Injection manuelle des propriétés @Value dans l'instance
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION_MS);

        // Utilisateur Spring Security simulé
        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("generateToken & extractUsername - Le token généré doit contenir l'email dans son Subject")
    void generateToken_ExtractUsername_Success() {
        // 1. ACT : Génération du token JWT
        String token = jwtService.generateToken(userDetails);

        // 2. ASSERT : Le token ne doit pas être vide et doit être au format header.payload.signature
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length, "Un token JWT doit être composé de 3 parties séparées par des points");

        // Extraction du subject (nom d'utilisateur/email)
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals("test@example.com", extractedUsername, "L'email extrait du token doit correspondre à celui de l'utilisateur");
    }

    @Test
    @DisplayName("generateToken avec extraClaims - Présence de données personnalisées dans le payload")
    void generateToken_WithExtraClaims_Success() {
        // 1. ARRANGE : Définition de claims additionnels (ex: rôle applicatif, ID)
        Map<String, Object> extraClaims = Map.of("role", "ADMIN", "customId", 42);

        // 2. ACT
        String token = jwtService.generateToken(extraClaims, userDetails);

        // 3. ASSERT : Extraction et vérification des claims
        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        Integer customId = jwtService.extractClaim(token, claims -> claims.get("customId", Integer.class));

        assertEquals("ADMIN", role);
        assertEquals(42, customId);
    }

    @Test
    @DisplayName("isTokenValid - Retourne TRUE si le token correspond à l'utilisateur et n'est pas expiré")
    void isTokenValid_ValidUser_ReturnsTrue() {
        // 1. ARRANGE : Token fraîchement généré
        String token = jwtService.generateToken(userDetails);

        // 2. ACT
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // 3. ASSERT
        assertTrue(isValid, "Le token doit être valide pour le même utilisateur");
    }

    @Test
    @DisplayName("isTokenValid - Retourne FALSE si le token est présenté pour un autre utilisateur")
    void isTokenValid_DifferentUser_ReturnsFalse() {
        // 1. ARRANGE : Token émis pour "test@example.com"
        String token = jwtService.generateToken(userDetails);

        // Utilisateur différent : "other@example.com"
        UserDetails differentUser = User.builder()
                .username("other@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        // 2. ACT
        boolean isValid = jwtService.isTokenValid(token, differentUser);

        // 3. ASSERT
        assertFalse(isValid, "Un token ne doit pas être valide pour un utilisateur dont l'email ne correspond pas");
    }
}

