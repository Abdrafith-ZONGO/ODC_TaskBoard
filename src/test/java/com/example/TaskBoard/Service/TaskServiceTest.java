package com.example.TaskBoard.Service;


import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.DTO.TaskRequestDTO;
import com.example.DTO.TaskResponseDTO;
import com.example.Exception.ResourceNotFoundException;
import com.example.Mapper.TaskMapper;
import com.example.Model.BoardModel;
import com.example.Model.TaskModel;
import com.example.Model.TaskStatus;
import com.example.Repository.TaskRepository;
import com.example.Service.BoardService;
import com.example.Service.TaskService;

/**
 * =============================================================================
 * TESTS UNITAIRES : TaskServiceTest
 * =============================================================================
 *
 * OBJECTIF PÉDAGOGIQUE :
 * - Tester la logique métier liée aux tâches (TaskModel).
 * - Vérifier les règles par défaut : si aucun statut n'est fourni à la création,
 *   la tâche prend automatiquement la valeur `TaskStatus.A_FAIRE`.
 * - Vérifier les transitions d'état (`A_FAIRE` -> `EN_COURS` -> `TERMINE`).
 * - Valider le rattachement obligatoire d'une tâche à un tableau (`BoardModel`).
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private BoardService boardService;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    private BoardModel board;
    private TaskModel task;
    private TaskRequestDTO taskRequestDTO;
    private TaskResponseDTO taskResponseDTO;

    @BeforeEach
    void setUp() {
        // Tableau parent simulé
        board = new BoardModel();
        board.setId(5L);
        board.setTitle("Tableau Sprint");

        // Entité tâche simulée
        task = new TaskModel();
        task.setId(100L);
        task.setTitle("Configurer la BDD");
        task.setDescription("Installer PostgreSQL");
        task.setStatus(TaskStatus.A_FAIRE);
        task.setBoard(board);

        // Requête sans statut spécifié (pour tester le statut par défaut)
        taskRequestDTO = new TaskRequestDTO("Configurer la BDD", "Installer PostgreSQL", null);
        taskResponseDTO = new TaskResponseDTO(100L, "Configurer la BDD", "Installer PostgreSQL", TaskStatus.A_FAIRE, 5L);
    }

    // =========================================================================
    // TESTS : CREATE TASK
    // =========================================================================

    @Test
    @DisplayName("createTask - Règle métier : le statut est par défaut A_FAIRE si non renseigné")
    void createTask_DefaultStatusIsAFaire() {
        // 1. ARRANGE : On simule la recherche du tableau parent et la sauvegarde
        when(boardService.getBoardEntityById(5L)).thenReturn(board);
        when(taskMapper.toEntity(taskRequestDTO)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponseDTO(task)).thenReturn(taskResponseDTO);

        // 2. ACT : Création de la tâche
        TaskResponseDTO result = taskService.createTask(5L, taskRequestDTO);

        // 3. ASSERT
        assertNotNull(result);
        assertEquals(TaskStatus.A_FAIRE, result.getStatus(), "Le statut par défaut doit être A_FAIRE");
        assertEquals(5L, result.getBoardId());
        verify(taskRepository).save(task);
    }

    @Test
    @DisplayName("createTask - Cas où un statut explicite (ex: EN_COURS) est fourni")
    void createTask_WithCustomStatus() {
        // 1. ARRANGE : La requête fournit déjà EN_COURS
        TaskRequestDTO customDTO = new TaskRequestDTO("Design", "Figma", TaskStatus.EN_COURS);
        TaskModel customTask = new TaskModel(101L, "Design", "Figma", TaskStatus.EN_COURS, board);
        TaskResponseDTO customResponse = new TaskResponseDTO(101L, "Design", "Figma", TaskStatus.EN_COURS, 5L);

        when(boardService.getBoardEntityById(5L)).thenReturn(board);
        when(taskMapper.toEntity(customDTO)).thenReturn(customTask);
        when(taskRepository.save(customTask)).thenReturn(customTask);
        when(taskMapper.toResponseDTO(customTask)).thenReturn(customResponse);

        // 2. ACT
        TaskResponseDTO result = taskService.createTask(5L, customDTO);

        // 3. ASSERT
        assertNotNull(result);
        assertEquals(TaskStatus.EN_COURS, result.getStatus(), "Le statut explicite doit être conservé");
    }

    // =========================================================================
    // TESTS : READ TASKS
    // =========================================================================

    @Test
    @DisplayName("getTasksByBoard - Retourne la liste des tâches d'un tableau donné")
    void getTasksByBoard_Success() {
        // 1. ARRANGE
        when(boardService.getBoardEntityById(5L)).thenReturn(board);
        when(taskRepository.findByBoardId(5L)).thenReturn(List.of(task));
        when(taskMapper.toResponseDTO(task)).thenReturn(taskResponseDTO);

        // 2. ACT
        List<TaskResponseDTO> tasks = taskService.getTasksByBoard(5L);

        // 3. ASSERT
        assertEquals(1, tasks.size());
        assertEquals("Configurer la BDD", tasks.get(0).getTitle());
        verify(taskRepository).findByBoardId(5L);
    }

    @Test
    @DisplayName("getTaskById - Cas nominal : tâche trouvée")
    void getTaskById_Success() {
        // 1. ARRANGE
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(taskMapper.toResponseDTO(task)).thenReturn(taskResponseDTO);

        // 2. ACT
        TaskResponseDTO result = taskService.getTaskById(100L);

        // 3. ASSERT
        assertNotNull(result);
        assertEquals(100L, result.getId());
    }

    @Test
    @DisplayName("getTaskById - Échec : tâche introuvable lève ResourceNotFoundException")
    void getTaskById_NotFound_ThrowsResourceNotFoundException() {
        // 1. ARRANGE
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // 2. ACT & 3. ASSERT
        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskById(999L));
    }

    // =========================================================================
    // TESTS : UPDATE TASK
    // =========================================================================

    @Test
    @DisplayName("updateTask - Cas nominal : mise à jour du titre, description et transition vers TERMINE")
    void updateTask_Success() {
        // 1. ARRANGE
        TaskRequestDTO updateDTO = new TaskRequestDTO("Titre modifié", "Nouvelle desc", TaskStatus.TERMINE);
        TaskResponseDTO updatedResponse = new TaskResponseDTO(100L, "Titre modifié", "Nouvelle desc", TaskStatus.TERMINE, 5L);

        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponseDTO(task)).thenReturn(updatedResponse);

        // 2. ACT
        TaskResponseDTO result = taskService.updateTask(100L, updateDTO);

        // 3. ASSERT
        assertNotNull(result);
        assertEquals("Titre modifié", result.getTitle());
        assertEquals(TaskStatus.TERMINE, result.getStatus());
        verify(taskRepository).save(task);
    }

    // =========================================================================
    // TESTS : DELETE TASK
    // =========================================================================

    @Test
    @DisplayName("deleteTask - Cas nominal : suppression d'une tâche existante")
    void deleteTask_Success() {
        // 1. ARRANGE
        when(taskRepository.existsById(100L)).thenReturn(true);

        // 2. ACT
        taskService.deleteTask(100L);

        // 3. ASSERT
        verify(taskRepository, times(1)).deleteById(100L);
    }

    @Test
    @DisplayName("deleteTask - Échec : tentative de suppression d'une tâche inexistante")
    void deleteTask_NotFound_ThrowsResourceNotFoundException() {
        // 1. ARRANGE
        when(taskRepository.existsById(999L)).thenReturn(false);

        // 2. ACT & 3. ASSERT
        assertThrows(ResourceNotFoundException.class, () -> taskService.deleteTask(999L));
        verify(taskRepository, never()).deleteById(any());
    }
}

