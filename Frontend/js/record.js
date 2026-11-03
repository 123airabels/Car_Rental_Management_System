// Draws one rental's full transaction record (see GET /rentals/{id}/record and /admin/rentals/{id}/record).
const day10 = v => v ? String(v).slice(0, 10) : '—';
function renderRecord(r) {
  const row = (k, v) => `<div class="flex justify-between gap-4 py-1.5"><dt class="text-asphalt/70">${k}</dt><dd class="text-right font-semibold">${v}</dd></div>`;
  const card = (title, body) => `<section class="rounded-lg bg-white p-6"><h2 class="font-display text-lg font-bold">${title}</h2><dl class="mt-3 divide-y divide-asphalt/10 text-sm">${body}</dl></section>`;
  const m = v => v == null ? '—' : money(v);
  const km = v => v == null ? '—' : Number(v).toLocaleString() + ' km';
  const x = v => esc(v ?? '—');
  const c = r.cost, d = r.dates, t = r.terms, k = r.cancellation;

  const booking = card('Booking',
    row('Booking number', '#' + Number(r.id)) + row('Status', badge(r.status)) + row('Booked on', esc(dt(r.createdAt)))
    + row('Approved on', esc(dt(r.approvedAt))) + row('Customer', x(r.customer.name) + '<br><span class="font-normal text-asphalt/60">' + x(r.customer.email) + '</span>')
    + row('Vehicle', x(r.vehicle.name) + (r.vehicle.plateNumber ? ' (' + esc(r.vehicle.plateNumber) + ')' : '')));

  const dates = card('Rental dates',
    row('Pickup', esc(day10(d.pickup))) + row('Return due', esc(day10(d.returnBy))) + row('Length', d.days + (d.days === 1 ? ' day' : ' days')));

  const hand = (h) => row('Scheduled', esc(day10(h.scheduled))) + row('Actual', h.actual ? esc(dt(h.actual)) : '<span class="font-normal text-asphalt/60">Not yet</span>')
    + (h.actual ? row('Mileage', km(h.mileage)) + row('Fuel level', x(h.fuelLevel)) + row('Condition notes', x(h.notes)) : '');
  const pickup = card('Pickup', hand(r.pickup));
  const ret = card('Return', hand(r.returned) + (Number(c.lateFee) > 0 ? row('Returned late', '<span class="text-brick">late fee applied</span>') : ''));

  const cost = card('Cost',
    row('Daily rate', m(c.dailyRate)) + row('Estimated cost', m(c.estimated))
    + (Number(c.pickupFee) > 0 ? row('Pickup fee', m(c.pickupFee)) : '') + (Number(c.returnFee) > 0 ? row('Return fee', m(c.returnFee)) : '')
    + (Number(c.lateFee) > 0 ? row('Late fee', m(c.lateFee) + (c.lateFeeStatus ? ' (' + esc(c.lateFeeStatus.toLowerCase()) + ')' : '')) : '')
    + row('Final cost', c.finalCost == null ? '<span class="font-normal text-asphalt/60">Set when the car is returned</span>' : m(c.finalCost))
    + row('Paid so far', m(c.paid)));

  const terms = card('Terms and conditions',
    row('Version', x(t.version)) + row('Accepted', t.accepted ? '<span class="text-teal">Yes, ' + esc(dt(t.acceptedAt)) + '</span>' : '<span class="text-brick">Not yet accepted</span>')
    + `<details class="pt-3"><summary class="cursor-pointer font-semibold text-teal">Read the terms</summary><p class="mt-2 whitespace-pre-line font-normal">${esc(t.text)}</p></details>`);

  const cancel = !k ? '' : card(k.type === 'REJECTED' ? 'Rejection details' : 'Cancellation details',
    row('Outcome', badge(k.type)) + row(k.type === 'REJECTED' ? 'Rejected by' : 'Cancelled by', x(k.by)) + row('When', esc(dt(k.at))) + row('Reason', x(k.reason))
    + row('Refund', k.refundEligible ? m(k.refundAmount) + (k.refundStatus ? ' (' + esc(k.refundStatus.toLowerCase()) + ')' : '') : 'None'));

  return `<div class="grid gap-5 md:grid-cols-2">${booking}${dates}${pickup}${ret}${cost}${terms}${cancel}</div>`;
}
