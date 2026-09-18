import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AddNameRequest,
  AuthResponse,
  CreateListRequest,
  Credentials,
  JoinListRequest,
  ListResponse,
  ResultsResponse,
  SelectionResponse,
  VoteRequest,
} from '../models/api.models';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private http = inject(HttpClient);
  private readonly baseUrl = '/api/v1';

  login(credentials: Credentials): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/login`, credentials);
  }

  createList(request: CreateListRequest): Observable<ListResponse> {
    return this.http.post<ListResponse>(`${this.baseUrl}/lists`, request);
  }

  joinList(request: JoinListRequest): Observable<ListResponse> {
    return this.http.post<ListResponse>(`${this.baseUrl}/lists/join`, request);
  }

  getActiveList(): Observable<ListResponse> {
    return this.http.get<ListResponse>(`${this.baseUrl}/lists/active`);
  }

  closeInvitations(id: string): Observable<ListResponse> {
    return this.http.patch<ListResponse>(`${this.baseUrl}/lists/${id}/close-invitations`, {});
  }

  addNames(listId: string, request: AddNameRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/lists/${listId}/names`, request);
  }

  finishAddition(listId: string): Observable<ListResponse> {
    return this.http.post<ListResponse>(`${this.baseUrl}/lists/${listId}/finish-addition`, {});
  }

  getSelection(listId: string): Observable<SelectionResponse> {
    return this.http.get<SelectionResponse>(`${this.baseUrl}/lists/${listId}/selection`);
  }

  adoptFadedName(listId: string, name: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/lists/${listId}/selection/adopt`, { name });
  }

  completeSelection(listId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/lists/${listId}/complete-selection`, {});
  }

  submitVote(listId: string, request: VoteRequest): Observable<ListResponse> {
    return this.http.post<ListResponse>(`${this.baseUrl}/lists/${listId}/vote`, request);
  }

  getResults(listId: string): Observable<ResultsResponse> {
    return this.http.get<ResultsResponse>(`${this.baseUrl}/lists/${listId}/results`);
  }
}