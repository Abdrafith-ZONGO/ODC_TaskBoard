package com.example.TaskBoard.Service;


import java.time.LocalDateTime;
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

import com.example.DTO.BoardRequestDTO;
import com.example.DTO.BoardResponseDTO;
import com.example.Exception.ResourceNotFoundException;
import com.example.Mapper.BoardMapper;
import com.example.Model.BoardModel;
import com.example.Model.UserModel;
import com.example.Repository.BoardRepository;
import com.example.Service.BoardService;
import com.example.Service.UserService;

/**
 * =============================================================================
 * TESTS UNITAIRES : BoardServiceTest
 * =============================================================================
 *
 * OBJECTIF PÉDAGOGIQUE :
 * - Vérifier la gestion métier des tableaux Kanban (Board).
 * - Notamment la relation One-to-Many / Many-to-One avec `UserModel` (un Board a un propriétaire).
 * - Utilisation des mocks Mockito pour simuler `BoardRepository`, `UserService` et `BoardMapper`.
 */
@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private UserService userService;

    @Mock
    private BoardMapper boardMapper;

    @InjectMocks
    private BoardService boardService;

    private UserModel owner;
    private BoardModel board;
    private BoardRequestDTO boardRequestDTO;
    private BoardResponseDTO boardResponseDTO;

    @BeforeEach
    void setUp() {
        // Propriétaire simulé
        owner = new UserModel();
        owner.setId(10L);
        owner.setName("Bob");
        owner.setEmail("bob@example.com");

        // Entité tableau simulée
        board = new BoardModel();
        board.setId(1L);
        board.setTitle("Projet Backend");
        board.setOwner(owner);
        board.setCreatedAt(LocalDateTime.now());

        boardRequestDTO = new BoardRequestDTO("Projet Backend");
        boardResponseDTO = new BoardResponseDTO(1L, "Projet Backend", LocalDateTime.now(), 10L);
    }

    // =========================================================================
    // TESTS : CREATE BOARD
    // =========================================================================

    @Test
    @DisplayName("createBoard - Cas nominal : création et association à un utilisateur propriétaire")
    void createBoard_Success() {
        // 1. ARRANGE
        // Le UserService retrouve bien le propriétaire
        when(userService.getUserEntityById(10L)).thenReturn(owner);
        when(boardMapper.toEntity(boardRequestDTO)).thenReturn(board);
        when(boardRepository.save(board)).thenReturn(board);
        when(boardMapper.toResponseDTO(board)).thenReturn(boardResponseDTO);

        // 2. ACT
        BoardResponseDTO result = boardService.createBoard(10L, boardRequestDTO);

        // 3. ASSERT
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Projet Backend", result.getTitle());
        assertEquals(10L, result.getOwnerId());
        
        // Vérifie que le propriétaire a bien été recherché et la sauvegarde déclenchée
        verify(userService).getUserEntityById(10L);
        verify(boardRepository).save(board);
    }

    // =========================================================================
    // TESTS : READ BOARDS
    // =========================================================================

    @Test
    @DisplayName("getAllBoards - Retourne l'ensemble des tableaux")
    void getAllBoards_ReturnsList() {
        // 1. ARRANGE
        when(boardRepository.findAll()).thenReturn(List.of(board));
        when(boardMapper.toResponseDTO(board)).thenReturn(boardResponseDTO);

        // 2. ACT
        List<BoardResponseDTO> result = boardService.getAllBoards();

        // 3. ASSERT
        assertEquals(1, result.size());
        assertEquals("Projet Backend", result.get(0).getTitle());
        verify(boardRepository).findAll();
    }

    @Test
    @DisplayName("getBoardsByOwner - Filtre et retourne les tableaux d'un propriétaire spécifique")
    void getBoardsByOwner_Success() {
        // 1. ARRANGE
        when(userService.getUserEntityById(10L)).thenReturn(owner);
        when(boardRepository.findByOwnerId(10L)).thenReturn(List.of(board));
        when(boardMapper.toResponseDTO(board)).thenReturn(boardResponseDTO);

        // 2. ACT
        List<BoardResponseDTO> result = boardService.getBoardsByOwner(10L);

        // 3. ASSERT
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getOwnerId());
        verify(boardRepository).findByOwnerId(10L);
    }

    @Test
    @DisplayName("getBoardById - Cas nominal : tableau trouvé")
    void getBoardById_Success() {
        // 1. ARRANGE
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(boardMapper.toResponseDTO(board)).thenReturn(boardResponseDTO);

        // 2. ACT
        BoardResponseDTO result = boardService.getBoardById(1L);

        // 3. ASSERT
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("getBoardById - Échec : tableau introuvable lève ResourceNotFoundException")
    void getBoardById_NotFound_ThrowsResourceNotFoundException() {
        // 1. ARRANGE
        when(boardRepository.findById(99L)).thenReturn(Optional.empty());

        // 2. ACT & 3. ASSERT
        assertThrows(ResourceNotFoundException.class, () -> boardService.getBoardById(99L));
    }

    // =========================================================================
    // TESTS : UPDATE BOARD
    // =========================================================================

    @Test
    @DisplayName("updateBoard - Cas nominal : modification du titre d'un tableau")
    void updateBoard_Success() {
        // 1. ARRANGE
        BoardRequestDTO updateRequest = new BoardRequestDTO("Nouveau Titre");
        BoardResponseDTO updatedResponse = new BoardResponseDTO(1L, "Nouveau Titre", LocalDateTime.now(), 10L);

        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(boardRepository.save(board)).thenReturn(board);
        when(boardMapper.toResponseDTO(board)).thenReturn(updatedResponse);

        // 2. ACT
        BoardResponseDTO result = boardService.updateBoard(1L, updateRequest);

        // 3. ASSERT
        assertNotNull(result);
        assertEquals("Nouveau Titre", result.getTitle());
        verify(boardRepository).save(board);
    }

    // =========================================================================
    // TESTS : DELETE BOARD
    // =========================================================================

    @Test
    @DisplayName("deleteBoard - Cas nominal : suppression d'un tableau existant")
    void deleteBoard_Success() {
        // 1. ARRANGE
        when(boardRepository.existsById(1L)).thenReturn(true);

        // 2. ACT
        boardService.deleteBoard(1L);

        // 3. ASSERT
        verify(boardRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deleteBoard - Échec : tentative de suppression d'un tableau inexistant")
    void deleteBoard_NotFound_ThrowsResourceNotFoundException() {
        // 1. ARRANGE
        when(boardRepository.existsById(99L)).thenReturn(false);

        // 2. ACT & 3. ASSERT
        assertThrows(ResourceNotFoundException.class, () -> boardService.deleteBoard(99L));
        verify(boardRepository, never()).deleteById(any());
    }
}

