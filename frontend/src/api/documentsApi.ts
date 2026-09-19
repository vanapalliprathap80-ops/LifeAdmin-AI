import type { DocumentDto, DocumentDetailDto } from '../types';
import { apiFetch } from './client';

const BASE = '/api/documents';

export async function uploadDocument(file: File): Promise<DocumentDto> {
  const form = new FormData();
  form.append('file', file);
  return apiFetch<DocumentDto>(BASE, { method: 'POST', body: form });
}

export async function listDocuments(): Promise<DocumentDto[]> {
  return apiFetch<DocumentDto[]>(BASE);
}

export async function getDocument(id: string): Promise<DocumentDetailDto> {
  return apiFetch<DocumentDetailDto>(`${BASE}/${id}`);
}

export async function getDocumentStatus(id: string): Promise<DocumentDto> {
  return apiFetch<DocumentDto>(`${BASE}/${id}/status`);
}

export async function reprocessDocument(id: string): Promise<DocumentDto> {
  return apiFetch<DocumentDto>(`${BASE}/${id}/reprocess`, { method: 'POST' });
}

export async function deleteDocument(id: string): Promise<void> {
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
