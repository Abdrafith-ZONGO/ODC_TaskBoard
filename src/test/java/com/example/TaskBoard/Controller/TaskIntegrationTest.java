package com.example.TaskBoard.Controller;

import java.time.LocalDateTime;

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

import com.example.DTO.TaskRequestDTO;
import com.example.Model.BoardModel;
import com.example.Model.TaskModel;
import com.example.Model.TaskStatus;
import com.example.Model.UserModel;
import com.example.Repository.BoardRepository;
import com.example.Repository.TaskRepository;
import com.example.Repository.UserRepository;
import com.example.Security.JwtService;
import com.example.Security.UserDetailService;
import com.example.TaskBoardApplication;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * =============================================================================
 * TESTS D'INTÉGRATION : TaskIntegrationTest
 * =============================================================================
 *
 * OBJECTIF PÉDAGOGIQUE :
 * - Tester l'API REST des tâches sous le chemin imbriqué : `/api/boards/{boardId}/tasks`.
 * - Valider le bon fonctionnement de `@PathVariable("boardId")` et `@PathVariable("taskId")`.
 * - Vérifier les règles métier fondamentales :
 *     * Une tâche créée sans statut prend par défaut `TaskStatus.A_FAIRE`.
 *     * Les transitions de statut (A_FAIRE -> EN_COURS -> TERMINE) sont persistées correctement.
 *     * L'intégrité relationnelle Many-to-One : une tâche appartient à un tableau.
 * - Vérifier que toutes les requêtes sont sécurisées par le token JWT Bearer.
 */
@SpringBootTest(classes = TaskBoardApplication.class)
@AutoConfigureMockMvc
class TaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailService userDetailService;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;
    private BoardModel board;
    private TaskModel task;

    @BeforeEach
    void setUp() {
        // Nettoyage dans l'ordre pour respecter l'intégrité référentielle
        taskRepository.deleteAll();
        boardRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Création de l'utilisateur propriétaire
        UserModel user = new UserModel();
        user.setName("Mariam");
        user.setEmail("mariam@example.com");
        user.setPassword(passwordEncoder.encode("secret123"));
        user = userRepository.save(user);

        // 2. Création du tableau parent
        board = new BoardModel();
        board.setTitle("Tableau Sprint 1");
        board.setOwner(user);
        board.setCreatedAt(LocalDateTime.now());
        board = boardRepository.save(board);

        // 3. Création d'une tâche existante
        task = new TaskModel();
        task.setTitle("Implémenter les tests");
        task.setDescription("Écrire les tests unitaires et intégration");
        task.setStatus(TaskStatus.A_FAIRE);
        task.setBoard(board);
        task = taskRepository.save(task);

        // 4. Token JWT pour l'utilisateur connecté
        UserDetails userDetails = userDetailService.loadUserByUsername("mariam@example.com");
        authToken = "Bearer " + jwtService.generateToken(userDetails);
    }

    // =========================================================================
    // TESTS : CREATE TASK
    // =========================================================================

    @Test
    @DisplayName("POST /api/boards/{boardId}/tasks - Création réussie avec statut par défaut A_FAIRE")
    void createTask_DefaultStatus_Success() throws Exception {
        // ARRANGE : Requête sans statut spécifié (null)
        TaskRequestDTO newRequest = new TaskRequestDTO("Configurer Docker", "Créer docker-compose.yml", null);

        // ACT & ASSERT : Création via l'URL imbriquée /api/boards/{boardId}/tasks
        mockMvc.perform(post("/api/boards/" + board.getId() + "/tasks")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Configurer Docker"))
                .andExpect(jsonPath("$.status").value("A_FAIRE")) // Règle métier validée
                .andExpect(jsonPath("$.boardId").value(board.getId()));
    }

    @Test
    @DisplayName("POST /api/boards/{boardId}/tasks - Échec de validation 400 si le titre est vide")
    void createTask_BlankTitle_Returns400() throws Exception {
        TaskRequestDTO invalidRequest = new TaskRequestDTO("", "Description sans titre", null);

        mockMvc.perform(post("/api/boards/" + board.getId() + "/tasks")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title").exists());
    }

    // =========================================================================
    // TESTS : READ TASKS
    // =========================================================================

    @Test
    @DisplayName("GET /api/boards/{boardId}/tasks - Récupération de la liste des tâches d'un tableau")
    void getTasksByBoard_Success() throws Exception {
        mockMvc.perform(get("/api/boards/" + board.getId() + "/tasks")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Implémenter les tests"))
                .andExpect(jsonPath("$[0].status").value("A_FAIRE"));
    }

    @Test
    @DisplayName("GET /api/boards/{boardId}/tasks/{taskId} - Récupération d'une tâche par son identifiant")
    void getTaskById_Success() throws Exception {
        mockMvc.perform(get("/api/boards/" + board.getId() + "/tasks/" + task.getId())
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId()))
                .andExpect(jsonPath("$.title").value("Implémenter les tests"));
    }

    // =========================================================================
    // TESTS : UPDATE & DELETE
    // =========================================================================

    @Test
    @DisplayName("PUT /api/boards/{boardId}/tasks/{taskId} - Mise à jour et transition de statut (A_FAIRE -> EN_COURS)")
    void updateTask_StatusChange_Success() throws Exception {
        TaskRequestDTO updateRequest = new TaskRequestDTO("Implémenter les tests", "En cours de rédaction", TaskStatus.EN_COURS);

        mockMvc.perform(put("/api/boards/" + board.getId() + "/tasks/" + task.getId())
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_COURS"))
                .andExpect(jsonPath("$.description").value("En cours de rédaction"));
    }

    @Test
    @DisplayName("DELETE /api/boards/{boardId}/tasks/{taskId} - Suppression d'une tâche (204 No Content)")
    void deleteTask_Success() throws Exception {
        mockMvc.perform(delete("/api/boards/" + board.getId() + "/tasks/" + task.getId())
                .header("Authorization", authToken))
                .andExpect(status().isNoContent());

        assertFalse(taskRepository.existsById(task.getId()), "La tâche ne doit plus exister en base");
    }
}
