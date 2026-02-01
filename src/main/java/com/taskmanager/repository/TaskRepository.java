package com.taskmanager.repository;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByPriority(TaskPriority priority);

    List<Task> findByStatusAndPriority(TaskStatus status, TaskPriority priority);

    List<Task> findByTitleContainingIgnoreCase(String title);

    /**
     * Find tasks with optional title (case-insensitive partial match), status, and priority.
     * Pass null for any parameter to omit that filter.
     */
    @Query("SELECT t FROM Task t WHERE " +
            "(:title IS NULL OR :title = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:priority IS NULL OR t.priority = :priority)")
    List<Task> findWithFilters(@Param("title") String title,
                               @Param("status") TaskStatus status,
                               @Param("priority") TaskPriority priority);
}
