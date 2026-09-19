import type { ProviderConnectionDto, ConnectProviderRequest, ProviderCatalogDto } from '../types';
import { apiFetch } from './client';

const BASE = '/api/integrations';

export const getConnections = async (): Promise<ProviderConnectionDto[]> => {
  return apiFetch<ProviderConnectionDto[]>(BASE);
};

export const getCatalog = async (): Promise<ProviderCatalogDto[]> => {
  return apiFetch<ProviderCatalogDto[]>(`${BASE}/catalog`);
};

export const connectProvider = async (req: ConnectProviderRequest): Promise<ProviderConnectionDto> => {
  return apiFetch<ProviderConnectionDto>(`${BASE}/connect`, {
    method: 'POST',
    body: JSON.stringify(req)
  });
};

export const syncProvider = async (id: string): Promise<ProviderConnectionDto> => {
  return apiFetch<ProviderConnectionDto>(`${BASE}/${id}/sync`, { method: 'POST' });
};

export const disconnectProvider = async (id: string): Promise<void> => {
  return apiFetch<void>(`${BASE}/${id}`, { method: 'DELETE' });
};
