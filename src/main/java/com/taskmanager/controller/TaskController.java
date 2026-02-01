package com.taskmanager.controller;
import com.taskmanager.controller.dto.TaskRequest;
import com.taskmanager.controller.dto.TaskResponse;
import com.taskmanager.controller.dto.TaskUpdateRequest;
import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final TaskMapper taskMapper;

    public TaskController(TaskService taskService, TaskMapper taskMapper) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@RequestBody @Valid TaskRequest request) {
        Task task = taskMapper.toEntity(request);
        Task created = taskService.createTask(task);
        TaskResponse response = taskMapper.toResponse(created);
        return ResponseEntity.created(URI.create("/api/tasks/" + created.getId())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        List<TaskResponse> responses = taskService.getAllTasks().stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        Task task = taskService.getTaskById(id);
        return ResponseEntity.ok(taskMapper.toResponse(task));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id, @RequestBody @Valid TaskRequest request) {
        Task existing = taskService.getTaskById(id);
        taskMapper.updateEntity(existing, request);
        Task updated = taskService.updateTask(id, existing);
        return ResponseEntity.ok(taskMapper.toResponse(updated));
    }

    @PatchMapping("/{id:\\d+}")
    public ResponseEntity<TaskResponse> patchTask(@PathVariable Long id, @RequestBody TaskUpdateRequest request) {
        Task updated = taskService.patchTask(id, request);
        return ResponseEntity.ok(taskMapper.toResponse(updated));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<TaskResponse>> searchTasksByTitle(
            @RequestParam(required = false) String title) {
        List<TaskResponse> responses;
        if (title == null || title.isBlank()) {
            responses = taskService.getAllTasks().stream()
                    .map(taskMapper::toResponse)
                    .collect(Collectors.toList());
        } else {
            responses = taskService.searchTasksByTitle(title.trim()).stream()
                    .map(taskMapper::toResponse)
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(responses);
    }

    /**
     * Filter tasks by optional status and/or priority.
     * Both params optional: both missing → all tasks; only status → by status; only priority → by priority; both → AND logic.
     * Empty or blank param values are treated as missing (not sent to backend by frontend; defensive handling here).
     */
    @GetMapping("/filter")
    public ResponseEntity<List<TaskResponse>> filterTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        TaskStatus statusEnum = parseStatus(status);
        TaskPriority priorityEnum = parsePriority(priority);
        List<Task> tasks;
        if (statusEnum != null && priorityEnum != null) {
            tasks = taskService.getTasksByStatusAndPriority(statusEnum, priorityEnum);
        } else if (statusEnum != null) {
            tasks = taskService.getTasksByStatus(statusEnum);
        } else if (priorityEnum != null) {
            tasks = taskService.getTasksByPriority(priorityEnum);
        } else {
            tasks = taskService.getAllTasks();
        }
        List<TaskResponse> responses = tasks.stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private static TaskStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return TaskStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static TaskPriority parsePriority(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return TaskPriority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
