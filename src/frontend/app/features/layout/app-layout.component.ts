import { Component, OnInit, computed, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { UiAppBarComponent } from '../../ui-kit/organisms/app-bar/app-bar.component';
import { AuthService } from '../../services/auth.service';
import { DashboardService } from '../../services/dashboard.service';
import { LocalStorageService } from '../../services/local-storage.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, UiAppBarComponent],
  template: `
    <ui-app-bar
      [title]="title"
      [userLabel]="userLabel()"
      [showAdmin]="isAdmin()"
      logoutLabel="Cerrar sesión"
      (titleClicked)="goHome()"
      (adminClicked)="goAdmin()"
      (logoutClicked)="logout()"
    ></ui-app-bar>
    <router-outlet></router-outlet>
  `,
})
export class AppLayoutComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly dashboardService = inject(DashboardService);
  private readonly localStorageService = inject(LocalStorageService);
  private readonly router = inject(Router);

  readonly title = 'Elegir el Nombre';
  readonly userLabel = computed(() => this.authService.getProfile()?.username ?? '');
  readonly isAdmin = this.authService.isAdmin;

  ngOnInit(): void {
    if (this.authService.getToken()) {
      this.authService.loadProfile().subscribe();
    }
  }

  goHome(): void {
    this.router.navigate(['/']);
  }

  goAdmin(): void {
    this.router.navigate(['/admin']);
  }

  logout(): void {
    this.authService.logout();
    this.dashboardService.invalidate();
    this.localStorageService.clearAllListCaches();
    this.router.navigate(['/login']);
  }
}
