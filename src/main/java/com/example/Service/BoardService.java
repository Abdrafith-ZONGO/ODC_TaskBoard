package com.example.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.DTO.BoardRequestDTO;
import com.example.DTO.BoardResponseDTO;
import com.example.Exception.ResourceNotFoundException;
import com.example.Mapper.BoardMapper;
import com.example.Model.BoardModel;
import com.example.Model.UserModel;
import com.example.Repository.BoardRepository;

import lombok.RequiredArgsConstructor;

/**
 * Couche Service : contient la LOGIQUE METIER pour Board.
 */
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserService userService;
    private final BoardMapper boardMapper;

    // CREATE

    public BoardResponseDTO createBoard(Long ownerId, BoardRequestDTO requestDTO) {
        UserModel owner = userService.getUserEntityById(ownerId);
        BoardModel board = boardMapper.toEntity(requestDTO);
        board.setOwner(owner);

        BoardModel savedBoard = boardRepository.save(board);
        return boardMapper.toResponseDTO(savedBoard);
    }

    // READ

    public List<BoardResponseDTO> getAllBoards() {
        return boardRepository.findAll()
                .stream()
                .map(boardMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<BoardResponseDTO> getBoardsByOwner(Long ownerId) {
        userService.getUserEntityById(ownerId);
        return boardRepository.findByOwnerId(ownerId)
                .stream()
                .map(boardMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public BoardResponseDTO getBoardById(Long id) {
        BoardModel board = getBoardEntityById(id);
        return boardMapper.toResponseDTO(board);
    }

    public BoardModel getBoardEntityById(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tableau introuvable avec id=" + id));
    }

    // UPDATE

    public BoardResponseDTO updateBoard(Long id, BoardRequestDTO requestDTO) {
        BoardModel existingBoard = getBoardEntityById(id);
        existingBoard.setTitle(requestDTO.getTitle());

        BoardModel updatedBoard = boardRepository.save(existingBoard);
        return boardMapper.toResponseDTO(updatedBoard);
    }

    // DELETE

    public void deleteBoard(Long id) {
        if (!boardRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Tableau introuvable avec id=" + id);
        }
        boardRepository.deleteById(id);
    }
}
