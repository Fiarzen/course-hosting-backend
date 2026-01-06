# Course Hosting Backend

Java Spring Boot backend for hosting educational courses with role based access control, enrollments, progress tracking, and file storage.

## Overview

This repository contains the backend for the "mindleaf" course hosting app. It exposes a REST API for managing courses, lessons, enrollments, and users, and is designed to be consumed by a React frontend deployed on Netlify.

Key features:

- User registration and login (token based authentication)
- Roles: STUDENT, CREATOR, ADMIN
- Course creation and access control (allowlist support)
- Lesson management with optional video and PDF resources
- Enrollments and lesson completion tracking
- Optional AWS S3 storage for uploaded lesson PDFs
- Infrastructure as code with Terraform for AWS

## Tech stack

- Java 17
- Spring Boot (Web MVC, Data JPA, Security)
- PostgreSQL
- AWS SDK v2 for S3
- Maven (with wrapper `mvnw`)
- Terraform for AWS infrastructure

## Project structure

- `courses/` Spring Boot application module
  - `src/main/java/com/jeremy/courses` domain, controllers, security, services
  - `src/main/resources/application.properties` core configuration
  - `.env` optional local overrides imported by Spring
- `terraform/` Infrastructure code for AWS
  - S3 buckets (artifacts and lesson files)
  - RDS PostgreSQL instance
  - EC2 instance that runs the Spring Boot JAR via systemd

## Running locally

### Prerequisites

- Java 17 or higher installed
- Docker and Docker Compose (recommended for local PostgreSQL)

### 1. Start PostgreSQL locally

From the repository root run:

```bash
docker compose up -d
```

This uses the provided `docker-compose.yml` file. It starts a PostgreSQL container that matches the defaults in `courses/src/main/resources/application.properties`.

### 2. Configure local environment (optional)

In `courses/` there is a `.env` file that can override properties in `application.properties`. Spring is configured to import it if present.

Relevant properties and environment variables:

- Database
  - `spring.datasource.url` defaults to `jdbc:postgresql://localhost:5432/coursedb`
  - `spring.datasource.username` defaults to `course_user`
  - `spring.datasource.password` defaults to `course_password`
- Seed users (used by `DataSeeder`)
  - `APP_ADMIN_EMAIL`
  - `APP_ADMIN_PASSWORD`
  - `APP_STUDENT_EMAIL`
  - `APP_STUDENT_PASSWORD`
- S3 integration
  - `AWS_S3_ENABLED` (true or false) controls whether S3 is used
  - `AWS_S3_BUCKET_NAME` name of the S3 bucket for PDFs
  - `AWS_REGION` AWS region, defaults to `eu-west-1`

### 3. Run the Spring Boot app

From `courses/` run:

```bash
./mvnw spring-boot:run
```

The API will start on `http://localhost:8080` by default.

You can also build a JAR and run it manually:

```bash
./mvnw clean package -DskipTests
java -jar target/courses-0.0.1-SNAPSHOT.jar
```

## Authentication and authorization

The backend uses Spring Security with a simple token based scheme.

- `POST /auth/login` accepts JSON `{ "email": "...", "password": "..." }`
- On success it returns `{ "token": "...", "user": { ... } }`
- Clients must send `Authorization: Bearer <token>` on subsequent requests

Roles:

- `STUDENT` default role for registered users
- `CREATOR` can create and manage their own courses and lessons
- `ADMIN` can manage users and has full access

Examples of protected endpoints:

- `GET /users` admin only
- `GET /courses/my-created` creator or admin
- `POST /enrollments/courses/{courseId}` authenticated user only

Public endpoints:

- `GET /courses` list of courses visible to the current user
- `POST /users/register` user registration
- `POST /auth/login` login
- Static files under `/files/**` when using local storage

## File storage

Lesson PDFs can be stored either locally or in S3:

- If `AWS_S3_ENABLED` is `true` and `AWS_S3_BUCKET_NAME` is set, uploads go to S3 and public URLs are generated
- Otherwise, uploads are written under a local `uploads/` directory and served via `/files/**` using `WebConfig`

## API documentation

Springdoc OpenAPI is enabled.

- JSON docs: `GET /v3/api-docs`
- Swagger UI: `GET /swagger-ui.html`

These endpoints are configured as public in `SecurityConfig`.

## Deployment with Terraform on AWS

The `terraform/` directory contains everything needed to deploy the backend on AWS.

High level resources:

- S3 bucket for lesson files (optionally public read for PDFs)
- S3 bucket for artifacts (stores the built Spring Boot JAR)
- RDS PostgreSQL instance
- EC2 instance in the default VPC
- Security groups for HTTP and PostgreSQL
- IAM roles and instance profile for S3 access

Terraform expects a built JAR at the path defined by the `artifact_source_path` variable (by default `../courses/target/courses-0.0.1-SNAPSHOT.jar`).

Typical workflow:

```bash
cd terraform
terraform init
terraform plan
terraform apply
```

The EC2 instance uses `user_data.sh.tpl` to install Java, download the JAR from S3, and start it as a systemd service.

## Useful commands

From `courses/`:

- Run tests

  ```bash
  ./mvnw test
  ```

- Build without tests

  ```bash
  ./mvnw clean package -DskipTests
  ```

From the repository root:

- Start local Docker services

  ```bash
  docker compose up -d
  ```
