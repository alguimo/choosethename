import { UiBadgeColor } from './atoms/badge/badge.component';

export const PHASE_LABELS: Readonly<Record<string, string>> = {
  ADDITION: 'Propuestas',
  SELECTION: 'Selección',
  VOTING: 'Votación',
  COMPLETED: 'Completada',
  EXPIRED: 'Expirada',
};

export const PHASE_BADGE_COLORS: Readonly<Record<string, UiBadgeColor>> = {
  ADDITION: 'default',
  SELECTION: 'default',
  VOTING: 'warning',
  COMPLETED: 'success',
  EXPIRED: 'danger',
};

export function phaseLabel(phase: string): string {
  return PHASE_LABELS[phase] ?? phase;
}

export function phaseBadgeColor(phase: string): UiBadgeColor {
  return PHASE_BADGE_COLORS[phase] ?? 'default';
}