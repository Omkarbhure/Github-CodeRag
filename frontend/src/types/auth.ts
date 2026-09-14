export type AuthProvider = 'LOCAL' | 'GITHUB';

export interface User {
  id: string;
  email: string | null;
  githubUsername: string | null;
  authProvider: AuthProvider;
  createdAt: string;
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  timestamp: string;
  validationErrors?: Record<string, string>;
}
