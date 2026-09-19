import { apiFetch } from './client';

export const sendChatMessage = async (message: string): Promise<string> => {
  const res = await apiFetch<{ response: string }>('/api/chat', {
    method: 'POST',
    body: JSON.stringify({ message }),
  });
  return res.response;
};
