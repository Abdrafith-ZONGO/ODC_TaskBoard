package com.example.TaskBoard.Controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.DTO.UserRequestDTO;
import com.example.Model.UserModel;
import com.example.Repository.UserRepository;
import com.example.Security.JwtService;
import com.example.Security.UserDetailService;
import com.example.TaskBoardApplication;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * =============================================================================
 * TESTS D'INTÉGRATION : UserIntegrationTest
 * =============================================================================
 *
 * OBJECTIF PÉDAGOGIQUE :
 * - Tester l'ensemble du cycle de vie des utilisateurs via l'API REST `/api/users`.
 * - Valider les contrôles de validation Jakarta (`@Valid`, `@NotBlank`, `@Email`, `@Size`).
 * - Valider le gestionnaire global d'exceptions (`GlobalExceptionHandler`) :
 *     * 400 Bad Request pour les erreurs de validation.
 *     * 409 Conflict en cas de tentative de création d'un email en double.
 *     * 404 Not Found quand un utilisateur recherché n'existe pas.
 * - Valider que toutes les opérations protégées s'exécutent avec succès en fournissant
 *   le token Bearer dans l'en-tête HTTP `Authorization`.
 */
@SpringBootTest(classes = TaskBoardApplication.class)
@AutoConfigureMockMvc
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailService userDetailService;

    @Autowired
    private ObjectMapper objectMapper;

    // Token d'authentification réutilisé dans les requêtes protégées
    private String authToken;
    private UserModel existingUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Création d'un utilisateur de test dans la base H2
        existingUser = new UserModel();
        existingUser.setName("Fatou");
        existingUser.setEmail("fatou@example.com");
        existingUser.setPassword(passwordEncoder.encode("password123"));
        existingUser = userRepository.save(existingUser);

        // Génération d'un token JWT valide pour cet utilisateur
        UserDetails userDetails = userDetailService.loadUserByUsername("fatou@example.com");
        authToken = "Bearer " + jwtService.generateToken(userDetails);
    }

    // =========================================================================
    // TESTS : VALIDATION DES ENTRÉES (JAKARTA BEAN VALIDATION)
    // =========================================================================

    @Test
    @DisplayName("POST /api/users - Échec de validation 400 si nom vide, email malformé ou mot de passe trop court")
    void createUser_ValidationErrors() throws Exception {
        // ARRANGE : Données volontairement invalides (nom vide, format email incorrect, mot de passe < 6 caractères)
        UserRequestDTO invalidRequest = new UserRequestDTO("", "not-an-email", "123");

        // ACT & ASSERT : Le GlobalExceptionHandler doit intercepter MethodArgumentNotValidException
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @DisplayName("POST /api/users - Conflit 409 si l'email existe déjà en base")
    void createUser_DuplicateEmail_Returns409() throws Exception {
        // ARRANGE : Tentative de création avec l'email déjà existant ("fatou@example.com")
        UserRequestDTO duplicateRequest = new UserRequestDTO("Autre Nom", "fatou@example.com", "password123");

        // ACT & ASSERT : DuplicateResourceException -> HTTP 409 CONFLICT
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // =========================================================================
    // TESTS : READ USERS
    // =========================================================================

    @Test
    @DisplayName("GET /api/users - Récupération de la liste avec un token valide (200 OK)")
    void getAllUsers_WithToken_Returns200() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].email").value("fatou@example.com"));
    }

    @Test
    @DisplayName("GET /api/users/{id} - Succès si l'utilisateur existe")
    void getUserById_Success() throws Exception {
        mockMvc.perform(get("/api/users/" + existingUser.getId())
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingUser.getId()))
                .andExpect(jsonPath("$.name").value("Fatou"));
    }

    @Test
    @DisplayName("GET /api/users/{id} - 404 Not Found si l'utilisateur n'existe pas")
    void getUserById_NotFound_Returns404() throws Exception {
        // ID 99999 inexistant
        mockMvc.perform(get("/api/users/99999")
                .header("Authorization", authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // =========================================================================
    // TESTS : UPDATE & DELETE
    // =========================================================================

    @Test
    @DisplayName("PUT /api/users/{id} - Succès de mise à jour du profil")
    void updateUser_Success() throws Exception {
        UserRequestDTO updateDTO = new UserRequestDTO("Fatou Ndiaye", "fatou.n@example.com", "newsecret");

        mockMvc.perform(put("/api/users/" + existingUser.getId())
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fatou Ndiaye"))
                .andExpect(jsonPath("$.email").value("fatou.n@example.com"));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - Suppression réussie (204 No Content)")
    void deleteUser_Success() throws Exception {
        mockMvc.perform(delete("/api/users/" + existingUser.getId())
                .header("Authorization", authToken))
                .andExpect(status().isNoContent());

        // Vérification en base : l'utilisateur doit avoir disparu
        assertFalse(userRepository.existsById(existingUser.getId()), "L'utilisateur ne doit plus exister en base");
    }
}