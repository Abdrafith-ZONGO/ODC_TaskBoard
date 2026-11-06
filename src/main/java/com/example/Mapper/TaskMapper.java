package com.example.Mapper;

import org.springframework.stereotype.Component;

import com.example.DTO.TaskRequestDTO;
import com.example.DTO.TaskResponseDTO;
import com.example.Model.TaskModel;

@Component
public class TaskMapper {

    /**
     * Convertit un DTO de création/mise à jour en Entité JPA.
     */
    public TaskModel toEntity(TaskRequestDTO dto) {
        TaskModel task = new TaskModel();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        return task;
    }

    /**
     * Convertit une Entité JPA en DTO de réponse.
     */
    public TaskResponseDTO toResponseDTO(TaskModel task) {
        return TaskResponseDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .boardId(task.getBoard() != null ? task.getBoard().getId() : null)
                .build();
    }
}

