const BASE_URL = '/api/tasks';

/**
 * Normalizes API response to a task array. GET /api/tasks returns a page { content, totalElements, ... };
 * search and filter may return a plain array.
 */
function toTaskList(data) {
  if (data && Array.isArray(data.content)) return data.content;
  if (Array.isArray(data)) return data;
  return [];
}

/**
 * Reads the response body at most once. For 204 No Content, returns null without reading.
 * For !response.ok, reads JSON once only if content-type is application/json, then throws.
 * For success, reads JSON once and returns it (or null if not JSON).
 */
async function handleResponse(response) {
  const contentType = response.headers.get('content-type');
  const isJson = contentType && contentType.includes('application/json');

  if (response.status === 204) {
    return null;
  }

  if (!response.ok) {
    let message = response.statusText;
    if (isJson) {
      try {
        const body = await response.json();
        if (body && typeof body.message === 'string') {
          message = body.message;
        }
      } catch (_) {
        // keep statusText
      }
    }
    throw new Error(message);
  }

  if (isJson) {
    return await response.json();
  }
  return null;
}

export async function getAllTasks() {
  const data = await handleResponse(await fetch(BASE_URL));
  return toTaskList(data);
}

export async function getTaskById(id) {
  return handleResponse(await fetch(`${BASE_URL}/${id}`));
}

export async function createTask(payload) {
  return handleResponse(await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }));
}

export async function updateTask(id, payload) {
  return handleResponse(await fetch(`${BASE_URL}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  }));
}

export async function deleteTask(id) {
  return handleResponse(await fetch(`${BASE_URL}/${id}`, { method: 'DELETE' }));
}

export async function searchTasks(title) {
  const url = `${BASE_URL}/search?title=${encodeURIComponent(title)}`;
  const data = await handleResponse(await fetch(url));
  return toTaskList(data);
}

export async function filterTasks(status, priority) {
  const params = new URLSearchParams();
  if (status != null && status !== '') params.set('status', status);
  if (priority != null && priority !== '') params.set('priority', priority);
  const query = params.toString();
  const url = query ? `${BASE_URL}/filter?${query}` : BASE_URL;
  const data = await handleResponse(await fetch(url));
  return toTaskList(data);
}
