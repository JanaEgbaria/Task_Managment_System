package com.taskmanager.service;

import com.taskmanager.controller.dto.TaskUpdateRequest;
import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TaskService {

    Task createTask(Task task);

    Task updateTask(Long id, Task task);

    Task patchTask(Long id, TaskUpdateRequest request);

    void deleteTask(Long id);

    Task getTaskById(Long id);

    List<Task> getAllTasks();

    Page<Task> getAllTasks(Pageable pageable);

    List<Task> getTasksByStatus(TaskStatus status);

    List<Task> getTasksByPriority(TaskPriority priority);

    List<Task> getTasksByStatusAndPriority(TaskStatus status, TaskPriority priority);

    List<Task> searchTasksByTitle(String title);

    /**
     * Get tasks filtered by optional title (case-insensitive partial match), status, and priority.
     * Pass null for any parameter to omit that filter.
     */
    List<Task> getTasksWithFilters(String title, TaskStatus status, TaskPriority priority);
}
