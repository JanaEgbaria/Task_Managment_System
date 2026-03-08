package com.taskmanager.controller;

import com.taskmanager.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String TASKS_URL = "/api/tasks";

    @BeforeEach
    void setUpFilter() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }
    private static final long NON_EXISTING_ID = 999999L;

    private static final String VALID_TASK_JSON = """
            {"title":"Integration task","description":"Test desc","status":"TODO","priority":"MEDIUM","dueDate":"2025-12-01"}
            """;

    private static final String INVALID_TASK_JSON_BLANK_TITLE = """
            {"title":"","description":"Desc","status":"TODO","priority":"MEDIUM"}
            """;

    @Nested
    @DisplayName("POST /api/tasks (create)")
    class CreateTask {

        @Test
        @WithMockUser
        @DisplayName("returns 201 and created task with Location header")
        void createTask_validBody_returns201AndBody() throws Exception {
            ResultActions result = mockMvc.perform(post(TASKS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_TASK_JSON));

            result.andExpect(status().isCreated())
                    .andExpect(header().string("Location", startsWith(TASKS_URL + "/")))
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.title").value("Integration task"))
                    .andExpect(jsonPath("$.description").value("Test desc"))
                    .andExpect(jsonPath("$.status").value("TODO"))
                    .andExpect(jsonPath("$.priority").value("MEDIUM"))
                    .andExpect(jsonPath("$.dueDate").value("2025-12-01"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        @WithMockUser
        @DisplayName("returns 400 when title is blank")
        void createTask_blankTitle_returns400() throws Exception {
            mockMvc.perform(post(TASKS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(INVALID_TASK_JSON_BLANK_TITLE))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/tasks (get all)")
    class GetAllTasks {

        @Test
        @WithMockUser
        @DisplayName("returns 200 and paged response with default page and size")
        void getAllTasks_returns200AndPagedResponse() throws Exception {
            mockMvc.perform(get(TASKS_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").isNumber())
                    .andExpect(jsonPath("$.totalPages").isNumber())
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.number").value(0));
        }

        @Test
        @WithMockUser
        @DisplayName("returns 200 and respects page and size query params")
        void getAllTasks_withPageAndSize_returnsRequestedPage() throws Exception {
            mockMvc.perform(get(TASKS_URL).param("page", "0").param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.size").value(5))
                    .andExpect(jsonPath("$.number").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/{id} (get by id)")
    class GetTaskById {

        @Test
        @WithMockUser
        @DisplayName("returns 200 and task when id exists")
        void getTaskById_existingId_returns200AndBody() throws Exception {
            String location = createTaskAndGetLocation();
            long id = extractIdFromLocation(location);

            mockMvc.perform(get(TASKS_URL + "/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.title").value("Integration task"));
        }

        @Test
        @WithMockUser
        @DisplayName("returns 404 when id does not exist")
        void getTaskById_nonExistingId_returns404() throws Exception {
            mockMvc.perform(get(TASKS_URL + "/" + NON_EXISTING_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/tasks/{id} (update)")
    class UpdateTask {

        @Test
        @WithMockUser
        @DisplayName("returns 200 and updated task when id exists")
        void updateTask_existingId_returns200AndUpdatedBody() throws Exception {
            String location = createTaskAndGetLocation();
            long id = extractIdFromLocation(location);
            String updateJson = """
                    {"title":"Updated title","description":"Updated desc","status":"IN_PROGRESS","priority":"HIGH","dueDate":"2026-01-15"}
                    """;

            mockMvc.perform(put(TASKS_URL + "/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.title").value("Updated title"))
                    .andExpect(jsonPath("$.description").value("Updated desc"))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.priority").value("HIGH"))
                    .andExpect(jsonPath("$.dueDate").value("2026-01-15"));
        }

        @Test
        @WithMockUser
        @DisplayName("returns 404 when id does not exist")
        void updateTask_nonExistingId_returns404() throws Exception {
            String updateJson = """
                    {"title":"Any","description":"","status":"TODO","priority":"MEDIUM"}
                    """;
            mockMvc.perform(put(TASKS_URL + "/" + NON_EXISTING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/tasks/{id} (patch)")
    class PatchTask {

        @Test
        @WithMockUser
        @DisplayName("returns 200 and patched task when id exists")
        void patchTask_existingId_statusOnly_returns200AndUpdatedStatus() throws Exception {
            String location = createTaskAndGetLocation();
            long id = extractIdFromLocation(location);
            String patchJson = "{\"status\":\"DONE\"}";

            mockMvc.perform(patch(TASKS_URL + "/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(patchJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.status").value("DONE"));
        }

        @Test
        @WithMockUser
        @DisplayName("returns 404 when id does not exist")
        void patchTask_nonExistingId_returns404() throws Exception {
            String patchJson = "{\"status\":\"DONE\"}";
            mockMvc.perform(patch(TASKS_URL + "/" + NON_EXISTING_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(patchJson))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/tasks/{id} (delete)")
    class DeleteTask {

        @Test
        @WithMockUser
        @DisplayName("returns 204 when id exists; then get returns 404")
        void deleteTask_existingId_returns204ThenGet404() throws Exception {
            String location = createTaskAndGetLocation();
            long id = extractIdFromLocation(location);

            mockMvc.perform(delete(TASKS_URL + "/" + id))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(TASKS_URL + "/" + id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("returns 404 when id does not exist")
        void deleteTask_nonExistingId_returns404() throws Exception {
            mockMvc.perform(delete(TASKS_URL + "/" + NON_EXISTING_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/search (search)")
    class SearchTasks {

        @Test
        @WithMockUser
        @DisplayName("returns 200 and array when no title param")
        void search_noParam_returns200AndArray() throws Exception {
            mockMvc.perform(get(TASKS_URL + "/search"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser
        @DisplayName("returns 200 and array when title param provided")
        void search_withTitle_returns200AndArray() throws Exception {
            mockMvc.perform(get(TASKS_URL + "/search").param("title", "foo"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/filter (filter)")
    class FilterTasks {

        @Test
        @WithMockUser
        @DisplayName("returns 200 and array when no params")
        void filter_noParams_returns200AndArray() throws Exception {
            mockMvc.perform(get(TASKS_URL + "/filter"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser
        @DisplayName("returns 200 and array when status param")
        void filter_statusOnly_returns200AndArray() throws Exception {
            mockMvc.perform(get(TASKS_URL + "/filter").param("status", "TODO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser
        @DisplayName("returns 200 and array when priority param")
        void filter_priorityOnly_returns200AndArray() throws Exception {
            mockMvc.perform(get(TASKS_URL + "/filter").param("priority", "HIGH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser
        @DisplayName("returns 200 and array when status and priority params")
        void filter_statusAndPriority_returns200AndArray() throws Exception {
            mockMvc.perform(get(TASKS_URL + "/filter")
                            .param("status", "DONE")
                            .param("priority", "LOW"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("Authentication")
    class Authentication {

        @Test
        @DisplayName("GET /api/tasks without auth returns 403")
        void getTasks_withoutAuth_returns403() throws Exception {
            mockMvc.perform(get(TASKS_URL))
                    .andExpect(status().isForbidden());
        }
    }

    private String createTaskAndGetLocation() throws Exception {
        return mockMvc.perform(post(TASKS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TASK_JSON))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
    }

    private long extractIdFromLocation(String location) {
        String idPart = location.substring(location.lastIndexOf('/') + 1);
        return Long.parseLong(idPart);
    }
}
