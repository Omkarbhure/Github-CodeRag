export type IndexingStatus =
  | 'PENDING'
  | 'DOWNLOADING'
  | 'SCANNING'
  | 'CHUNKING'
  | 'EMBEDDING'
  | 'COMPLETED'
  | 'FAILED'
  | 'REJECTED_TOO_LARGE';

export interface RepositorySummary {
  id: string;
  owner: string;
  name: string;
  fullName: string;
  defaultBranch: string | null;
  latestCommitSha: string | null;
  url: string;
  sizeKb: number;
  totalFiles: number;
  skippedFiles: number;
  status: IndexingStatus;
  errorMessage: string | null;
  createdAt: string;
  alreadyIndexed?: boolean;
}

export interface RepositoryDetail {
  id: string;
  owner: string;
  name: string;
  fullName: string;
  defaultBranch: string | null;
  latestCommitSha: string | null;
  url: string;
  sizeKb: number;
  totalFiles: number;
  skippedFiles: number;
  keptFiles: number;
  lowValueSkippedCount?: number;
  totalChunks?: number;
  embeddedChunkCount?: number;
  status: IndexingStatus;
  errorMessage: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
}

export interface RepositoryFile {
  id: string;
  repositoryId: string;
  filePath: string;
  language: string | null;
  sizeBytes: number;
  skipped: boolean;
  skipReason: string | null;
  createdAt: string;
}

export interface CodeChunk {
  id: string;
  repositoryId: string;
  repositoryFileId: string;
  filePath: string;
  commitSha: string | null;
  chunkIndex: number;
  startLine: number;
  endLine: number;
  content?: string | null;
  createdAt: string;
}

export interface SearchResult {
  id: string;
  repositoryId: string;
  repositoryFileId?: string;
  filePath: string;
  language?: string | null;
  startLine: number;
  endLine: number;
  chunkIndex: number;
  commitSha?: string | null;
  content: string;
  score: number;
}

export interface Conversation {
  id: string;
  repositoryId: string;
  userId: string;
  title: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Message {
  id: string;
  conversationId: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ArchitectureOverview {
  id?: string;
  repositoryId: string;
  commitSha?: string | null;
  overviewText: string;
  technologies: string[];
  modules: string[];
  createdAt?: string;
}

export interface BugInvestigationResponse {
  analysis: string;
  identifiedFiles: string[];
  relevantChunks: SearchResult[];
}

export interface RelatedFile {
  filePath: string;
  language?: string | null;
  relevanceScore: number;
  reason: string;
}

