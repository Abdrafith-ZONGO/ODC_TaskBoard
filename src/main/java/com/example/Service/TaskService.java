package com.example.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.DTO.TaskRequestDTO;
import com.example.DTO.TaskResponseDTO;
import com.example.Exception.ResourceNotFoundException;
import com.example.Mapper.TaskMapper;
import com.example.Model.BoardModel;
import com.example.Model.TaskModel;
import com.example.Model.TaskStatus;
import com.example.Repository.TaskRepository;

import lombok.RequiredArgsConstructor;

/**
 * Couche Service : contient la LOGIQUE METIER pour Task.
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final BoardService boardService;
    private final TaskMapper taskMapper;

    // CREATE

    public TaskResponseDTO createTask(Long boardId, TaskRequestDTO requestDTO) {
        BoardModel board = boardService.getBoardEntityById(boardId);

        TaskModel task = taskMapper.toEntity(requestDTO);
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.A_FAIRE);
        }
        task.setBoard(board);

        TaskModel savedTask = taskRepository.save(task);
        return taskMapper.toResponseDTO(savedTask);
    }

    // READ

    public List<TaskResponseDTO> getTasksByBoard(Long boardId) {
        boardService.getBoardEntityById(boardId);
        return taskRepository.findByBoardId(boardId)
                .stream()
                .map(taskMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public TaskResponseDTO getTaskById(Long id) {
        TaskModel task = getTaskEntityById(id);
        return taskMapper.toResponseDTO(task);
    }

    public TaskModel getTaskEntityById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tache introuvable avec id=" + id));
    }

    // UPDATE

    public TaskResponseDTO updateTask(Long id, TaskRequestDTO requestDTO) {
        TaskModel existingTask = getTaskEntityById(id);

        existingTask.setTitle(requestDTO.getTitle());
        existingTask.setDescription(requestDTO.getDescription());
        if (requestDTO.getStatus() != null) {
            existingTask.setStatus(requestDTO.getStatus());
        }

        TaskModel updatedTask = taskRepository.save(existingTask);
        return taskMapper.toResponseDTO(updatedTask);
    }

    // DELETE

    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Tache introuvable avec id=" + id);
        }
        taskRepository.deleteById(id);
    }
}
