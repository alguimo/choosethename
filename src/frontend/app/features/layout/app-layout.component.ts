import { Component, inject } from '@angular/core';
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
      logoutLabel="Cerrar sesión"
      (titleClicked)="goHome()"
      (logoutClicked)="logout()"
    ></ui-app-bar>
    <router-outlet></router-outlet>
  `,
})
export class AppLayoutComponent {
  private readonly authService = inject(AuthService);
  private readonly dashboardService = inject(DashboardService);
  private readonly localStorageService = inject(LocalStorageService);
  private readonly router = inject(Router);

  readonly title = 'Elegir el Nombre';

  goHome(): void {
    this.router.navigate(['/']);
  }

  logout(): void {
    this.authService.logout();
    this.dashboardService.invalidate();
    this.localStorageService.clearAllListCaches();
    this.router.navigate(['/login']);
  }
}
