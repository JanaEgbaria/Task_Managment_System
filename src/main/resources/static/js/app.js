(function () {
  'use strict';

  const API_BASE = 'http://localhost:8081/api/tasks';
  const AUTH_BASE = 'http://localhost:8081/api/auth';
  const loadingSpinner = document.getElementById('loading-spinner');
  const tasksContainer = document.getElementById('tasks-container');
  const taskTbody = document.getElementById('task-tbody');
  const emptyState = document.getElementById('empty-state');
  const taskModal = new bootstrap.Modal(document.getElementById('taskModal'));
  const taskModalLabel = document.getElementById('taskModalLabel');
  const taskForm = document.getElementById('task-form');
  const taskIdInput = document.getElementById('task-id');
  const toastContainer = document.getElementById('toast-container');
  const searchTitleInput = document.getElementById('search-title');
  const filterStatusSelect = document.getElementById('filter-status');
  const filterPrioritySelect = document.getElementById('filter-priority');
  const authView = document.getElementById('auth-view');
  const appView = document.getElementById('app-view');
  const navAppActions = document.getElementById('nav-app-actions');
  const navUsername = document.getElementById('nav-username');
  const loginFormContainer = document.getElementById('login-form-container');
  const registerFormContainer = document.getElementById('register-form-container');
  const authCardTitle = document.getElementById('auth-card-title');

  var authToken = null;
  var authUsername = null;
  var searchAbortController = null;
  var SEARCH_DEBOUNCE_MS = 400;
  var lastInteraction = null;

  function showLoading(show) {
    loadingSpinner.classList.toggle('d-none', !show);
    tasksContainer.classList.toggle('d-none', show);
    emptyState.classList.add('d-none');
  }

  function showToast(message, type) {
    type = type === 'success' || type === 'error' ? type : 'success';
    const bg = type === 'success' ? 'bg-success' : 'bg-danger';
    const id = 'toast-' + Date.now();
    const html = `
      <div class="toast align-items-center text-white ${bg} border-0" role="alert" id="${id}">
        <div class="d-flex">
          <div class="toast-body">${escapeHtml(message)}</div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
      </div>`;
    toastContainer.insertAdjacentHTML('beforeend', html);
    const el = document.getElementById(id);
    const toast = new bootstrap.Toast(el, { delay: 4000 });
    toast.show();
    el.addEventListener('hidden.bs.toast', function () { el.remove(); });
  }

  function escapeHtml(s) {
    if (s == null) return '';
    const div = document.createElement('div');
    div.textContent = s;
    return div.innerHTML;
  }

  function formatDate(value) {
    if (!value) return '—';
    const d = new Date(value);
    return isNaN(d.getTime()) ? value : d.toISOString().slice(0, 10);
  }

  function getPayload() {
    return {
      title: document.getElementById('title').value.trim(),
      description: document.getElementById('description').value.trim() || null,
      status: document.getElementById('status').value || null,
      priority: document.getElementById('priority').value || null,
      dueDate: document.getElementById('dueDate').value || null
    };
  }

  function setForm(task) {
    document.getElementById('title').value = task.title || '';
    document.getElementById('description').value = task.description || '';
    document.getElementById('status').value = task.status || 'TODO';
    document.getElementById('priority').value = task.priority || 'MEDIUM';
    document.getElementById('dueDate').value = task.dueDate ? formatDate(task.dueDate) : '';
    taskIdInput.value = task.id ? String(task.id) : '';
  }

  function clearForm() {
    setForm({});
    taskIdInput.value = '';
  }

  async function request(url, options) {
    var headers = { 'Content-Type': 'application/json', ...(options && options.headers) };
    if (authToken) {
      headers['Authorization'] = 'Bearer ' + authToken;
    }
    const res = await fetch(url, {
      headers: headers,
      ...options
    });
    const contentType = res.headers.get('content-type');
    const isJson = contentType && contentType.includes('application/json');

    if (res.status === 401 || res.status === 403) {
      authToken = null;
      authUsername = null;
      showAuthView();
      showToast('Session expired. Please log in again.', 'error');
      var msg = res.statusText;
      if (isJson) {
        try {
          const body = await res.json();
          if (body && typeof body.message === 'string') msg = body.message;
        } catch (_) {}
      }
      throw new Error(msg);
    }

    if (res.status === 204) return null;

    if (!res.ok) {
      var message = res.statusText;
      if (isJson) {
        try {
          const body = await res.json();
          if (body && typeof body.message === 'string') message = body.message;
        } catch (_) {}
      }
      throw new Error(message);
    }

    if (isJson) return await res.json();
    return null;
  }

  function showAuthView() {
    if (authView) authView.classList.remove('d-none');
    if (appView) appView.classList.add('d-none');
    if (navAppActions) navAppActions.classList.add('d-none');
    if (loginFormContainer) loginFormContainer.classList.remove('d-none');
    if (registerFormContainer) registerFormContainer.classList.add('d-none');
    if (authCardTitle) authCardTitle.textContent = 'Login';
  }

  function showAppView() {
    if (authView) authView.classList.add('d-none');
    if (appView) appView.classList.remove('d-none');
    if (navAppActions) navAppActions.classList.remove('d-none');
    if (navUsername) navUsername.textContent = authUsername ? 'Hello, ' + authUsername : '';
  }

  function showLoginForm() {
    if (loginFormContainer) loginFormContainer.classList.remove('d-none');
    if (registerFormContainer) registerFormContainer.classList.add('d-none');
    if (authCardTitle) authCardTitle.textContent = 'Login';
  }

  function showRegisterForm() {
    if (loginFormContainer) loginFormContainer.classList.add('d-none');
    if (registerFormContainer) registerFormContainer.classList.remove('d-none');
    if (authCardTitle) authCardTitle.textContent = 'Register';
  }

  async function handleLogin(e) {
    e.preventDefault();
    var username = document.getElementById('login-username').value.trim();
    var password = document.getElementById('login-password').value;
    if (!username || !password) {
      showToast('Please enter username and password', 'error');
      return;
    }
    try {
      var res = await fetch(AUTH_BASE + '/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username, password: password })
      });
      var data = res.ok && res.headers.get('content-type') && res.headers.get('content-type').includes('application/json') ? await res.json() : null;
      if (!res.ok) {
        var msg = (data && data.message) ? data.message : res.statusText;
        showToast(msg || 'Login failed', 'error');
        return;
      }
      authToken = data && data.token ? data.token : null;
      authUsername = data && data.username ? data.username : username;
      showAppView();
      fetchAllTasks();
      showToast('Logged in successfully.', 'success');
    } catch (err) {
      showToast(err.message || 'Login failed', 'error');
    }
  }

  async function handleRegister(e) {
    e.preventDefault();
    var username = document.getElementById('register-username').value.trim();
    var email = document.getElementById('register-email').value.trim();
    var password = document.getElementById('register-password').value;
    if (!username || !email || !password) {
      showToast('Please fill in all fields', 'error');
      return;
    }
    if (password.length < 6) {
      showToast('Password must be at least 6 characters', 'error');
      return;
    }
    try {
      var res = await fetch(AUTH_BASE + '/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username, email: email, password: password })
      });
      var data = res.ok && res.headers.get('content-type') && res.headers.get('content-type').includes('application/json') ? await res.json() : null;
      if (!res.ok) {
        var msg = (data && data.message) ? data.message : res.statusText;
        showToast(msg || 'Registration failed', 'error');
        return;
      }
      authToken = data && data.token ? data.token : null;
      authUsername = data && data.username ? data.username : username;
      showAppView();
      fetchAllTasks();
      showToast('Account created. Welcome!', 'success');
    } catch (err) {
      showToast(err.message || 'Registration failed', 'error');
    }
  }

  var VALID_STATUSES = ['TODO', 'IN_PROGRESS', 'DONE'];
  var VALID_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'];

  /**
   * Builds the correct API URL. Search overrides filters.
   * - Search non-empty → GET /api/tasks/search?title=... (ignore status/priority).
   * - Search empty + status/priority "All" (or empty) → GET /api/tasks (all tasks).
   * - Search empty + only status selected → GET /api/tasks/filter?status=STATUS.
   * - Search empty + only priority selected → GET /api/tasks/filter?priority=PRIORITY.
   * - Search empty + both selected → GET /api/tasks/filter?status=STATUS&priority=PRIORITY.
   * Never send "ALL" or empty values to the backend.
   */
  function buildListUrl() {
    var searchTrimmed = searchTitleInput && searchTitleInput.value ? searchTitleInput.value.trim() : '';
    var statusVal = filterStatusSelect && filterStatusSelect.value ? filterStatusSelect.value.trim() : '';
    var priorityVal = filterPrioritySelect && filterPrioritySelect.value ? filterPrioritySelect.value.trim() : '';
    if (statusVal && statusVal.toUpperCase() === 'ALL') statusVal = '';
    if (priorityVal && priorityVal.toUpperCase() === 'ALL') priorityVal = '';
    var statusSelected = VALID_STATUSES.indexOf(statusVal) !== -1;
    var prioritySelected = VALID_PRIORITIES.indexOf(priorityVal) !== -1;

    if (searchTrimmed) {
      return API_BASE + '/search?title=' + encodeURIComponent(searchTrimmed);
    }
    if (statusSelected || prioritySelected) {
      var params = new URLSearchParams();
      if (statusSelected) params.set('status', statusVal);
      if (prioritySelected) params.set('priority', priorityVal);
      return API_BASE + '/filter?' + params.toString();
    }
    return API_BASE;
  }

  /**
   * Loads tasks (all, filtered, or search). Optional signal: when provided (e.g. from search),
   * the request can be aborted so older responses never override newer ones.
   * focusState: { shouldRestoreSearchFocus: boolean, caret: number } to restore search input focus after render.
   */
  async function fetchAllTasks(signal, focusState) {
    showLoading(true);
    var aborted = false;
    try {
      var url = buildListUrl();
      var opts = signal ? { signal: signal } : {};
      var data = await request(url, opts);
      if (signal && signal.aborted) return [];
      var tasks = (data && Array.isArray(data.content)) ? data.content : (Array.isArray(data) ? data : []);

      if (tasks.length === 0) {
        taskTbody.innerHTML = '';
        tasksContainer.classList.add('d-none');
        emptyState.classList.remove('d-none');
        var msgEl = document.getElementById('empty-state-message');
        if (msgEl) {
          msgEl.textContent = (url === API_BASE || url === API_BASE + '/') ? 'No tasks yet.' : 'No tasks found.';
        }
        document.getElementById('btn-add-task-empty').onclick = openModalForCreate;
      } else {
        emptyState.classList.add('d-none');
        tasksContainer.classList.remove('d-none');
        renderTable(tasks);
      }

      if (focusState && focusState.shouldRestoreSearchFocus && searchTitleInput) {
        var caret = typeof focusState.caret === 'number' ? Math.min(focusState.caret, searchTitleInput.value.length) : searchTitleInput.value.length;
        requestAnimationFrame(function () {
          searchTitleInput.focus({ preventScroll: true });
          searchTitleInput.setSelectionRange(caret, caret);
        });
      }
      return tasks;
    } catch (e) {
      if (e.name === 'AbortError') {
        aborted = true;
        return [];
      }
      if (authToken) showToast(e.message || 'Failed to load tasks', 'error');
      taskTbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">Failed to load.</td></tr>';
      tasksContainer.classList.remove('d-none');
      return [];
    } finally {
      if (!aborted) showLoading(false);
    }
  }

  /** Only updates the table tbody (rows). Toolbar with search + filters is never re-rendered. */
  function renderTable(tasks) {
    taskTbody.innerHTML = tasks.map(function (task) {
      const title = escapeHtml(task.title);
      const desc = escapeHtml((task.description || '').slice(0, 80)) + (task.description && task.description.length > 80 ? '…' : '');
      const status = escapeHtml(task.status || '');
      const priority = escapeHtml(task.priority || '');
      const due = formatDate(task.dueDate);
      const id = escapeHtml(String(task.id));
      return (
        '<tr>' +
        '<td>' + title + '</td>' +
        '<td class="text-muted small">' + (task.description ? desc : '—') + '</td>' +
        '<td><span class="badge status-' + (task.status || '').toLowerCase() + '">' + status + '</span></td>' +
        '<td><span class="badge priority-' + (task.priority || '').toLowerCase() + '">' + priority + '</span></td>' +
        '<td>' + due + '</td>' +
        '<td class="text-end">' +
        '<button type="button" class="btn btn-sm btn-outline-primary me-1 btn-edit" data-id="' + id + '">Edit</button>' +
        '<button type="button" class="btn btn-sm btn-outline-danger btn-delete" data-id="' + id + '">Delete</button>' +
        '</td></tr>'
      );
    }).join('');

    taskTbody.querySelectorAll('.btn-edit').forEach(function (btn) {
      btn.addEventListener('click', function () { openModalForEdit(Number(btn.dataset.id)); });
    });
    taskTbody.querySelectorAll('.btn-delete').forEach(function (btn) {
      btn.addEventListener('click', function () { deleteTask(Number(btn.dataset.id)); });
    });
  }

  function openModalForCreate() {
    lastInteraction = 'filter';
    taskModalLabel.textContent = 'Add Task';
    clearForm();
    taskModal.show();
  }

  async function openModalForEdit(id) {
    lastInteraction = 'filter';
    taskModalLabel.textContent = 'Edit Task';
    try {
      const task = await request(API_BASE + '/' + id);
      setForm(task);
      taskModal.show();
    } catch (e) {
      showToast(e.message || 'Failed to load task', 'error');
    }
  }

  async function saveTask() {
    const id = taskIdInput.value.trim();
    const payload = getPayload();
    if (!payload.title) {
      showToast('Title is required', 'error');
      return;
    }
    try {
      if (id) {
        await request(API_BASE + '/' + id, { method: 'PUT', body: JSON.stringify(payload) });
        showToast('Task updated successfully.', 'success');
      } else {
        await request(API_BASE, { method: 'POST', body: JSON.stringify(payload) });
        showToast('Task created successfully.', 'success');
      }
      taskModal.hide();
      lastInteraction = 'filter';
      fetchAllTasks();
    } catch (e) {
      showToast(e.message || (id ? 'Update failed' : 'Create failed'), 'error');
    }
  }

  async function deleteTask(id) {
    lastInteraction = 'filter';
    if (!confirm('Delete this task?')) return;
    try {
      await request(API_BASE + '/' + id, { method: 'DELETE' });
      showToast('Task deleted.', 'success');
      fetchAllTasks();
    } catch (e) {
      showToast(e.message || 'Delete failed', 'error');
    }
  }

  function debounce(fn, ms) {
    var timeoutId;
    return function () {
      clearTimeout(timeoutId);
      var self = this;
      var args = arguments;
      timeoutId = setTimeout(function () { fn.apply(self, args); }, ms);
    };
  }

  function resetFilters() {
    lastInteraction = 'filter';
    if (searchTitleInput) searchTitleInput.value = '';
    if (filterStatusSelect) filterStatusSelect.value = '';
    if (filterPrioritySelect) filterPrioritySelect.value = '';
    fetchAllTasks();
  }

  document.getElementById('btn-add-task').addEventListener('click', openModalForCreate);
  document.getElementById('btn-save-task').addEventListener('click', saveTask);

  var btnLogout = document.getElementById('btn-logout');
  if (btnLogout) btnLogout.addEventListener('click', function () {
    authToken = null;
    authUsername = null;
    showAuthView();
  });

  var loginForm = document.getElementById('login-form');
  if (loginForm) loginForm.addEventListener('submit', handleLogin);
  var registerForm = document.getElementById('register-form');
  if (registerForm) registerForm.addEventListener('submit', handleRegister);

  var linkShowRegister = document.getElementById('link-show-register');
  if (linkShowRegister) linkShowRegister.addEventListener('click', function (e) { e.preventDefault(); showRegisterForm(); });
  var linkShowLogin = document.getElementById('link-show-login');
  if (linkShowLogin) linkShowLogin.addEventListener('click', function (e) { e.preventDefault(); showLoginForm(); });

  var btnReset = document.getElementById('btn-reset-filters');
  if (btnReset) btnReset.addEventListener('click', resetFilters);

  if (searchTitleInput) {
    searchTitleInput.addEventListener('input', function () { lastInteraction = 'search'; });
    searchTitleInput.addEventListener('input', debounce(function () {
      if (searchAbortController) searchAbortController.abort();
      searchAbortController = new AbortController();
      var shouldRestoreSearchFocus = (document.activeElement === searchTitleInput) || (lastInteraction === 'search');
      var caret = searchTitleInput.selectionStart != null ? searchTitleInput.selectionStart : searchTitleInput.value.length;
      fetchAllTasks(searchAbortController.signal, { shouldRestoreSearchFocus: shouldRestoreSearchFocus, caret: caret });
    }, SEARCH_DEBOUNCE_MS));
  }
  if (filterStatusSelect) {
    filterStatusSelect.addEventListener('change', function () { lastInteraction = 'filter'; fetchAllTasks(); });
  }
  if (filterPrioritySelect) {
    filterPrioritySelect.addEventListener('change', function () { lastInteraction = 'filter'; fetchAllTasks(); });
  }

  if (authToken) {
    showAppView();
    fetchAllTasks();
  } else {
    showAuthView();
  }
})();
