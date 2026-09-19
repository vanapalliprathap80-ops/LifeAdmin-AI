import type { ServiceDto, CreateServiceRequest } from '../types';
import { apiFetch } from './client';

const BASE = '/api/services';

export async function createService(req: CreateServiceRequest): Promise<ServiceDto> {
  return apiFetch<ServiceDto>(BASE, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function listServices(): Promise<ServiceDto[]> {
  return apiFetch<ServiceDto[]>(BASE);
}

export async function getService(id: string): Promise<ServiceDto> {
  return apiFetch<ServiceDto>(`${BASE}/${id}`);
}

export async function deleteService(id: string): Promise<void> {
  try {
    return await apiFetch<void>(`${BASE}/${id}`, { method: 'DELETE' });
  } catch (err) {
    if (err instanceof Error && err.message.includes('404')) {
      // Already deleted, treat as success
      return;
    }
    throw err;
  }
}
