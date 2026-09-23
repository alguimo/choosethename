import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { guestGuard } from './guards/guest.guard';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register.component').then((m) => m.RegisterComponent),
    canActivate: [guestGuard],
  },
  {
    path: '',
    loadComponent: () =>
      import('./features/layout/app-layout.component').then((m) => m.AppLayoutComponent),
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'admin',
        loadComponent: () => import('./features/admin/admin.component').then((m) => m.AdminComponent),
        canActivate: [adminGuard],
      },
      {
        path: 'lists/:id/suggestion',
        loadComponent: () =>
          import('./features/suggestion/suggestion.component').then((m) => m.SuggestionComponent),
      },
      {
        path: 'lists/:id/selection',
        loadComponent: () =>
          import('./features/selection/selection.component').then((m) => m.SelectionComponent),
      },
      {
        path: 'lists/:id/vote',
        loadComponent: () => import('./features/vote/vote.component').then((m) => m.VoteComponent),
      },
      {
        path: 'lists/:id/results',
        loadComponent: () =>
          import('./features/results/results.component').then((m) => m.ResultsComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
