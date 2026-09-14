# Deployment Guide: GitHub CodeRAG

This guide details how to deploy **GitHub CodeRAG** (Next.js Frontend, Spring Boot Backend, PostgreSQL, Qdrant Cloud, and Google Gemini) to a production free/cheap hosting platform such as **Render** (or **Railway** / **Fly.io**).

---

## 1. Production Architecture

```mermaid
graph TD
    User["End User Browser"]
    Frontend["Next.js Frontend\n(Render / Vercel Web Service)"]
    Backend["Spring Boot Backend\n(Render Docker Web Service)"]
    Postgres[("PostgreSQL 16\n(Render Managed Database)")]
    Qdrant[("Qdrant Cloud\n(Free Tier Cluster: 1GB)")]
    Gemini["Google Gemini AI\n(Embeddings + Flash Chat)"]
    GitHub["GitHub API / OAuth2\n(Authentication & Repo Sync)"]

    User -->|HTTPS| Frontend
    Frontend -->|API / Proxy & JWT Cookie| Backend
    Backend -->|JDBC / Flyway| Postgres
    Backend -->|REST API / 768-dim Vectors| Qdrant
    Backend -->|REST API / Backoff & Quota| Gemini
    Backend -->|REST API / Token Auth| GitHub
```

---

## 2. Prerequisites & Free Tier Accounts

