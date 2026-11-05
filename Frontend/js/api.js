// All backend calls go through API.req(). Expected Spring Boot endpoints:
//   POST /auth/login, POST /auth/register  -> { token, user:{name,email,role} }
//   GET  /vehicles?category=&start=&end=   -> [ {id,make,model,year,category,seats,transmission,dailyRate,status} ]
//   POST /rentals {vehicleId,start,end}    -> rental
//   GET  /rentals/me                       -> [ rental ]
//   POST /rentals/{id}/cancel              -> rental
//   GET  /admin/vehicles                   -> [ vehicle ]
const MOCK = {
  vehicles: [
    { id: 1, make: 'Toyota', model: 'Vios', year: 2023, category: 'Sedan', seats: 5, transmission: 'Automatic', dailyRate: 1800, status: 'AVAILABLE' },
    { id: 2, make: 'Mitsubishi', model: 'Xpander', year: 2024, category: 'MPV', seats: 7, transmission: 'Automatic', dailyRate: 2600, status: 'AVAILABLE' },
    { id: 3, make: 'Toyota', model: 'Fortuner', year: 2023, category: 'SUV', seats: 7, transmission: 'Automatic', dailyRate: 4200, status: 'AVAILABLE' },
    { id: 4, make: 'Honda', model: 'City', year: 2022, category: 'Sedan', seats: 5, transmission: 'Manual', dailyRate: 1700, status: 'MAINTENANCE' },
    { id: 5, make: 'Toyota', model: 'Hiace', year: 2022, category: 'Van', seats: 12, transmission: 'Manual', dailyRate: 4800, status: 'AVAILABLE' },
    { id: 6, make: 'Suzuki', model: 'Jimny', year: 2024, category: 'SUV', seats: 4, transmission: 'Manual', dailyRate: 3200, status: 'RENTED' }
  ],
  rentals() { return JSON.parse(localStorage.getItem('mockRentals') || '[]'); },
  save(r) { localStorage.setItem('mockRentals', JSON.stringify(r)); },
  async handle(path, opts) {
    await new Promise(r => setTimeout(r, 250));
    const body = opts.body || {};
    if (path === '/auth/forgot-password') return { message: 'If that email has an account, we sent a link to reset the password. It expires in about an hour.' };
    if (path === '/auth/resend-verification') return { message: 'If that email has an account waiting for confirmation, we sent a new link. Check your inbox and spam folder.' };
    if (path === '/auth/reset-password') return { message: 'Your password has been changed. You can log in with it now.' };
    if (path.startsWith('/auth/')) {
      const role = /admin/i.test(body.email) ? 'ADMIN' : /staff/i.test(body.email) ? 'STAFF' : 'CUSTOMER';
      return { token: 'mock.jwt.token', user: { name: body.name || body.email.split('@')[0], email: body.email, role } };
    }
    if (path.startsWith('/vehicles')) {
      const cat = new URLSearchParams(path.split('?')[1] || '').get('category');
      return this.vehicles.filter(v => !cat || v.category === cat);
    }
    if (path === '/admin/vehicles') return this.vehicles;
    if (path === '/rentals/me') return this.rentals();
    if (path === '/rentals') {
      const v = this.vehicles.find(v => v.id === body.vehicleId);
      const days = Math.max(1, Math.round((new Date(body.end) - new Date(body.start)) / 864e5));
      const rental = { id: Date.now(), vehicle: `${v.make} ${v.model}`, start: body.start, end: body.end, total: days * v.dailyRate, status: 'RESERVED' };
      this.save([rental, ...this.rentals()]);
      return rental;
    }
    const m = path.match(/^\/rentals\/(\d+)\/cancel$/);
    if (m) {
      const all = this.rentals().map(r => r.id == m[1] ? { ...r, status: 'CANCELLED' } : r);
      this.save(all);
      return all.find(r => r.id == m[1]);
    }
    throw new Error('Unknown mock endpoint: ' + path);
  }
};

const API = {
  async req(path, opts = {}) {
    if (CONFIG.USE_MOCK) return MOCK.handle(path, opts);
    const token = localStorage.getItem('token');
    // FormData (file uploads) must go as-is: the browser sets the multipart Content-Type and boundary itself.
    const isForm = typeof FormData !== 'undefined' && opts.body instanceof FormData;
    const res = await fetch(CONFIG.API_URL + path, {
      method: opts.method || 'GET',
      headers: { 'ngrok-skip-browser-warning': '1', ...(isForm ? {} : { 'Content-Type': 'application/json' }), ...(token ? { Authorization: 'Bearer ' + token } : {}) },
      body: opts.body ? (isForm ? opts.body : JSON.stringify(opts.body)) : undefined
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      if (res.status === 401) Auth.logout();
      throw new Error(err.message || err.detail || 'Something went wrong (' + res.status + ')');
    }
    return res.json();
  },
  get: p => API.req(p),
  post: (p, body) => API.req(p, { method: 'POST', body }),
  put: (p, body) => API.req(p, { method: 'PUT', body }),
  del: p => API.req(p, { method: 'DELETE' }),
  upload: (p, formData) => API.req(p, { method: 'POST', body: formData }),
  // Protected images need the Bearer token, so <img src> can't load them directly. Fetch the bytes and return a blob: URL.
  async blobUrl(path) {
    const token = localStorage.getItem('token');
    const res = await fetch(CONFIG.API_URL + path, { headers: { 'ngrok-skip-browser-warning': '1', ...(token ? { Authorization: 'Bearer ' + token } : {}) } });
    if (!res.ok) throw new Error('Photo not available');
    return URL.createObjectURL(await res.blob());
  }
};

const Auth = {
  user: () => JSON.parse(localStorage.getItem('user') || 'null'),
  login(data) { localStorage.setItem('token', data.token); localStorage.setItem('user', JSON.stringify(data.user)); },
  logout() { localStorage.removeItem('token'); localStorage.removeItem('user'); location.href = 'index.html'; },
  require(roles) {
    const u = Auth.user();
    if (!u) location.href = 'login.html?next=' + encodeURIComponent(location.pathname.split('/').pop() + location.search);
    else if (roles && !roles.includes(u.role)) location.href = 'index.html';
    return u;
  }
};
