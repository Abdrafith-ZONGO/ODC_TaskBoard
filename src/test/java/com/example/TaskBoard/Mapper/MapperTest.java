package com.example.TaskBoard.Mapper;


import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.DTO.BoardRequestDTO;
import com.example.DTO.BoardResponseDTO;
import com.example.DTO.TaskRequestDTO;
import com.example.DTO.TaskResponseDTO;
import com.example.DTO.UserRequestDTO;
import com.example.DTO.UserResponseDTO;
import com.example.Mapper.BoardMapper;
import com.example.Mapper.TaskMapper;
import com.example.Mapper.UserMapper;
import com.example.Model.BoardModel;
import com.example.Model.TaskModel;
import com.example.Model.TaskStatus;
import com.example.Model.UserModel;

/**
 * =============================================================================
 * TESTS UNITAIRES : MapperTest (UserMapper, BoardMapper, TaskMapper)
 * =============================================================================
 *
 * OBJECTIF :
 * - Les Mappers jouent un rôle fondamental dans une architecture en couches :
 *     * Ils isolent le modèle de données (Entités JPA) des contrats API (DTOs).
 *     * Ils empêchent d'exposer des données sensibles vers l'extérieur (ex: le mot de passe hashé
 *       présent dans `UserModel` ne doit JAMAIS apparaître dans `UserResponseDTO`).
 *     * Ils évitent les problèmes de boucles infinies de sérialisation JSON causées par
 *       les relations bidirectionnelles JPA (`@OneToMany` / `@ManyToOne`).
 */
class MapperTest {

    private UserMapper userMapper;
    private BoardMapper boardMapper;
    private TaskMapper taskMapper;

    @BeforeEach
    void setUp() {
        // Instanciation directe sans Spring ni Mockito (classes utilitaires pures)
        userMapper = new UserMapper();
        boardMapper = new BoardMapper();
        taskMapper = new TaskMapper();
    }

    @Test
    @DisplayName("UserMapper - Conversion bidirectionnelle RequestDTO -> Entité et Entité -> ResponseDTO (sans mot de passe)")
    void userMapper_Conversions() {
        // 1. DTO -> Entité
        UserRequestDTO requestDTO = new UserRequestDTO("Khadija", "khadija@example.com", "pass123");
        UserModel entity = userMapper.toEntity(requestDTO);

        assertEquals("Khadija", entity.getName());
        assertEquals("khadija@example.com", entity.getEmail());
        assertEquals("pass123", entity.getPassword());

        // 2. Entité -> ResponseDTO (Sécurité : le mot de passe ne doit pas être présent dans le ResponseDTO)
        entity.setId(10L);
        UserResponseDTO responseDTO = userMapper.toResponseDTO(entity);

        assertEquals(10L, responseDTO.getId());
        assertEquals("Khadija", responseDTO.getName());
        assertEquals("khadija@example.com", responseDTO.getEmail());
    }

    @Test
    @DisplayName("BoardMapper - Conversion RequestDTO -> Entité et Entité -> ResponseDTO avec extraction de l'ownerId")
    void boardMapper_Conversions() {
        // 1. DTO -> Entité
        BoardRequestDTO requestDTO = new BoardRequestDTO("Tableau Kanban");
        BoardModel entity = boardMapper.toEntity(requestDTO);

        assertEquals("Tableau Kanban", entity.getTitle());

        // 2. Entité -> ResponseDTO (Vérification de l'extraction propre de l'ID du propriétaire)
        UserModel owner = new UserModel();
        owner.setId(20L);
        entity.setId(5L);
        entity.setOwner(owner);
        entity.setCreatedAt(LocalDateTime.now());

        BoardResponseDTO responseDTO = boardMapper.toResponseDTO(entity);

        assertEquals(5L, responseDTO.getId());
        assertEquals("Tableau Kanban", responseDTO.getTitle());
        assertEquals(20L, responseDTO.getOwnerId(), "L'ownerId du DTO doit correspondre à l'ID de l'entité UserModel");
    }

    @Test
    @DisplayName("TaskMapper - Conversion RequestDTO -> Entité et Entité -> ResponseDTO avec extraction du boardId")
    void taskMapper_Conversions() {
        // 1. DTO -> Entité
        TaskRequestDTO requestDTO = new TaskRequestDTO("Écrire les tests", "Couverture 100%", TaskStatus.EN_COURS);
        TaskModel entity = taskMapper.toEntity(requestDTO);

        assertEquals("Écrire les tests", entity.getTitle());
        assertEquals("Couverture 100%", entity.getDescription());
        assertEquals(TaskStatus.EN_COURS, entity.getStatus());

        // 2. Entité -> ResponseDTO
        BoardModel board = new BoardModel();
        board.setId(30L);
        entity.setId(50L);
        entity.setBoard(board);

        TaskResponseDTO responseDTO = taskMapper.toResponseDTO(entity);

        assertEquals(50L, responseDTO.getId());
        assertEquals("Écrire les tests", responseDTO.getTitle());
        assertEquals(TaskStatus.EN_COURS, responseDTO.getStatus());
        assertEquals(30L, responseDTO.getBoardId(), "Le boardId du DTO doit correspondre à l'ID du BoardModel associé");
    }
}

