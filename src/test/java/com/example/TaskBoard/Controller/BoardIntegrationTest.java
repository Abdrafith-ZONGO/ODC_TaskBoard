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

import com.example.DTO.BoardRequestDTO;
import com.example.Model.BoardModel;
import com.example.Model.UserModel;
import com.example.Repository.BoardRepository;
import com.example.Repository.UserRepository;
import com.example.Security.JwtService;
import com.example.Security.UserDetailService;
import com.example.TaskBoardApplication;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * =============================================================================
 * TESTS D'INTÉGRATION : BoardIntegrationTest
 * =============================================================================
 *
 * OBJECTIF :
 * - Valider les opérations CRUD sur les tableaux de bord (Boards) via `/api/boards`.
 * - Valider le passage de paramètres d'URL (Query Parameters) : `?ownerId=...`
 *   et de variables de chemin (Path Variables) : `/{id}`.
 * - S'assurer que le header `Authorization: Bearer <token>` est bien requis
 *   pour chaque opération sur les tableaux.
 * - Valider la contrainte relationnelle JPA : un tableau doit obligatoirement
 *   être rattaché à un utilisateur existant (`owner`).
 */
@SpringBootTest(classes = TaskBoardApplication.class)
@AutoConfigureMockMvc
class BoardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailService userDetailService;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;
    private UserModel owner;
    private BoardModel board;

    @BeforeEach
    void setUp() {
        // Nettoyage en respectant l'ordre des contraintes de clés étrangères
        boardRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Création d'un propriétaire en base
        owner = new UserModel();
        owner.setName("Ibrahima");
        owner.setEmail("ibrahima@example.com");
        owner.setPassword(passwordEncoder.encode("secret123"));
        owner = userRepository.save(owner);

        // 2. Création d'un premier tableau lié à ce propriétaire
        board = new BoardModel();
        board.setTitle("Projet Mobile");
        board.setOwner(owner);
        board.setCreatedAt(LocalDateTime.now());
        board = boardRepository.save(board);

        // 3. Génération du token JWT pour authentifier les requêtes de test
        UserDetails userDetails = userDetailService.loadUserByUsername("ibrahima@example.com");
        authToken = "Bearer " + jwtService.generateToken(userDetails);
    }

    // =========================================================================
    // TESTS : CREATE BOARD
    // =========================================================================

    @Test
    @DisplayName("POST /api/boards - Création d'un tableau avec paramètre ownerId (201 Created)")
    void createBoard_Success() throws Exception {
        // ARRANGE : Données du nouveau tableau
        BoardRequestDTO newBoard = new BoardRequestDTO("Projet Web Fullstack");

        // ACT & ASSERT : Requête avec paramètre de requête ?ownerId=...
        mockMvc.perform(post("/api/boards")
                .param("ownerId", owner.getId().toString())
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newBoard)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Projet Web Fullstack"))
                .andExpect(jsonPath("$.ownerId").value(owner.getId()));
    }

    @Test
    @DisplayName("POST /api/boards - Échec de validation 400 si le titre est vide")
    void createBoard_InvalidTitle_Returns400() throws Exception {
        // Titre vide non conforme à @NotBlank
        BoardRequestDTO invalidBoard = new BoardRequestDTO("");

        mockMvc.perform(post("/api/boards")
                .param("ownerId", owner.getId().toString())
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidBoard)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title").exists());
    }

    // =========================================================================
    // TESTS : READ BOARDS
    // =========================================================================

    @Test
    @DisplayName("GET /api/boards - Récupération de tous les tableaux")
    void getAllBoards_Success() throws Exception {
        mockMvc.perform(get("/api/boards")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Projet Mobile"));
    }

    @Test
    @DisplayName("GET /api/boards?ownerId={id} - Filtrage des tableaux par ID du propriétaire")
    void getBoardsByOwner_Success() throws Exception {
        mockMvc.perform(get("/api/boards")
                .param("ownerId", owner.getId().toString())
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].ownerId").value(owner.getId()));
    }

    @Test
    @DisplayName("GET /api/boards/{id} - Récupération d'un tableau spécifique par son identifiant")
    void getBoardById_Success() throws Exception {
        mockMvc.perform(get("/api/boards/" + board.getId())
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(board.getId()))
                .andExpect(jsonPath("$.title").value("Projet Mobile"));
    }

    // =========================================================================
    // TESTS : UPDATE & DELETE
    // =========================================================================

    @Test
    @DisplayName("PUT /api/boards/{id} - Modification réussie du titre d'un tableau")
    void updateBoard_Success() throws Exception {
        BoardRequestDTO updateRequest = new BoardRequestDTO("Projet Mobile V2");

        mockMvc.perform(put("/api/boards/" + board.getId())
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Projet Mobile V2"));
    }

    @Test
    @DisplayName("DELETE /api/boards/{id} - Suppression réussie (204 No Content)")
    void deleteBoard_Success() throws Exception {
        mockMvc.perform(delete("/api/boards/" + board.getId())
                .header("Authorization", authToken))
                .andExpect(status().isNoContent());

        assertFalse(boardRepository.existsById(board.getId()), "Le tableau ne doit plus exister en base");
    }
}
