package com.taskmanager.service;

import com.taskmanager.controller.dto.TaskUpdateRequest;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task existingTask;
    private static final Long EXISTING_ID = 1L;
    private static final Long NON_EXISTING_ID = 999L;

    @BeforeEach
    void setUp() {
        existingTask = new Task();
        existingTask.setId(EXISTING_ID);
        existingTask.setTitle("Original title");
        existingTask.setDescription("Original description");
        existingTask.setStatus(TaskStatus.TODO);
        existingTask.setPriority(TaskPriority.MEDIUM);
        existingTask.setDueDate(LocalDate.of(2025, 6, 1));
        existingTask.setCreatedAt(LocalDateTime.now());
        existingTask.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("createTask")
    class CreateTask {

        @Test
        @DisplayName("delegates to repository and returns saved task")
        void createTask_returnsSavedTask() {
            Task input = new Task();
            input.setTitle("New task");
            input.setDescription("Desc");
            input.setStatus(TaskStatus.TODO);
            input.setPriority(TaskPriority.HIGH);
            Task saved = new Task();
            saved.setId(2L);
            saved.setTitle(input.getTitle());
            saved.setDescription(input.getDescription());
            saved.setStatus(input.getStatus());
            saved.setPriority(input.getPriority());
            when(taskRepository.save(any(Task.class))).thenReturn(saved);

            Task result = taskService.createTask(input);

            assertThat(result).isSameAs(saved);
            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(input);
        }
    }

    @Nested
    @DisplayName("getTaskById")
    class GetTaskById {

        @Test
        @DisplayName("returns task when id exists")
        void getTaskById_existingId_returnsTask() {
            when(taskRepository.findById(EXISTING_ID)).thenReturn(Optional.of(existingTask));

            Task result = taskService.getTaskById(EXISTING_ID);

            assertThat(result).isSameAs(existingTask);
            verify(taskRepository).findById(EXISTING_ID);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when id does not exist")
        void getTaskById_nonExistingId_throws() {
            when(taskRepository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTaskById(NON_EXISTING_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Task not found with id: " + NON_EXISTING_ID);
            verify(taskRepository).findById(NON_EXISTING_ID);
        }
    }

    @Nested
    @DisplayName("getAllTasks")
    class GetAllTasks {

        @Test
        @DisplayName("returns list from repository")
        void getAllTasks_returnsRepositoryList() {
            List<Task> tasks = List.of(existingTask);
            when(taskRepository.findAll()).thenReturn(tasks);

            List<Task> result = taskService.getAllTasks();

            assertThat(result).isSameAs(tasks);
            verify(taskRepository).findAll();
        }

        @Test
        @DisplayName("getAllTasks(Pageable) returns page from repository")
        void getAllTasks_withPageable_returnsPage() {
            Pageable pageable = PageRequest.of(0, 10);
            List<Task> content = List.of(existingTask);
            Page<Task> page = new PageImpl<>(content, pageable, 1);
            when(taskRepository.findAll(pageable)).thenReturn(page);

            Page<Task> result = taskService.getAllTasks(pageable);

            assertThat(result).isSameAs(page);
            assertThat(result.getContent()).isEqualTo(content);
            assertThat(result.getTotalElements()).isOne();
            verify(taskRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("updateTask")
    class UpdateTask {

        @Test
        @DisplayName("updates existing task and returns saved entity")
        void updateTask_existingId_updatesAndReturns() {
            when(taskRepository.findById(EXISTING_ID)).thenReturn(Optional.of(existingTask));
            Task updates = new Task();
            updates.setTitle("Updated title");
            updates.setDescription("Updated desc");
            updates.setStatus(TaskStatus.IN_PROGRESS);
            updates.setPriority(TaskPriority.HIGH);
            updates.setDueDate(LocalDate.of(2025, 7, 15));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            Task result = taskService.updateTask(EXISTING_ID, updates);

            assertThat(result).isSameAs(existingTask);
            assertThat(existingTask.getTitle()).isEqualTo("Updated title");
            assertThat(existingTask.getDescription()).isEqualTo("Updated desc");
            assertThat(existingTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(existingTask.getPriority()).isEqualTo(TaskPriority.HIGH);
            assertThat(existingTask.getDueDate()).isEqualTo(LocalDate.of(2025, 7, 15));
            verify(taskRepository).save(existingTask);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when id does not exist")
        void updateTask_nonExistingId_throws() {
            when(taskRepository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());
            Task updates = new Task();
            updates.setTitle("Any");

            assertThatThrownBy(() -> taskService.updateTask(NON_EXISTING_ID, updates))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Task not found with id: " + NON_EXISTING_ID);
            verify(taskRepository, never()).save(any(Task.class));
        }
    }

    @Nested
    @DisplayName("patchTask")
    class PatchTask {

        @Test
        @DisplayName("patches status only when only status provided")
        void patchTask_statusOnly_updatesStatus() {
            when(taskRepository.findById(EXISTING_ID)).thenReturn(Optional.of(existingTask));
            TaskUpdateRequest request = new TaskUpdateRequest();
            request.setStatus(TaskStatus.DONE);
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            Task result = taskService.patchTask(EXISTING_ID, request);

            assertThat(result).isSameAs(existingTask);
            assertThat(existingTask.getStatus()).isEqualTo(TaskStatus.DONE);
            assertThat(existingTask.getPriority()).isEqualTo(TaskPriority.MEDIUM);
            verify(taskRepository).save(existingTask);
        }

        @Test
        @DisplayName("patches priority only when only priority provided")
        void patchTask_priorityOnly_updatesPriority() {
            when(taskRepository.findById(EXISTING_ID)).thenReturn(Optional.of(existingTask));
            TaskUpdateRequest request = new TaskUpdateRequest();
            request.setPriority(TaskPriority.LOW);
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            Task result = taskService.patchTask(EXISTING_ID, request);

            assertThat(result).isSameAs(existingTask);
            assertThat(existingTask.getStatus()).isEqualTo(TaskStatus.TODO);
            assertThat(existingTask.getPriority()).isEqualTo(TaskPriority.LOW);
            verify(taskRepository).save(existingTask);
        }

        @Test
        @DisplayName("patches both status and priority when both provided")
        void patchTask_bothFields_updatesBoth() {
            when(taskRepository.findById(EXISTING_ID)).thenReturn(Optional.of(existingTask));
            TaskUpdateRequest request = new TaskUpdateRequest();
            request.setStatus(TaskStatus.IN_PROGRESS);
            request.setPriority(TaskPriority.HIGH);
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            Task result = taskService.patchTask(EXISTING_ID, request);

            assertThat(result).isSameAs(existingTask);
            assertThat(existingTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(existingTask.getPriority()).isEqualTo(TaskPriority.HIGH);
            verify(taskRepository).save(existingTask);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when id does not exist")
        void patchTask_nonExistingId_throws() {
            when(taskRepository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());
            TaskUpdateRequest request = new TaskUpdateRequest();
            request.setStatus(TaskStatus.DONE);

            assertThatThrownBy(() -> taskService.patchTask(NON_EXISTING_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Task not found with id: " + NON_EXISTING_ID);
            verify(taskRepository, never()).save(any(Task.class));
        }
    }

    @Nested
    @DisplayName("deleteTask")
    class DeleteTask {

        @Test
        @DisplayName("deletes task when id exists")
        void deleteTask_existingId_callsDelete() {
            when(taskRepository.findById(EXISTING_ID)).thenReturn(Optional.of(existingTask));

            taskService.deleteTask(EXISTING_ID);

            verify(taskRepository).findById(EXISTING_ID);
            verify(taskRepository).delete(existingTask);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when id does not exist")
        void deleteTask_nonExistingId_throws() {
            when(taskRepository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.deleteTask(NON_EXISTING_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Task not found with id: " + NON_EXISTING_ID);
            verify(taskRepository, never()).delete(any(Task.class));
        }
    }

    @Nested
    @DisplayName("getTasksByStatus")
    class GetTasksByStatus {

        @Test
        @DisplayName("delegates to repository and returns list")
        void getTasksByStatus_returnsRepositoryList() {
            List<Task> tasks = List.of(existingTask);
            when(taskRepository.findByStatus(TaskStatus.TODO)).thenReturn(tasks);

            List<Task> result = taskService.getTasksByStatus(TaskStatus.TODO);

            assertThat(result).isSameAs(tasks);
            verify(taskRepository).findByStatus(TaskStatus.TODO);
        }
    }

    @Nested
    @DisplayName("getTasksByPriority")
    class GetTasksByPriority {

        @Test
        @DisplayName("delegates to repository and returns list")
        void getTasksByPriority_returnsRepositoryList() {
            List<Task> tasks = List.of(existingTask);
            when(taskRepository.findByPriority(TaskPriority.HIGH)).thenReturn(tasks);

            List<Task> result = taskService.getTasksByPriority(TaskPriority.HIGH);

            assertThat(result).isSameAs(tasks);
            verify(taskRepository).findByPriority(TaskPriority.HIGH);
        }
    }

    @Nested
    @DisplayName("getTasksByStatusAndPriority")
    class GetTasksByStatusAndPriority {

        @Test
        @DisplayName("delegates to repository and returns list")
        void getTasksByStatusAndPriority_returnsRepositoryList() {
            List<Task> tasks = List.of(existingTask);
            when(taskRepository.findByStatusAndPriority(TaskStatus.DONE, TaskPriority.LOW)).thenReturn(tasks);

            List<Task> result = taskService.getTasksByStatusAndPriority(TaskStatus.DONE, TaskPriority.LOW);

            assertThat(result).isSameAs(tasks);
            verify(taskRepository).findByStatusAndPriority(TaskStatus.DONE, TaskPriority.LOW);
        }
    }

    @Nested
    @DisplayName("searchTasksByTitle")
    class SearchTasksByTitle {

        @Test
        @DisplayName("delegates to repository and returns list")
        void searchTasksByTitle_returnsRepositoryList() {
            List<Task> tasks = List.of(existingTask);
            when(taskRepository.findByTitleContainingIgnoreCase("exam")).thenReturn(tasks);

            List<Task> result = taskService.searchTasksByTitle("exam");

            assertThat(result).isSameAs(tasks);
            verify(taskRepository).findByTitleContainingIgnoreCase("exam");
        }
    }

    @Nested
    @DisplayName("getTasksWithFilters")
    class GetTasksWithFilters {

        @Test
        @DisplayName("delegates to repository and returns list")
        void getTasksWithFilters_returnsRepositoryList() {
            List<Task> tasks = List.of(existingTask);
            when(taskRepository.findWithFilters("foo", TaskStatus.TODO, TaskPriority.MEDIUM)).thenReturn(tasks);

            List<Task> result = taskService.getTasksWithFilters("foo", TaskStatus.TODO, TaskPriority.MEDIUM);

            assertThat(result).isSameAs(tasks);
            verify(taskRepository).findWithFilters("foo", TaskStatus.TODO, TaskPriority.MEDIUM);
        }
    }
}
