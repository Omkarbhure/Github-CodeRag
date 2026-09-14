# Project Status: GitHub CodeRAG

## Completed Phases

### [x] Phase 0: Dual Authentication & Base Monorepo
- Email/Password signup & login issuing HttpOnly JWT cookie.
- GitHub OAuth2 integration issuing internal JWT cookie and redirecting to `/dashboard`.
- Spring Security 6 stateless filter chain & Next.js App Router context.
- PostgreSQL 16 + Flyway V1 schema.

### [x] Phase 1: GitHub Repository Integration
- Flyway Migration `V2__init_repository_schema.sql` (`github_repositories`, `indexing_jobs`, `repository_files`).
- JPA Entities: `GitHubRepository`, `RepositoryFile`, `IndexingJob`, `IndexingStatus`.
- GitHub REST API client for metadata & commit SHA with rate limiting & error handling.
- Whole-repo cap enforcement: 100MB pre-check via GitHub API + streaming byte guard on zip download.
- Per-file cap enforcement: 10MB limit during tree walk (recorded as skipped with reason).
- Zip Slip protection and directory extraction to `./data/repos/{repositoryId}`.
- Source file filtering (.java, .ts, .py, .go, etc.) and directory/binary exclusion (.git, node_modules, target, etc.).
- Same-commit re-indexing detection (skips re-downloading).
- REST endpoints: `POST /api/repositories`, `GET /api/repositories`, `GET /api/repositories/{id}`, `GET /api/repositories/{id}/files`.
- Dashboard UI: Repository import form, active job status polling, and paginated file explorer with skip reasons.
- Backend unit & integration tests passing.

### [x] Phase 2: Code Processing (Chunking) & Low-Value Detection
- Flyway Migration `V3__init_chunking_schema.sql` (`code_chunks` table with indexes on `repository_id` and `repository_file_id`).
- JPA Entity: `CodeChunk` mapping chunk index, 1-indexed line ranges (`start_line`, `end_line`), commit SHA, and chunk text.
- `LowValueDetectorService`: Multi-heuristic detector identifying minified files (max line length > 1000, avg line length > 300, newline ratio < 0.005) and auto-generated code (`do not edit`, `auto-generated`, `@generated`, etc.), marking files with explicit skip reasons.
- `ChunkerService`: Sliding line-window chunker (120-line window, 20-line overlap, single chunk for $\le 120$ lines) with clear-and-rechunk idempotency.
- Extended `IndexingStatus` with `CHUNKING` stage (`PENDING -> DOWNLOADING -> SCANNING -> CHUNKING -> COMPLETED`).
- REST Endpoints: `GET /api/repositories/{id}/chunks` (paginated, `includeContent` param) and updated `GET /api/repositories/{id}` with `totalChunks` and `lowValueSkippedCount`.
- Frontend UI: Code Chunks explorer with interactive code preview toggle, chunk counts, and low-value skip indicators.
- 30/30 Backend unit & integration tests passing.
- Frontend Next.js production build clean.

### [x] Phase 3: Vector Search (Gemini Embeddings + Qdrant)
- `docker-compose.yml`: Added `qdrant` vector database container (ports 6333 REST / 6334 gRPC).
- `EmbeddingService` & `GeminiEmbeddingService`: Plain HTTP RestClient targeting Google Gemini `text-embedding-004` (768-dim) with 429 exponential backoff retry and 401/403 fail-fast authentication error handling.
- `VectorStoreService` & `QdrantVectorStoreService`: Qdrant REST client managing collection `"code_chunks"` (768-dim, Cosine distance), keyword index on `repositoryId`, CodeChunk UUID point IDs, and payload preservation (`filePath`, `lines`, `chunkIndex`, `content`).
- `IndexingStatus`: Extended with `EMBEDDING` stage (`PENDING -> DOWNLOADING -> SCANNING -> CHUNKING -> EMBEDDING -> COMPLETED`).
- REST Endpoint: `POST /api/repositories/{id}/search` for semantic code search, taking `{ query, topK }` and returning ranked `SearchResultDto` objects.
- Frontend UI: Added interactive **Semantic Search** tab on repo detail page with suggestion pills, Top-K filter, ranked similarity matches, and formatted code snippets.
- 36/36 Backend unit & integration tests passing.
- Frontend Next.js production build clean.

