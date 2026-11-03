// Escape text before putting it into innerHTML so a vehicle name or similar can never run as a script.
const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = n => CONFIG.CURRENCY + Number(n).toLocaleString();
const daysBetween = (a, b) => Math.max(1, Math.round((new Date(b) - new Date(a)) / 864e5));
const STATUS = {
  AVAILABLE: 'bg-teal/10 text-teal', RESERVED: 'bg-line/40 text-asphalt', PENDING: 'bg-line/40 text-asphalt', APPROVED: 'bg-teal/10 text-teal', ACTIVE: 'bg-teal/10 text-teal',
  RENTED: 'bg-asphalt/10 text-asphalt', MAINTENANCE: 'bg-brick/10 text-brick', CANCELLED: 'bg-brick/10 text-brick', COMPLETED: 'bg-asphalt/10 text-asphalt',
  SUSPENDED: 'bg-brick/10 text-brick', REJECTED: 'bg-brick/10 text-brick', VERIFIED: 'bg-teal/10 text-teal', INACTIVE: 'bg-brick/10 text-brick',
  PAID: 'bg-teal/10 text-teal', FAILED: 'bg-brick/10 text-brick', REFUNDED: 'bg-asphalt/10 text-asphalt', ASSESSED: 'bg-line/40 text-asphalt', DISPUTED: 'bg-brick/10 text-brick', WAIVED: 'bg-asphalt/10 text-asphalt'
};
const dt = v => v ? String(v).replace('T', ' ').slice(0, 16) : '—';
const badge = s => !s ? '<span class="text-asphalt/50">—</span>' : `<span class="rounded px-2 py-0.5 text-xs font-semibold ${STATUS[s] || ''}">${s.charAt(0) + s.slice(1).toLowerCase()}</span>`;

function toast(msg, bad) {
  const t = document.createElement('div');
  t.setAttribute('role', 'status');
  t.className = `fixed bottom-6 left-1/2 -translate-x-1/2 z-50 rounded px-4 py-3 text-sm font-semibold shadow-lg ${bad ? 'bg-brick text-white' : 'bg-asphalt text-white'}`;
  t.textContent = msg;
  document.body.appendChild(t);
  setTimeout(() => t.remove(), 3500);
}

function layout() {
  const u = Auth.user();
  const link = (h, t) => `<a href="${h}" class="hover:text-line focus-visible:outline focus-visible:outline-2 focus-visible:outline-line">${t}</a>`;
  document.getElementById('nav').innerHTML = `
  <header class="bg-asphalt text-white">
    <nav class="mx-auto flex max-w-6xl items-center justify-between px-5 py-4" aria-label="Main">
      <a href="index.html" class="font-display text-xl font-extrabold tracking-tight">Byahe<span class="text-line">Rentals</span></a>
      <div class="flex flex-wrap items-center justify-end gap-x-5 gap-y-2 text-sm font-semibold">
        ${u && ['STAFF', 'FINANCE', 'ADMIN', 'SYSTEM_ADMIN'].includes(u.role) ? link('overview.html', 'Dashboard') : ''}
        ${link('vehicles.html', 'Browse cars')}
        ${u && u.role === 'CUSTOMER' ? link('dashboard.html', 'My rentals') : ''}
        ${u && u.role === 'CUSTOMER' ? link('payments.html', 'Payments') : ''}
        ${u && u.role === 'CUSTOMER' ? link('my-damages.html', 'Damage fees') : ''}
        ${u && u.role === 'CUSTOMER' ? link('profile.html', 'My profile') : ''}
        ${u && ['STAFF', 'ADMIN', 'SYSTEM_ADMIN'].includes(u.role) ? link('customers.html', 'Customers') : ''}
        ${u && ['STAFF', 'ADMIN', 'SYSTEM_ADMIN'].includes(u.role) ? link('licenses.html', 'Licenses') : ''}
        ${u && ['STAFF', 'FINANCE', 'ADMIN', 'SYSTEM_ADMIN'].includes(u.role) ? link('bookings.html', 'Bookings') : ''}
        ${u && ['STAFF', 'FINANCE', 'ADMIN', 'SYSTEM_ADMIN'].includes(u.role) ? link('transactions.html', 'Transactions') : ''}
        ${u && ['STAFF', 'FINANCE', 'ADMIN', 'SYSTEM_ADMIN'].includes(u.role) ? link('damages.html', 'Damage') : ''}
        ${u && ['STAFF', 'FINANCE', 'ADMIN', 'SYSTEM_ADMIN'].includes(u.role) ? link('admin.html', 'Fleet') : ''}
        ${u && ['ADMIN', 'SYSTEM_ADMIN'].includes(u.role) ? link('audit.html', 'Audit log') : ''}
        ${u ? `<button onclick="Auth.logout()" class="rounded border border-white/30 px-3 py-1.5 hover:bg-white/10">Log out</button>`
            : `<a href="login.html" class="rounded bg-line px-3 py-1.5 text-asphalt hover:brightness-95">Log in</a>`}
      </div>
    </nav>
  </header>`;
  document.getElementById('footer').innerHTML = `
  <footer class="mt-16 bg-asphalt py-8 text-sm text-white/70"><div class="mx-auto max-w-6xl px-5">
    © ${new Date().getFullYear()} Byahe Rentals. ${CONFIG.USE_MOCK ? 'Running on sample data.' : ''}
  </div></footer>`;
}

function vehicleCard(v, action) {
  const off = ['MAINTENANCE', 'RETIRED'].includes(v.status);
  return `<article data-view="${v.id}" tabindex="0" role="button" aria-label="View details for ${esc(v.make)} ${esc(v.model)}" class="flex cursor-pointer flex-col rounded-lg border border-asphalt/15 bg-white p-5 hover:border-teal focus-visible:outline focus-visible:outline-2 focus-visible:outline-teal ${off ? 'opacity-60' : ''}">
    <div class="mb-4 flex h-28 items-center justify-center rounded bg-concrete font-display text-4xl font-extrabold text-asphalt/20">${esc(v.category)}</div>
    <div class="flex items-start justify-between gap-2">
      <h3 class="font-display text-lg font-bold">${esc(v.make)} ${esc(v.model)}</h3>${badge(v.status)}
    </div>
    <p class="mt-1 text-sm text-asphalt/70">${esc(v.year)} · ${esc(v.seats)} seats · ${esc(v.fuel || v.transmission)}</p>
    <p class="mt-1 text-xs font-semibold text-teal">View details</p>
    <div class="mt-auto flex items-end justify-between pt-5">
      <p><span class="font-display text-2xl font-extrabold">${money(v.dailyRate)}</span><span class="text-sm text-asphalt/70"> per day</span></p>
      ${action && !off ? `<button data-id="${v.id}" class="book rounded bg-asphalt px-4 py-2 text-sm font-semibold text-white hover:bg-teal">Reserve</button>` : ''}
    </div></article>`;
}
