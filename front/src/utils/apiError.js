/**
 * Утилиты для извлечения сообщений об ошибках из ответов backend API.
 * Backend возвращает тело вида:
 *   { status, error, message, timestamp, errors: { field: message } }
 */

export function getApiErrorMessage(err) {
  const data = err?.response?.data;
  if (data) {
    if (typeof data === 'string' && data.trim()) return data;
    if (data.message) return data.message;
    if (data.error_description) return data.error_description;
    if (data.error) return data.error;
  }
  return err?.message || 'Неизвестная ошибка';
}

export function getApiFieldErrors(err) {
  const errors = err?.response?.data?.errors;
  return errors && typeof errors === 'object' ? errors : {};
}
