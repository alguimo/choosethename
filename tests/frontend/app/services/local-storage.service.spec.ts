import { TestBed } from '@angular/core/testing';
import { LocalStorageService } from '@app/services/local-storage.service';

describe('LocalStorageService', () => {
  let service: LocalStorageService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(LocalStorageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should set and get a JSON-serialized value for a list', () => {
    const payload = ['Pablo', 'Ana'];
    service.setItem('42', 'suggestions', payload);
    expect(service.getItem<string[]>('42', 'suggestions')).toEqual(payload);
  });

  it('should namespace keys per list id', () => {
    service.setItem('1', 'suggestions', ['a']);
    service.setItem('2', 'suggestions', ['b']);

    expect(service.getItem<string[]>('1', 'suggestions')).toEqual(['a']);
    expect(service.getItem<string[]>('2', 'suggestions')).toEqual(['b']);
  });

  it('should return null when the key does not exist', () => {
    expect(service.getItem('99', 'vote_round_1')).toBeNull();
  });

  it('should remove an item', () => {
    service.setItem('42', 'suggestions', ['a']);
    service.removeItem('42', 'suggestions');
    expect(service.getItem('42', 'suggestions')).toBeNull();
  });

  it('should clear only the cache entries belonging to the given list', () => {
    service.setItem('1', 'suggestions', ['a']);
    service.setItem('1', 'vote_round_1', ['x']);
    service.setItem('2', 'suggestions', ['b']);

    service.clearListCache('1');

    expect(service.getItem('1', 'suggestions')).toBeNull();
    expect(service.getItem('1', 'vote_round_1')).toBeNull();
    expect(service.getItem<string[]>('2', 'suggestions')).toEqual(['b']);
  });

  it('FR-65: should clear every list cache entry while keeping unrelated keys', () => {
    service.setItem('1', 'suggestions', ['a']);
    service.setItem('1', 'vote_round_1', ['x']);
    service.setItem('2', 'suggestions', ['b']);
    localStorage.setItem('auth_token', 'jwt');

    service.clearAllListCaches();

    expect(service.getItem('1', 'suggestions')).toBeNull();
    expect(service.getItem('1', 'vote_round_1')).toBeNull();
    expect(service.getItem('2', 'suggestions')).toBeNull();
    expect(localStorage.getItem('auth_token')).toBe('jwt');
  });

  it('TS-27: should report unavailable when storage access is blocked at startup', () => {
    spyOn(localStorage, 'setItem').and.throwError('SecurityError');

    const blocked = new LocalStorageService();

    expect(blocked.isAvailable()).toBeFalse();
  });

  it('TS-27: should degrade gracefully and continue without persistence when a write fails', () => {
    expect(service.isAvailable()).toBeTrue();

    spyOn(localStorage, 'setItem').and.throwError('QuotaExceededError');

    expect(() => service.setItem('1', 'suggestions', ['a'])).not.toThrow();
    expect(service.isAvailable()).toBeFalse();
    expect(service.getItem('1', 'suggestions')).toBeNull();
    expect(() => service.removeItem('1', 'suggestions')).not.toThrow();
    expect(() => service.clearListCache('1')).not.toThrow();
  });
});