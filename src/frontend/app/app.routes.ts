import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'lists/:id/suggestion',
    loadComponent: () =>
      import('./features/suggestion/suggestion.component').then((m) => m.SuggestionComponent),
    canActivate: [authGuard],
  },
  {
    path: 'lists/:id/selection',
    loadComponent: () =>
      import('./features/selection/selection.component').then((m) => m.SelectionComponent),
    canActivate: [authGuard],
  },
  {
    path: 'lists/:id/vote',
    loadComponent: () => import('./features/vote/vote.component').then((m) => m.VoteComponent),
    canActivate: [authGuard],
  },
  {
    path: 'lists/:id/results',
    loadComponent: () =>
      import('./features/results/results.component').then((m) => m.ResultsComponent),
    canActivate: [authGuard],
  },
  {
    path: '',
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
    canActivate: [authGuard],
  },
  { path: '**', redirectTo: '' },
];
