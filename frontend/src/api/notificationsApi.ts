import type { NotificationDto } from '../types';
import { apiFetch } from './client';

const BASE = '/api/notifications';

export const getNotifications = async (): Promise<NotificationDto[]> => {
  return apiFetch<NotificationDto[]>(BASE);
};
