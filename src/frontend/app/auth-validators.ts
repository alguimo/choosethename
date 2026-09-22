import { AbstractControl, ValidationErrors } from '@angular/forms';

const PASSWORD_MIN_LENGTH = 8;
const PASSWORD_UPPERCASE = /[A-Z]/;
const PASSWORD_DIGIT = /\d/;

export function nonWhitespaceOnly(control: AbstractControl): ValidationErrors | null {
  const value: string | null | undefined = control.value;
  if (value !== null && value !== undefined && String(value).length > 0 && !/\S/.test(value)) {
    return { whitespaceOnly: true };
  }
  return null;
}

export function passwordComplexity(control: AbstractControl): ValidationErrors | null {
  const value: string = control.value ?? '';
  if (value.length === 0) {
    return null;
  }
  if (value.length < PASSWORD_MIN_LENGTH) {
    return { passwordLength: true };
  }
  if (!PASSWORD_UPPERCASE.test(value)) {
    return { passwordUppercase: true };
  }
  if (!PASSWORD_DIGIT.test(value)) {
    return { passwordDigit: true };
  }
  return null;
}