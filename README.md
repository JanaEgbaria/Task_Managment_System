# Task Management System

A RESTful web application for managing tasks, built with Spring Boot 3 and PostgreSQL. This project demonstrates modern backend development practices including layered architecture, validation, auditing, and database migrations.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Technologies Used](#technologies-used)
3. [How to Run the Project](#how-to-run-the-project)
4. [Database Configuration](#database-configuration)
5. [Authentication](#authentication)
6. [API Documentation](#api-documentation)
7. [Validation & Error Handling](#validation--error-handling)
8. [Auditing](#auditing)

---

## Project Overview

This application provides a complete task management solution with the following features:

- **Create, Read, Update, Delete (CRUD)** operations for tasks
- **Partial updates** via PATCH endpoint (update only status or priority)
- **Search** tasks by title (case-insensitive)
- **Filter** tasks by status and/or priority
- **Automatic timestamps** for creation and modification
- **Input validation** with meaningful error messages
- **Database migrations** using Flyway

### Task Properties

| Field       | Type          | Description                              |
|-------------|---------------|------------------------------------------|
| id          | Long          | Auto-generated unique identifier         |
| title       | String        | Task title (required, max 150 chars)     |
| description | String        | Optional description (max 1000 chars)    |
| status      | Enum          | `TODO`, `IN_PROGRESS`, `DONE`            |
| priority    | Enum          | `LOW`, `MEDIUM`, `HIGH`                  |
| dueDate     | LocalDate     | Optional due date                        |
| createdAt   | LocalDateTime | Auto-set on creation                     |
| updatedAt   | LocalDateTime | Auto-updated on modification             |

---

## Technologies Used

| Technology        | Version | Purpose                          |
|-------------------|---------|----------------------------------|
| Java              | 17      | Programming language             |
| Spring Boot       | 3.2.x   | Application framework            |
| Spring Security   | -       | Authentication and authorization |
| JJWT              | 0.12.x  | JWT creation and validation      |
| Spring Data JPA   | -       | Database persistence             |
| PostgreSQL        | 15+     | Relational database              |
| Flyway            | -       | Database migrations              |
| Lombok            | -       | Boilerplate code reduction       |
| Bean Validation   | -       | Input validation                 |
| Maven             | 3.6+    | Build and dependency management  |

---

## How to Run the Project

### Prerequisites

- JDK 17 or higher
- Maven 3.6 or higher
- PostgreSQL 15 or higher

### Step-by-Step Instructions

1. **Clone the repository**

   ```bash
   git clone <repository-url>
   cd task-manager
   ```

2. **Create the PostgreSQL database**

   ```sql
   CREATE DATABASE task_manager;
   ```

3. **Configure database credentials**

   Edit `src/main/resources/application.properties` (or set the `DB_PASSWORD` environment variable):

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/task_manager
   spring.datasource.username=postgres
   spring.datasource.password=${DB_PASSWORD:postgres1234!}
   ```

4. **Build the project**

   ```bash
   mvn clean install
   ```

5. **Run the application**

   ```bash
   mvn spring-boot:run
   ```

6. **Access the application**

   - **Web UI:** http://localhost:8081
   - **API Base URL:** http://localhost:8081/api/tasks

---

## Database Configuration

The application uses PostgreSQL with Flyway for schema management.

### application.properties

```properties
# Server
server.port=8081

# PostgreSQL (use DB_PASSWORD env var in production)
spring.datasource.url=jdbc:postgresql://localhost:5432/task_manager
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD:postgres1234!}

# Flyway migrations
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
spring.flyway.locations=classpath:db/migration

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
```

For local development, you can enable SQL logging by activating the `dev` profile (e.g. `-Dspring.profiles.active=dev`). That loads `application-dev.properties`, which sets `spring.jpa.show-sql=true`.

### Schema Migrations

Flyway automatically runs migrations from `src/main/resources/db/migration/` on startup. The `tasks` table is created with the following structure:

| Column      | Type                     | Constraints                |
|-------------|--------------------------|----------------------------|
| id          | BIGSERIAL                | PRIMARY KEY                |
| title       | VARCHAR(150)             | NOT NULL                   |
| description | VARCHAR(1000)            | -                          |
| status      | VARCHAR(20)              | NOT NULL, DEFAULT 'TODO'   |
| priority    | VARCHAR(20)              | NOT NULL, DEFAULT 'MEDIUM' |
| due_date    | DATE                     | -                          |
| created_at  | TIMESTAMP                | NOT NULL, DEFAULT NOW()    |
| updated_at  | TIMESTAMP                | NOT NULL, DEFAULT NOW()    |

---

## Authentication

The API uses **JWT (JSON Web Token)** for authentication. All task endpoints under `/api/tasks/**` require a valid token. Public endpoints are used to obtain a token.

### Register

Create a new user account.

```
POST /api/auth/register
Content-Type: application/json
```

**Request Body:**

```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "secret123"
}
```

**Validation:** `username` (2–50 chars), `email` (valid format), `password` (min 6 chars).

**Response:** `201 Created`

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "johndoe"
}
```

**Error:** `409 Conflict` if username or email is already taken.

---

### Login

Authenticate and receive a JWT.

```
POST /api/auth/login
Content-Type: application/json
```

**Request Body:**

```json
{
  "username": "johndoe",
  "password": "secret123"
}
```

**Response:** `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "johndoe"
}
```

**Error:** `401 Unauthorized` for invalid username or password.

---

### Using the Token

Send the token in the `Authorization` header for all task API requests:

```
Authorization: Bearer <your_token>
```

Example:

```
GET /api/tasks
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Without a valid token, requests to `/api/tasks/**` return `401 Unauthorized` or `403 Forbidden`.

### Configuration

JWT is configured in `application.properties` (or environment variables):

- `app.jwt.secret` — signing key (min 32 characters for HS256). Override with `JWT_SECRET`.
- `app.jwt.expiration-ms` — token lifetime in milliseconds. Override with `JWT_EXPIRATION_MS`.

---

## API Documentation

Base URL: `http://localhost:8081/api/tasks`

All task endpoints require the `Authorization: Bearer <token>` header.

### Get All Tasks

```
GET /api/tasks
```

**Response:** `200 OK`

```json
[
  {
    "id": 1,
    "title": "Complete project report",
    "description": "Write the final report for the semester project",
    "status": "IN_PROGRESS",
    "priority": "HIGH",
    "dueDate": "2024-12-15",
    "createdAt": "2024-11-01T10:30:00",
    "updatedAt": "2024-11-10T14:45:00"
  }
]
```

---

### Get Task by ID

```
GET /api/tasks/{id}
```

**Response:** `200 OK`

```json
{
  "id": 1,
  "title": "Complete project report",
  "description": "Write the final report for the semester project",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "dueDate": "2024-12-15",
  "createdAt": "2024-11-01T10:30:00",
  "updatedAt": "2024-11-10T14:45:00"
}
```

**Error Response:** `404 Not Found`

```json
{
  "timestamp": "2024-11-15T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Task not found with id: 999"
}
```

---

### Create Task

```
POST /api/tasks
Content-Type: application/json
```

**Request Body:**

```json
{
  "title": "Study for exams",
  "description": "Review chapters 1-5",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2024-12-20"
}
```

**Response:** `201 Created`

```json
{
  "id": 2,
  "title": "Study for exams",
  "description": "Review chapters 1-5",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2024-12-20",
  "createdAt": "2024-11-15T09:00:00",
  "updatedAt": "2024-11-15T09:00:00"
}
```

**Notes:**
- `title` is required (cannot be blank)
- `status` defaults to `TODO` if not provided
- `priority` defaults to `MEDIUM` if not provided

---

### Update Task (Full Update)

```
PUT /api/tasks/{id}
Content-Type: application/json
```

**Request Body:**

```json
{
  "title": "Study for final exams",
  "description": "Review all chapters",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "dueDate": "2024-12-18"
}
```

**Response:** `200 OK`

```json
{
  "id": 2,
  "title": "Study for final exams",
  "description": "Review all chapters",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "dueDate": "2024-12-18",
  "createdAt": "2024-11-15T09:00:00",
  "updatedAt": "2024-11-15T10:30:00"
}
```

---

### Patch Task (Partial Update)

```
PATCH /api/tasks/{id}
Content-Type: application/json
```

Use PATCH to update only `status` and/or `priority` without providing all fields.

**Request Body (update status only):**

```json
{
  "status": "DONE"
}
```

**Request Body (update priority only):**

```json
{
  "priority": "LOW"
}
```

**Request Body (update both):**

```json
{
  "status": "IN_PROGRESS",
  "priority": "MEDIUM"
}
```

**Response:** `200 OK`

```json
{
  "id": 2,
  "title": "Study for final exams",
  "description": "Review all chapters",
  "status": "DONE",
  "priority": "LOW",
  "dueDate": "2024-12-18",
  "createdAt": "2024-11-15T09:00:00",
  "updatedAt": "2024-11-15T11:00:00"
}
```

---

### Delete Task

```
DELETE /api/tasks/{id}
```

**Response:** `204 No Content`

---

### Search Tasks by Title

```
GET /api/tasks/search?title={searchTerm}
```

Performs a case-insensitive partial match on task titles.

**Example:**

```
GET /api/tasks/search?title=exam
```

**Response:** `200 OK`

```json
[
  {
    "id": 2,
    "title": "Study for final exams",
    "description": "Review all chapters",
    "status": "DONE",
    "priority": "LOW",
    "dueDate": "2024-12-18",
    "createdAt": "2024-11-15T09:00:00",
    "updatedAt": "2024-11-15T11:00:00"
  }
]
```

---

### Filter Tasks

```
GET /api/tasks/filter?status={status}&priority={priority}
```

Both parameters are optional. Uses AND logic when both are provided.

**Filter by status only:**

```
GET /api/tasks/filter?status=TODO
```

**Filter by priority only:**

```
GET /api/tasks/filter?priority=HIGH
```

**Filter by both (AND logic):**

```
GET /api/tasks/filter?status=TODO&priority=HIGH
```

**Valid Values:**
- `status`: `TODO`, `IN_PROGRESS`, `DONE`
- `priority`: `LOW`, `MEDIUM`, `HIGH`

**Response:** `200 OK`

```json
[
  {
    "id": 3,
    "title": "Submit assignment",
    "description": null,
    "status": "TODO",
    "priority": "HIGH",
    "dueDate": "2024-12-01",
    "createdAt": "2024-11-14T08:00:00",
    "updatedAt": "2024-11-14T08:00:00"
  }
]
```

---

## Validation & Error Handling

### Validation Rules

| Field       | Rule                                      |
|-------------|-------------------------------------------|
| title       | Required, not blank, max 150 characters   |
| description | Optional, max 1000 characters             |
| status      | Must be `TODO`, `IN_PROGRESS`, or `DONE`  |
| priority    | Must be `LOW`, `MEDIUM`, or `HIGH`        |
| dueDate     | Optional, format `YYYY-MM-DD`             |

### Error Responses

**Validation Error (400 Bad Request):**

```json
{
  "timestamp": "2024-11-15T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "title",
      "message": "must not be blank"
    }
  ]
}
```

**Resource Not Found (404 Not Found):**

```json
{
  "timestamp": "2024-11-15T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Task not found with id: 999"
}
```

---

## Auditing

The application automatically tracks when tasks are created and modified using Spring Data JPA Auditing.

### How It Works

- **`createdAt`**: Set automatically when a task is first saved. Never changes after creation.
- **`updatedAt`**: Set automatically on creation and updated every time the task is modified.

### Implementation

The `Task` entity uses the following annotations:

```java
@EntityListeners(AuditingEntityListener.class)
public class Task {
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

JPA Auditing is enabled in the main application class:

```java
@SpringBootApplication
@EnableJpaAuditing
public class TaskManagerApplication {
    // ...
}
```

---

## Project Structure

```
src/main/java/com/taskmanager/
├── TaskManagerApplication.java      # Application entry point
├── controller/
│   ├── TaskController.java          # REST endpoints
│   └── dto/
│       ├── TaskRequest.java         # Create/Update request DTO
│       ├── TaskUpdateRequest.java   # Patch request DTO
│       └── TaskResponse.java        # Response DTO
├── service/
│   ├── TaskService.java             # Service interface
│   └── TaskServiceImpl.java         # Service implementation
├── repository/
│   └── TaskRepository.java          # Spring Data JPA repository
├── mapper/
│   └── TaskMapper.java              # Entity ↔ DTO mapping
├── model/
│   ├── Task.java                    # JPA entity
│   ├── TaskStatus.java              # Status enum
│   └── TaskPriority.java            # Priority enum
└── exception/
    ├── ResourceNotFoundException.java
    └── GlobalExceptionHandler.java

src/main/resources/
├── application.properties           # Application configuration
├── db/migration/                    # Flyway migrations
└── static/                          # Frontend files
    ├── index.html
    ├── css/style.css
    └── js/app.js
```

---

## License

This project was developed for educational purposes as part of a university course.
