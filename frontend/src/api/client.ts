import type { ApiResponse } from '../types';

const TOKEN_KEY = 'lifeadmin_token';

export function getAuthToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setAuthToken(token: string | null): void {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

export async function apiFetch<T>(
  url: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getAuthToken();
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string> || {}),
  };

  if (token && !headers['Authorization']) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // If body is not FormData and Content-Type is not set, default to JSON
  if (options.body && !(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  const res = await fetch(url, {
    ...options,
    headers,
  });

  if (!res.ok) {
    let errorMsg = `Request failed (${res.status})`;
    try {
      const errorBody = await res.json();
      errorMsg = errorBody?.error || errorBody?.message || errorMsg;
    } catch {
      // Ignored
    }
    throw new Error(errorMsg);
  }

  // If 204 No Content
  if (res.status === 204) {
    return {} as T;
  }

  const body = await res.json().catch(() => ({}));
  // If wrapped in standard ApiResponse { success, data, error }
  if (body && typeof body === 'object' && 'success' in body) {
    const apiRes = body as ApiResponse<T>;
    if (!apiRes.success) {
      throw new Error(apiRes.error ?? 'Unknown server error');
    }
    return apiRes.data;
  }

  return body as T;
}

export async function ensureDemoSession(): Promise<string> {
  let token = getAuthToken();
  if (token) return token;

  try {
    const res = await fetch('/api/auth/demo', { method: 'POST' });
    if (res.ok) {
      const data = await res.json();
      if (data.token) {
        setAuthToken(data.token);
        return data.token;
      }
    }
  } catch (err) {
    console.warn('Could not auto-login demo user:', err);
  }
  return '';
}
