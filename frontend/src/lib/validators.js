/**
 * Client-side validators matching Spring Boot Bean Validation (BR-10).
 */

export const NIC_REGEX = /^(\d{9}[VvXx]|\d{12})$/;
export const PHONE_REGEX = /^07\d{8}$/;
export const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;
export const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateRegistration(form) {
  const errors = {};
  if (!form.fullName || form.fullName.trim().length < 2) {
    errors.fullName = 'Full name must be at least 2 characters.';
  }
  if (!NIC_REGEX.test((form.nic || '').trim())) {
    errors.nic = 'Enter a valid Sri Lankan NIC (9 digits + V/X or 12 digits).';
  }
  if (!form.dateOfBirth || !/^\d{4}-\d{2}-\d{2}$/.test(form.dateOfBirth)) {
    errors.dateOfBirth = 'Select your date of birth.';
  }
  if (!['MALE', 'FEMALE', 'OTHER'].includes(form.gender)) {
    errors.gender = 'Please select gender.';
  }
  if (!PHONE_REGEX.test((form.phone || '').trim())) {
    errors.phone = 'Phone must follow Sri Lankan format 07XXXXXXXX.';
  }
  if (!EMAIL_REGEX.test((form.email || '').trim())) {
    errors.email = 'Enter a valid email address.';
  }
  if (!form.address || form.address.trim().length < 5) {
    errors.address = 'Address must be at least 5 characters.';
  }
  if (!PASSWORD_REGEX.test(form.password || '')) {
    errors.password = 'Password needs 8+ characters with a letter, a number, and a symbol.';
  }
  return errors;
}

export function validateLogin(form) {
  const errors = {};
  if (!EMAIL_REGEX.test((form.email || '').trim())) {
    errors.email = 'Enter a valid email address.';
  }
  if (!form.password) {
    errors.password = 'Password is required.';
  }
  return errors;
}
