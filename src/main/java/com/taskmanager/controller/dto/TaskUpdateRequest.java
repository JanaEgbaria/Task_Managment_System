package com.taskmanager.controller.dto;

import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;

/**
 * DTO for partial (PATCH) task updates. All fields are optional.
 * Only non-null fields are applied; title and dueDate are never changed.
 */
public class TaskUpdateRequest {

    private TaskStatus status;
    private TaskPriority priority;

    public TaskUpdateRequest() {
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }
}
