import type { ActionDto } from '../types';
import { apiFetch } from './client';

const BASE = '/api/actions';

export async function listActions(status?: string, priority?: string): Promise<ActionDto[]> {
  const params = new URLSearchParams();
  if (status) params.set('status', status);
  if (priority) params.set('priority', priority);
  const qs = params.toString();
  return apiFetch<ActionDto[]>(`${BASE}${qs ? '?' + qs : ''}`);
}

export async function completeAction(id: string): Promise<ActionDto> {
  return apiFetch<ActionDto>(`${BASE}/${id}/complete`, { method: 'PATCH' });
}