### [x] Phase 4: Core RAG Chat (LLM Answer Generation & Grounded Citations)
- Flyway Migration `V4__init_chat_schema.sql` (`conversations` and `messages` tables with user and repository relationships).
- JPA Entities: `Conversation`, `Message`, `MessageRole` (`USER`, `ASSISTANT`).
- `ContextBuilder`: Top-K (default top-8, budget 8,000 chars) context assembler formatting code chunks with strict `[filePath:startLine-endLine]` header delimiters.
- `AnswerGenerationService` & `GeminiAnswerService`: Grounded code question answering using Google Gemini (`gemini-1.5-flash`), with system prompts enforcing no hallucinations, code grounding, and mandatory inline citations `[filePath:startLine-endLine]`.
- `ChatService`: End-to-end RAG orchestrator performing user authentication verification, conversation title auto-generation, vector embedding retrieval via Qdrant, LLM answer synthesis, and transactional chat message persistence.
- REST Endpoints:
  - `POST /api/repositories/{id}/conversations`: Start conversation thread.
  - `GET /api/repositories/{id}/conversations`: List user threads for a repo.
  - `GET /api/conversations/{id}/messages`: Retrieve message history.
  - `POST /api/conversations/{id}/messages`: Submit prompt and receive grounded assistant answer.
- 45/45 Backend unit & integration tests passing.
- Frontend Next.js production build clean.

### [x] Phase 5: Frontend Polish (Dashboard, Split-View Chat + Monaco Code Viewer)
- Backend additions:
  - `GET /api/repositories/{id}/files/content?path={filePath}` endpoint reading raw on-disk file text with path traversal protection and 404 validation.
  - `RepositoryFileRepository.findByRepositoryIdAndFilePath` query method.
  - 46/46 backend unit & integration tests passing.
- Frontend split-view chat and code inspection workspace at `/repositories/[id]/chat`:
  - **Left Pane (`ChatPanel`)**: Conversation threads selector, message history stream, thinking state indicator, prompt suggestion pills, and markdown code block rendering with one-click copy.
  - **Citation Chip (`CitationChip`)**: Parses `[filePath:startLine-endLine]` regex into interactive badges showing `fileName:startLine-endLine` with tooltip on hover.
  - **Right Pane (`CodeViewerPanel`)**: Integrated `@monaco-editor/react` (read-only) with dynamic language syntax highlighting, automatic scroll to line, and highlighted line range decorations (`editor.deltaDecorations`).
  - Mobile responsive stacking / tab switcher between Chat and Code Viewer.
- **Dashboard Polish**:
  - Enhanced status badges for all states (`PENDING`, `DOWNLOADING`, `SCANNING`, `CHUNKING`, `EMBEDDING`, `COMPLETED`, `FAILED`, `REJECTED_TOO_LARGE`).
  - Direct **"Open Chat"** primary action on completed repositories.
- **Repository Overview Page**:
  - Kept as clean inspection view (`/repositories/[id]`) for files, code chunks, and vector similarity search, with launch banner linking to the split-view chat.
- Clean Next.js production build (`npm run build`).

### [x] Phase 6: Hybrid Search & Retrieval Quality (Vector + Keyword Fusion)
- Flyway Migration `V5__init_keyword_search_schema.sql` (`content_tsv` generated tsvector column + GIN index on `code_chunks`).
- `CodeChunkSearchProjection` & `CodeChunkRepository.searchByKeyword`: Scoped PostgreSQL full-text search using `plainto_tsquery('english', :query)` and `ts_rank`.
- `KeywordSearchService`: Graceful full-text query executor with documented code tokenization handling.
- `HybridSearchService`: Parallel retrieval orchestrator querying Qdrant vector store and PostgreSQL keyword search concurrently via `CompletableFuture`.
- Score Normalization & Fusion: Min-max score scaling per modality + weighted linear combination (`finalScore = vectorWeight * normVec + keywordWeight * normKw`), defaulting to 0.7 / 0.3 configurable via `HYBRID_VECTOR_WEIGHT` and `HYBRID_KEYWORD_WEIGHT`.
- Diagnostic Fields: Enriched `SearchResultDto` with `vectorScore`, `keywordScore`, and `finalScore` for tuning.
- Pipeline Integration: Integrated hybrid search into `RepositoryService.searchRepository` (`POST /api/repositories/{id}/search`) and `ChatService.sendMessage` (context retrieval).
- 56/56 backend unit & integration tests passing.
- Frontend Next.js production build clean.