1. **GitHub Account**:
   - Create a GitHub OAuth App at [GitHub Developer Settings](https://github.com/settings/developers).
2. **Google AI Studio Account**:
   - Obtain a free API key at [Google AI Studio](https://aistudio.google.com/).
3. **Qdrant Cloud Account**:
   - Create a free 1GB cluster at [cloud.qdrant.io](https://cloud.qdrant.io/).
   - Copy your **Cluster URL** (e.g., `https://xyz-example.qdrant.tech:6333`) and **API Key** (if enabled).
4. **Render Account**:
   - Sign up at [render.com](https://render.com/).

---

## 3. Step-by-Step Deployment on Render

### Step A: Deploy Managed PostgreSQL Database
1. In Render Dashboard, click **New +** $\to$ **PostgreSQL**.
2. **Name**: `coderag-postgres`
3. **Database**: `coderag`
4. **User**: `coderag_user`
5. **Plan**: **Free**.
6. Click **Create Database**.
7. Once created, copy the **Internal Database URL** (e.g. `postgresql://coderag_user:pass@dpg-xxx:5432/coderag`).

---

### Step B: Setup GitHub OAuth App
1. Go to [GitHub Settings -> Developer Settings -> OAuth Apps -> New OAuth App](https://github.com/settings/applications/new).
2. **Application Name**: `GitHub CodeRAG`
3. **Homepage URL**: Your frontend domain (e.g., `https://coderag-ui.onrender.com` or `http://localhost:3000` during dev).
4. **Authorization Callback URL**: Your backend URL + `/login/oauth2/code/github`
   - e.g., `https://coderag-api.onrender.com/login/oauth2/code/github`
5. Click **Register Application**, then generate a **Client Secret**.

---

### Step C: Deploy Backend (Spring Boot Web Service)
1. In Render Dashboard, click **New +** $\to$ **Web Service**.
2. Connect your GitHub repository.
3. Configure the service:
   - **Name**: `coderag-api`
   - **Region**: Same as PostgreSQL (e.g. Frankfurt, Ohio, Oregon).
   - **Root Directory**: `backend`
   - **Runtime**: **Docker** (Render will automatically detect `backend/Dockerfile`).
   - **Instance Type**: **Free** (or Starter).
4. In the **Environment Variables** section, add:

| Variable Name | Description | Example / Recommended Value |
| :--- | :--- | :--- |
| `PORT` | HTTP port | `8081` |
| `SPRING_DATASOURCE_URL` | Render Postgres JDBC URL | `jdbc:postgresql://dpg-xxx:5432/coderag` *(prefix with `jdbc:` if using Internal URL)* |
| `DB_USER` | Postgres Username | `coderag_user` |
| `DB_PASSWORD` | Postgres Password | `(from Render PostgreSQL)` |
| `JWT_SECRET` | 256-bit Hex Key for signing JWTs | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` *(generate custom in prod)* |
| `JWT_EXPIRATION` | Token expiry in seconds | `604800` *(7 days)* |
| `FRONTEND_URL` | Public frontend URL | `https://coderag-ui.onrender.com` |
| `ALLOWED_ORIGINS` | Comma-separated CORS origins | `https://coderag-ui.onrender.com,http://localhost:3000` |
| `COOKIE_SECURE` | Enable HTTPS-only cookies | `true` |
| `COOKIE_SAME_SITE` | Cookie SameSite policy | `None` *(for cross-domain frontend/backend) or `Lax`* |
| `GITHUB_CLIENT_ID` | GitHub OAuth Client ID | `(from GitHub OAuth App)` |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth Client Secret | `(from GitHub OAuth App)` |
| `GITHUB_API_TOKEN` | Optional GitHub Personal Access Token | `ghp_...` *(increases API rate limits from 60 to 5000/hr)* |
| `GEMINI_API_KEY` | Google Gemini API Key | `AIzaSy...` |
| `EMBEDDING_MODEL` | Gemini embedding model | `text-embedding-004` |
| `CHAT_MODEL` | Gemini generation model | `gemini-1.5-flash` |
| `GEMINI_DAILY_QUOTA` | Global daily quota safety guardrail | `1500` |
| `GEMINI_QUOTA_ENABLED` | Enable daily quota tracking | `true` |
| `QDRANT_URL` | Qdrant Cloud cluster endpoint | `https://xyz-example.qdrant.tech:6333` |
| `QDRANT_COLLECTION` | Qdrant vector collection name | `code_chunks` |
| `RATE_LIMIT_IMPORT_PER_HOUR` | Per-user repo import limit | `5` |
| `RATE_LIMIT_CHAT_PER_HOUR` | Per-user chat messages limit | `30` |
| `RATE_LIMIT_INTELLIGENCE_PER_HOUR` | Per-user intelligence limit | `15` |
| `INDEXING_CORE_POOL_SIZE` | Concurrent indexing thread count | `2` |
| `INDEXING_MAX_POOL_SIZE` | Max indexing thread count | `3` |

5. Click **Create Web Service**. Render will build the Docker container and start the backend.

---

### Step D: Deploy Frontend (Next.js Web Service)
1. In Render Dashboard, click **New +** $\to$ **Web Service** (or deploy on **Vercel**).
2. Connect your GitHub repository.
3. Configure the service:
   - **Name**: `coderag-ui`
   - **Root Directory**: `frontend`
   - **Runtime**: **Docker** (uses `frontend/Dockerfile` with standalone output).
   - **Instance Type**: **Free**.
4. In **Environment Variables**, add:

| Variable Name | Description | Value |
| :--- | :--- | :--- |
| `NEXT_PUBLIC_API_URL` | Public backend API URL | `https://coderag-api.onrender.com` |
| `NODE_ENV` | Environment mode | `production` |

5. Click **Create Web Service**.

---

## 4. Production Smoke Test & Verification Checklist

- [ ] **Health Probe**: Send `GET https://coderag-api.onrender.com/api/health` and verify HTTP 200 `{ "status": "UP", ... }`.
- [ ] **Database Migrations**: Check backend logs to ensure Flyway applied migrations `V1` through `V6` successfully.
- [ ] **User Authentication**:
  - Sign up with email/password and verify redirect to `/dashboard`.
  - Sign in with GitHub OAuth and verify JWT cookie is set.
- [ ] **Asynchronous Repository Import**:
  - Import a public repo (e.g. `https://github.com/octocat/Hello-World` or a sample Spring repo).
  - Verify the server returns HTTP `202 Accepted` immediately without blocking the browser.
  - Watch the UI transition dynamically from `PENDING` $\to$ `DOWNLOADING` $\to$ `SCANNING` $\to$ `CHUNKING` $\to$ `EMBEDDING` $\to$ `COMPLETED`.
- [ ] **Orphan Recovery Test**:
  - If the backend restarts during an indexing job, verify that on next boot the job is automatically transitioned to `FAILED` with message `"Interrupted by server restart — please re-import"`.
- [ ] **Rate Limiting Verification**:
  - Submitting rapid import requests past the 5/hr limit returns HTTP `429 Too Many Requests` with a `Retry-After` header.
- [ ] **Developer Intelligence**:
  - Open Architecture Overview and Bug Investigation to verify Gemini grounded citations and Monaco code viewer deep linking.
