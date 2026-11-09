package com.example.TaskBoard.Controller;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.DTO.LoginRequestDTO;
import com.example.DTO.UserRequestDTO;
import com.example.Model.UserModel;
import com.example.Repository.UserRepository;
import com.example.TaskBoardApplication;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * =============================================================================
 * TESTS D'INTÉGRATION : AuthenticationIntegrationTest
 * =============================================================================
 *
 * OBJECTIF :
 * - Valider le flux complet d'authentification Stateless par JWT.
 * - `@SpringBootTest(classes = TaskBoardApplication.class)` :
 *     Démarre le serveur Spring complet avec sa configuration de sécurité,
 *     ses filtres (`JwtAuthenticationFilter`), ses repositories JPA et sa base H2.
 * - `@AutoConfigureMockMvc` :
 *     Injecte `MockMvc` qui simule l'envoi de requêtes HTTP réelles (POST, GET)
 *     sans avoir besoin d'ouvrir un socket réseau ou un port HTTP réel.
 * - Scénarios couverts :
 *     1. Inscription publique d'un compte (POST /api/users) sans être connecté.
 *     2. Connexion avec bons identifiants (POST /login) -> renvoie 200 + token JWT.
 *     3. Connexion avec mauvais identifiants -> renvoie 401 Unauthorized.
 *     4. Tentative d'accès à une ressource protégée sans token -> rejet 401.
 *     5. Accès à une ressource protégée avec le header "Authorization: Bearer <token>" -> succès 200.
 */
@SpringBootTest(classes = TaskBoardApplication.class)
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Nettoyage de la base de données en mémoire avant chaque test
     * pour garantir l'indépendance totale des scénarios.
     */
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Inscription publique d'un utilisateur via POST /api/users (sans token requis)")
    void registerUser_ShouldSucceed() throws Exception {
        // ARRANGE : Données du nouvel utilisateur
        UserRequestDTO requestDTO = new UserRequestDTO("Amadou Diallo", "amadou@example.com", "password123");

        // ACT & ASSERT : Requête POST publique autorisée par SecurityConfig (.permitAll())
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated()) // HTTP 201 Created attendu
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("amadou@example.com"))
                .andExpect(jsonPath("$.name").value("Amadou Diallo"));
    }

    @Test
    @DisplayName("2. Connexion avec identifiants valides via POST /login -> retourne le token JWT")
    void login_WithValidCredentials_ShouldReturnJwtToken() throws Exception {
        // ARRANGE : Création d'un utilisateur existant avec mot de passe haché par BCrypt
        UserModel user = new UserModel();
        user.setName("Amadou Diallo");
        user.setEmail("amadou@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(user);

        // Identifiants envoyés en clair lors du login
        LoginRequestDTO loginRequest = new LoginRequestDTO("amadou@example.com", "password123");

        // ACT & ASSERT
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk()) // HTTP 200 OK
                .andExpect(jsonPath("$.token").isNotEmpty()); // Le token JWT signé doit être présent
    }

    @Test
    @DisplayName("3. Connexion avec mot de passe incorrect via POST /login -> 401 Unauthorized")
    void login_WithInvalidCredentials_ShouldReturn401() throws Exception {
        // ARRANGE : Utilisateur existant
        UserModel user = new UserModel();
        user.setName("Amadou Diallo");
        user.setEmail("amadou@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(user);

        // Mot de passe intentionnellement faux
        LoginRequestDTO loginRequest = new LoginRequestDTO("amadou@example.com", "wrongPassword");

        // ACT & ASSERT : Interception par GlobalExceptionHandler (BadCredentialsException -> 401)
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()) // HTTP 401 Unauthorized
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect"));
    }

    @Test
    @DisplayName("4. Accès à une route protégée sans token -> 401 Unauthorized")
    void accessProtectedResource_WithoutToken_ShouldReturn401() throws Exception {
        // ACT & ASSERT : Appel direct de GET /api/users sans en-tête Authorization
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized()); // Bloqué par SecurityConfig (.anyRequest().authenticated())
    }

    @Test
    @DisplayName("5. Accès à une route protégée avec token JWT valide (Bearer) -> 200 OK")
    void accessProtectedResource_WithValidToken_ShouldSucceed() throws Exception {
        // ARRANGE : 1. Création du compte utilisateur
        UserModel user = new UserModel();
        user.setName("Amadou Diallo");
        user.setEmail("amadou@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(user);

        // 2. Connexion pour récupérer le JWT
        LoginRequestDTO loginRequest = new LoginRequestDTO("amadou@example.com", "password123");
        MvcResult result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseString).get("token").asText();

        // ACT & ASSERT : 3. Appel de la route protégée avec le header "Authorization: Bearer <token>"
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()) // Autorisé par JwtAuthenticationFilter
                .andExpect(jsonPath("$").isArray());
    }
}