### [x] Phase 7: Developer Intelligence Features
- Flyway Migration `V6__init_intelligence_schema.sql` (`architecture_overviews` table with unique index on `(repository_id, commit_sha)`).
- JPA Entity & Repository: `ArchitectureOverview`, `ArchitectureOverviewRepository`.
- **Architecture Overview (`POST /api/repositories/{id}/architecture-overview?force=true`)**:
  - Deterministic representative sampling across manifests/build files, READMEs, entry points, and directory hierarchy (capped at ~18 files / 14k characters).
  - LLM synthesis via Gemini generating summary, key components, data flow, architecture patterns, and structured technology/module tags.
  - Commit-keyed caching in PostgreSQL (`repositoryId` + `commitSha`) with force re-generation bypass.
- **Bug Investigation (`POST /api/repositories/{id}/investigate-bug`)**:
  - Stack trace parsing extracting structured signals (Java, Python, JS/TS, and generic `file:line` formats) with deduplication.
  - Multi-phase retrieval: prioritized exact chunk lookup by file path & line range + fallback hybrid search for contextual references.
  - Strict grounding prompt with insufficient-context detection guardrail and mandatory `[filePath:startLine-endLine]` citations.
- **Related Files (`GET /api/repositories/{id}/files/related?path={filePath}&topK=10`)**:
  - Qdrant vector similarity across other files in the repository.
  - Symbol/import substring boost heuristic (+0.35 score boost capped at 1.0) detecting references to the target class/module.
  - File-level score aggregation, deduplication, and reason generation.
- **Frontend Workspace Integration**:
  - Repository Detail Page (`/repositories/[id]`): Added dedicated **Architecture Overview** and **Bug Investigation** tabs with structured outputs, tech tag badges, and interactive clickable citations.
  - Monaco Split-View Chat (`/repositories/[id]/chat` & `CodeViewerPanel`): Added interactive **Related Files** slide-out drawer with similarity badges, reference indicators, and one-click file navigation.
  - Direct deep linking: Citations from Bug Investigation and Architecture Overview navigate straight to Monaco viewer with exact line range highlighted.
- 64/64 backend unit & integration tests passing.
- Frontend Next.js production build clean.

### [x] Phase 8: Production Hardening & Deployment
- **Non-Blocking Asynchronous Indexing**:
  - Configured dedicated Spring `@Async` task executor `indexingTaskExecutor` (core pool 2, max pool 3, queue 50).
  - Implemented `AsyncIndexingService` handling background repository download, scanning, chunking, and embedding.
  - `POST /api/repositories` returns `202 Accepted` immediately with `PENDING` status for new imports, and `200 OK` if already indexed.
  - Startup orphan job recovery (`StartupOrphanJobCleaner`) automatically transitions any interrupted non-terminal jobs on restart to `FAILED` with message `"Interrupted by server restart — please re-import"`.
- **In-Memory Rate Limiting & Abuse Protection**:
  - `RateLimiterService`: Sliding-window in-memory rate limiter per user/action (5 imports/hr, 30 chat msgs/hr, 15 intelligence queries/hr).
  - Returns `429 Too Many Requests` with `Retry-After` header when limit is exceeded.
  - `GeminiQuotaManager`: Global daily API quota guardrail (default 1,500 calls/day for Free Tier) with automatic UTC midnight counter reset.
- **Production Configuration & Health Checks**:
  - `GET /api/health` unauthenticated probe returning service status, uptime, and version.
  - Multi-origin CORS support via `ALLOWED_ORIGINS` / `app.cors.allowed-origins`.
  - Comprehensive externalized configuration in `application.yml` supporting cloud database URLs (`SPRING_DATASOURCE_URL` / `DATABASE_URL`).
