# GitHub CodeRAG - Memory & Architecture Notes

## Overview
GitHub CodeRAG is a modular monolith (Spring Boot 3 + Next.js App Router + PostgreSQL 16) designed to index, chunk, and perform Retrieval-Augmented Generation (RAG) over public GitHub repositories.

## System Architecture

```text
repopilot/
├── docker-compose.yml              # PostgreSQL 16 + Local Dev Services
├── MEMORY.md                       # Architectural memory & conventions
├── STATUS.md                       # Project phase tracking & task state
├── backend/                        # Spring Boot 3.3.4 (Java 17/25 compatible)
│   ├── config/                     # Security, CORS, Cookie, GitHubConfig
│   ├── controller/                 # AuthController, UserController, RepositoryController
│   ├── dto/                        # Request / Response payloads & pagination
│   ├── exception/                  # Custom exceptions & GlobalExceptionHandler
│   ├── github/                     # GitHubApiClient, ZipDownloadService, FileFilterService
│   ├── model/                      # User, GitHubRepository, RepositoryFile, IndexingJob
│   ├── repository/                 # Spring Data JPA repositories
│   ├── security/                   # JwtService, CookieService, JwtAuthenticationFilter
│   └── service/                    # AuthService, UserService, RepositoryService, IndexingService
└── frontend/                       # Next.js 14 (App Router), React 18, Tailwind, TypeScript
    └── src/
        ├── app/                    # /login, /signup, /dashboard, /repositories/[id]
        ├── context/                # AuthContext (session via /api/me)
        ├── lib/                    # api.ts fetch client (credentials: include)
        └── types/                  # auth.ts, repository.ts
```

## Phase Status Summary
- **Phase 0 (Completed)**: Dual Authentication (Email/Password + GitHub OAuth2 login -> HttpOnly JWT cookie session), clean JSON GlobalExceptionHandler, Flyway migrations (`V1`).
- **Phase 1 (In Planning)**: GitHub Repository Integration. Public URL import, GitHub REST API validation & size checks, streaming zip download with size caps, file filtering, on-disk storage under `/data/repos/{id}`, and database persistence (`GitHubRepository`, `RepositoryFile`, `IndexingJob`).
- **Phase 2 (Upcoming)**: Code Processing & Chunking.
- **Phase 3 (Upcoming)**: Vector Embeddings & Qdrant integration.
- **Phase 4 (Upcoming)**: Code Search & RAG Chat.

## Key Technical Conventions & Invariants
1. **Session Auth**: All authenticated API calls use `credentials: 'include'` with backend `coderag_token` HttpOnly cookie evaluated by `JwtAuthenticationFilter`.
2. **File Storage**: Raw repo source files reside on disk under `${app.storage.repo-base-path:/data/repos}/{repositoryId}`. Database only stores metadata in `repository_files`.
3. **Caps & Safety**:
   - `MAX_REPO_SIZE_MB`: 100 MB (pre-checked via GitHub API + streaming limit on zip download).
   - `MAX_FILE_SIZE_MB`: 10 MB (files over limit marked `skipped = true` with reason).
4. **Clean Code**: Standard Java POJOs/Records, constructor injection, strict error handling returning `ApiErrorResponse`.
