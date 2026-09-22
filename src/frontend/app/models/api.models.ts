export interface Credentials {
  username: string;
  password: string;
}

export interface UserProfile {
  id: number;
  username: string;
  role: 'ADMIN' | 'PARTICIPANT';
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
}

export interface CreateListRequest {
  name: string;
}

export interface JoinListRequest {
  code: string;
}

export interface ListMember {
  id: string;
  username: string;
}

export interface ListResponse {
  id: string;
  name: string;
  invitationCode: string;
  codeExpiresAt: string;
  phase: string;
  invitationsOpen: boolean;
  ownerUsername: string;
  currentRound: number;
  totalRounds: number;
  currentPool: string[];
  members: ListMember[];
}

export interface NameEntry {
  name: string;
  normalizedName: string;
}

export interface AddNameRequest {
  names: string[];
}

export interface SelectionResponse {
  commonNames: NameEntry[];
  fadedSuggestions: NameEntry[];
  myNames: NameEntry[];
}

export interface VoteRequest {
  roundNumber: number;
  rankings: string[];
}

export interface ResultEntry {
  rank: number;
  name: string;
  score: number;
}

export interface ResultsResponse {
  results: ResultEntry[];
}

export interface ApiError {
  error: string;
}