- **Containerization & Deployment Guide**:
  - Production multi-stage `backend/Dockerfile` (Maven 3.9 + Temurin JDK 17 $\to$ Temurin JRE 17 slim, non-root user).
  - Production multi-stage `frontend/Dockerfile` (Next.js standalone output mode, Node 20 alpine).
  - `DEPLOYMENT.md`: Step-by-step production deployment documentation for Render (Backend + Frontend + Managed Postgres) and Qdrant Cloud.
- **Frontend Active Job Polling**:
  - Real-time polling on repository detail page during non-terminal indexing states (`PENDING`, `DOWNLOADING`, `SCANNING`, `CHUNKING`, `EMBEDDING`).
- 76/76 backend unit & integration tests passing.
- Frontend Next.js production build clean.

### [x] CI/CD Pipeline & Branch Protection Gate
- **GitHub Actions Workflow (`.github/workflows/ci.yml`)**:
  - Triggers on `pull_request` targeting `main` and `push` to `main`.
  - Parallel **`backend-ci`** job: Java 17 Temurin, Maven dependency caching, runs `mvn -B test` (76 unit & integration tests against in-memory H2 database, zero external credentials needed).
  - Parallel **`frontend-ci`** job: Node 20, npm caching, `npm ci`, ESLint verification (`next lint`), and Next.js standalone build (`npm run build`).
- **ESLint Integration**: Added `.eslintrc.json` configured with `next/core-web-vitals` with 0 warnings/errors.
- **Branch Protection Documentation (`CI.md`)**:
  - Step-by-step instructions for enforcing status checks (`Backend CI`, `Frontend CI`) and requiring pull requests before merging to `main`, safeguarding Render auto-deployments from breaking changes.

### [x] Session Management & Navigation Flow
- **Public Landing Page (`/`)**:
  - Hero section with value proposition, tagline, and dual CTA buttons.
  - Interactive Preview section featuring tabs for Split-View Monaco Chat, Architecture Overview, and Bug Investigation with clearly marked screenshot placeholders (`/screenshots/*.png`).
  - Core Capabilities feature grid highlighting Hybrid Search, Grounded Citations, Architecture Synthesis, and Root Cause Fixes.
  - Footer with tech stack notes, navigation links, and GitHub repository link.
  - Dynamic CTA: Displays "Go to Dashboard" if the user is authenticated, without forced auto-redirect.
- **Two-Tier Route Protection**:
  - Edge `middleware.ts`: Intercepts `/dashboard/:path*` and `/repositories/:path*`, checking for `coderag_token` cookie presence; redirects unauthenticated requests immediately to `/login` with zero UI flash.
  - Client-side `AuthGuard` & `useAuth`: Calls `GET /api/me` on mount to verify session validity and redirects to `/login` if unauthenticated or expired.
- **Session Lifecycle & Global 401 Handling**:
  - `apiFetch` in `frontend/src/lib/api.ts` intercepts mid-session 401s on protected endpoints and cleanly routes the user to `/login`.
  - Logout clears the session cookie and reliably redirects back to `/` (landing page).
- **Quality & Verification**:
  - Next.js build passes cleanly (`npm run build`).
  - ESLint verification with 0 errors / warnings (`npm run lint`).
  - 76/76 backend unit & integration tests passing (`mvn test`).

## All Phases & CI/CD Completed!
- Dual Auth & Monorepo (Phase 0)
- Repository Sync & Indexing Pipeline (Phase 1)
- Code Chunking & Low-Value Detection (Phase 2)
- Vector Embeddings & Qdrant Search (Phase 3)
- Grounded RAG Chat & Citations (Phase 4)
- Split-View Chat + Monaco Editor (Phase 5)
- Hybrid Vector + Keyword Search Fusion (Phase 6)
- Developer Intelligence Features (Phase 7)
- Production Hardening & Deployment (Phase 8)
- CI/CD Pipeline & Branch Protection
- Session Management & Landing Page Navigation Flow


