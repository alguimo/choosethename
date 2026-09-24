import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';

const SPECIFIED_DEFAULTS: readonly [string, string][] = [
  ['--ui-color-primary', '#6fa5f0'],
  ['--ui-color-danger', '#e57373'],
  ['--ui-color-success', '#81c784'],
  ['--ui-color-surface', '#1d1e26'],
  ['--ui-color-on-surface', '#e8e6ed'],
  ['--ui-color-outline', '#8a8794'],
  ['--ui-radius-sm', '4px'],
  ['--ui-radius-md', '8px'],
  ['--ui-spacing-xs', '4px'],
  ['--ui-spacing-sm', '8px'],
  ['--ui-spacing-md', '16px'],
  ['--ui-spacing-lg', '24px'],
  ['--ui-font-size-sm', '0.875rem'],
  ['--ui-font-size-base', '1rem'],
  ['--ui-font-size-lg', '1.25rem'],
];

@Component({
  standalone: true,
  selector: 'ui-tokens-spec-host',
  template: '<div class="token-probe"></div>',
  styles: [
    ':host { --ui-color-primary: #123456; }',
    '.token-probe { color: var(--ui-color-primary); }',
  ],
})
class TokenOverrideHostComponent {}

describe('ui-kit design tokens', () => {
  it('defines every specified default on the document root when no overrides exist', () => {
    const styles = getComputedStyle(document.documentElement);

    for (const [token, expected] of SPECIFIED_DEFAULTS) {
      expect(styles.getPropertyValue(token).trim().toLowerCase()).toBe(expected);
    }
  });

  it('lets a consuming scope override a token while root defaults stay intact', () => {
    const fixture = TestBed.createComponent(TokenOverrideHostComponent);
    const probe = fixture.nativeElement.querySelector('.token-probe') as HTMLElement;

    expect(getComputedStyle(probe).color).toBe('rgb(18, 52, 86)');
    expect(
      getComputedStyle(document.documentElement).getPropertyValue('--ui-color-primary').trim(),
    ).toBe('#6FA5F0');
  });
});