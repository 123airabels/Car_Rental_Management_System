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
// Payment indicator for a booking row (staff pages): works out Paid / Awaiting confirmation / Partly paid / Unpaid / Overdue from the amounts.
// Returns { key, label, cls, left } so pages can show it and filter on it.
function payState(r) {
  const none = (key, label) => ({ key, label, cls: 'bg-asphalt/10 text-asphalt/60', left: 0 });
  if (['CANCELLED', 'REJECTED'].includes(r.status)) return none('none', 'No payment');
  if (r.status === 'PENDING') return none('notdue', 'Not due yet');
  const total = Number(r.finalCost ?? r.estimatedCost ?? 0), paid = Number(r.paid || 0), pending = Number(r.pending || 0), left = Math.max(0, total - paid);
  if (left <= 0) return { key: 'paid', label: 'Paid', cls: 'bg-teal/10 text-teal', left: 0 };
  if (pending >= left) return { key: 'confirm', label: 'Awaiting confirmation', cls: 'bg-line/40 text-asphalt', left };
  const late = r.paymentDueAt && new Date(r.paymentDueAt) < new Date();
  if (late) return { key: 'overdue', label: 'Overdue', cls: 'bg-brick text-white', left };
  if (paid > 0) return { key: 'partial', label: 'Partly paid', cls: 'bg-line/40 text-asphalt', left };
  return { key: 'unpaid', label: 'Unpaid', cls: 'bg-brick/10 text-brick', left };
}
const PAY_FILTERS = [['', 'All payments'], ['unpaid', 'Unpaid'], ['overdue', 'Overdue'], ['partial', 'Partly paid'], ['confirm', 'Awaiting confirmation'], ['paid', 'Paid']];
function payBadge(r) {
  const s = payState(r);
  const sub = s.left > 0 ? `<br><span class="text-xs text-asphalt/60">${money(s.left)} left${r.paymentDueAt && ['unpaid', 'partial', 'overdue'].includes(s.key) ? ' · by ' + esc(dt(r.paymentDueAt)) : ''}</span>` : '';
  return `<span class="rounded px-2 py-0.5 text-xs font-semibold ${s.cls}">${s.label}</span>${sub}`;
}
// Opens a payment's proof-of-payment screenshot (it needs the login token, so it is fetched and shown from a blob: URL).
async function viewProof(id) {
  const w = window.open('', '_blank');   // opened first so the browser does not block it as a popup
  try { const u = await API.blobUrl('/payments/' + Number(id) + '/proof'); if (w) w.location = u; else location.href = u; }
  catch (e) { if (w) w.close(); toast(e.message, true); }
}
// Customer side: attaches the screenshot to a payment. Online payments (GCash, Maya, card, bank transfer) need one.
const needsProof = method => method !== 'Cash';
async function sendProof(paymentId, file) {
  const fd = new FormData(); fd.append('photo', file);
  return API.upload('/payments/' + Number(paymentId) + '/proof', fd);
}
// Damage-fee payment state of a booking (staff pages): none / unpaid / awaiting confirmation / paid.
function feeState(r) {
  const total = Number(r.feeTotal || 0), paid = Number(r.feePaid || 0), pending = Number(r.feePending || 0), left = Math.max(0, total - paid);
  if (!(total > 0)) return { key: 'none' };
  if (left <= 0) return { key: 'paid', label: 'Damage fee paid', cls: 'bg-teal/10 text-teal', left: 0 };
  if (pending > 0) return { key: 'confirm', label: 'Damage fee: confirm payment', cls: 'bg-line/40 text-asphalt', left, pending };
  return { key: 'unpaid', label: 'Damage fee unpaid', cls: 'bg-brick/10 text-brick', left };
}
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
  // Sticky footer: on short pages the footer sits at the bottom of the window instead of the middle.
  if (!document.getElementById('stickyFooter')) {
    const st = document.createElement('style'); st.id = 'stickyFooter';
    st.textContent = 'body{min-height:100vh;display:flex;flex-direction:column}'
      + 'body>main,body>section{width:100%}'
      + '#footer{margin-top:auto}';
    document.head.appendChild(st);
  }
  const u = Auth.user();
  const link = (h, t) => `<a href="${h}" class="hover:text-line focus-visible:outline focus-visible:outline-2 focus-visible:outline-line">${t}</a>`;
  document.getElementById('nav').innerHTML = `
  <header class="bg-asphalt text-white">
    <nav class="mx-auto flex max-w-6xl items-center justify-between px-5 py-4" aria-label="Main">
      <a href="index.html" class="font-display text-xl font-extrabold tracking-tight">Byahe<span class="text-line">Rentals</span></a>
      <div class="flex flex-wrap items-center justify-end gap-x-5 gap-y-2 text-sm font-semibold">
        ${u && ['STAFF', 'FINANCE', 'ADMIN'].includes(u.role) ? link('overview.html', 'Dashboard') : ''}
        ${u && ['STAFF', 'FINANCE', 'ADMIN', 'SYSTEM_ADMIN'].includes(u.role) ? '' : link('vehicles.html', 'Browse cars')}
        ${u && u.role === 'CUSTOMER' ? link('dashboard.html', 'My rentals') : ''}
        ${u && u.role === 'CUSTOMER' ? link('payments.html', 'Payments') : ''}
        ${u && u.role === 'CUSTOMER' ? link('my-damages.html', 'Damage fees') : ''}
        ${u && u.role === 'CUSTOMER' ? link('profile.html', 'My profile') : ''}
        ${u && ['STAFF', 'ADMIN'].includes(u.role) ? link('customers.html', 'Customers') : ''}
        ${u && ['STAFF', 'ADMIN'].includes(u.role) ? link('licenses.html', 'Licenses') : ''}
        ${u && ['STAFF', 'FINANCE', 'ADMIN'].includes(u.role) ? link('bookings.html', 'Bookings') : ''}
        ${u && ['STAFF', 'FINANCE', 'ADMIN'].includes(u.role) ? link('transactions.html', 'Transactions') : ''}
        ${u && ['STAFF', 'FINANCE', 'ADMIN'].includes(u.role) ? link('damages.html', 'Damage') : ''}
        ${u && ['STAFF', 'FINANCE', 'ADMIN'].includes(u.role) ? link('admin.html', 'Fleet') : ''}
        ${u && ['ADMIN', 'SYSTEM_ADMIN'].includes(u.role) ? link('staff.html', 'Staff accounts') : ''}
        ${u && ['ADMIN', 'SYSTEM_ADMIN'].includes(u.role) ? link('audit.html', 'Audit log') : ''}
        ${u ? `<button onclick="Auth.logout()" class="rounded border border-white/30 px-3 py-1.5 hover:bg-white/10">Log out</button>`
            : `<a href="login.html" class="rounded border border-white/30 px-3 py-1.5 hover:bg-white/10">Log in</a><a href="login.html?mode=signup" class="rounded bg-line px-3 py-1.5 text-asphalt hover:brightness-95">Sign up</a>`}
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
