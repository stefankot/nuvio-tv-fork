// Panel „Nuvio Rodzina”: klucz rodziny tylko w tej przeglądarce; serwer widzi wyłącznie szyfrogram.
import { unzip, zip, decodePrefs, encodePrefs, decodeXml, encodeXml, same } from './codec.js';

const API = '../api.php';
const $ = s => document.querySelector(s);
const app = $('#app');
const te = new TextEncoder(), td = new TextDecoder();
const h = (s, ...v) => s.reduce((a, x, i) => a + x + (i < v.length ? String(v[i]).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]) : ''), '');

function toast(msg) { const t = $('#toast'); t.textContent = msg; t.classList.add('on'); clearTimeout(toast.t); toast.t = setTimeout(() => t.classList.remove('on'), 4000); }
async function api(a, body, q = '') {
  const r = await fetch(`${API}?a=${a}${q}`, body ? { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body), credentials: 'same-origin' } : { credentials: 'same-origin' });
  let j = {}; try { j = await r.json(); } catch {}
  return { status: r.status, ...j };
}

// ---------- kryptografia (zgodna z FamilyCrypto.kt) ----------
const b64 = b => btoa(String.fromCharCode(...new Uint8Array(b)));
const unb64 = s => Uint8Array.from(atob(s), c => c.charCodeAt(0));
async function sha256hex(s) { return [...new Uint8Array(await crypto.subtle.digest('SHA-256', te.encode(s)))].map(x => x.toString(16).padStart(2, '0')).join(''); }
async function codeKey(code) {
  const base = await crypto.subtle.importKey('raw', te.encode(code), 'PBKDF2', false, ['deriveKey']);
  return crypto.subtle.deriveKey({ name: 'PBKDF2', hash: 'SHA-256', salt: te.encode('nuvio-pair-key:v1'), iterations: 200000 }, base, { name: 'AES-GCM', length: 256 }, false, ['encrypt', 'decrypt']);
}
const aesKey = raw => crypto.subtle.importKey('raw', raw, 'AES-GCM', false, ['encrypt', 'decrypt']);
async function enc(key, plain) { const iv = crypto.getRandomValues(new Uint8Array(12)); const c = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, plain); const o = new Uint8Array(12 + c.byteLength); o.set(iv); o.set(new Uint8Array(c), 12); return o; }
async function dec(key, data) { return new Uint8Array(await crypto.subtle.decrypt({ name: 'AES-GCM', iv: data.subarray(0, 12) }, key, data.subarray(12))); }

// ---------- IndexedDB: nieeksportowalny klucz przeglądarki + zaszyfrowane dane rodziny ----------
function idb() { return new Promise((ok, no) => { const r = indexedDB.open('nuvio-rodzina', 1); r.onupgradeneeded = () => r.result.createObjectStore('kv'); r.onsuccess = () => ok(r.result); r.onerror = () => no(r.error); }); }
async function kv(op, k, v) { const d = await idb(); return new Promise((ok, no) => { const t = d.transaction('kv', op === 'get' ? 'readonly' : 'readwrite').objectStore('kv'); const r = op === 'get' ? t.get(k) : op === 'del' ? t.delete(k) : t.put(v, k); r.onsuccess = () => ok(r.result); r.onerror = () => no(r.error); }); }
async function saveFamily(f) {
  let wrap = await kv('get', 'wrap');
  if (!wrap) { wrap = await crypto.subtle.generateKey({ name: 'AES-GCM', length: 256 }, false, ['encrypt', 'decrypt']); await kv('put', 'wrap', wrap); }
  await kv('put', 'family', await enc(wrap, te.encode(JSON.stringify(f))));
}
async function loadFamily() {
  const wrap = await kv('get', 'wrap'), box = await kv('get', 'family');
  if (!wrap || !box) return null;
  try { return JSON.parse(td.decode(await dec(wrap, box))); } catch { return null; }
}

let family = null; // {id, key, owner}
let snap = null;
let revoked = null; // nazwy kluczy do zmiany u dostawców po ostatnim odebraniu dostępu   // {version, updated, src, files: [{name, entry, kind, prefs, orig}]}

// ---------- ekrany ----------
async function start() {
  const me = await api('me');
  if (me.status !== 200) return loginView();
  $('#nav').hidden = false;
  family = await loadFamily();
  route(family ? 'home' : 'link');
}
document.addEventListener('click', e => { const b = e.target.closest('[data-go]'); if (b) route(b.dataset.go); });
function route(r) { ({ home: homeView, settings: settingsView, link: linkView })[r](); }

function loginView() {
  app.innerHTML = `<h2>Zaloguj się</h2><p>Wyślę link logowania na adres właściciela.</p>
  <form id="f" class="row"><input id="email" type="email" autocomplete="email" placeholder="Twój adres e-mail" required><button class="primary">Wyślij link</button></form>`;
  $('#f').onsubmit = async e => {
    e.preventDefault();
    const r = await api('login_start', { email: $('#email').value });
    app.innerHTML = r.status === 200 ? '<h2>Sprawdź pocztę</h2><p>Jeśli adres się zgadza, link już leci (ważny 15 minut). Sprawdź też folder „Spam”.</p>'
      : r.status === 429 ? '<p>Za dużo prób. Spróbuj za godzinę.</p>' : '<p>Nie udało się wysłać poczty. Spróbuj za chwilę.</p>';
  };
}

function linkView() {
  app.innerHTML = `<h2>Połącz z telewizorem</h2>
  ${family ? `<p>Ta przeglądarka jest połączona z rodziną.</p><div class="row"><button id="unlink">Odłącz tę przeglądarkę</button><button id="logout">Wyloguj</button></div><h3>Połącz ponownie</h3>` : ''}
  <p>Na swoim telewizorze: Ustawienia → Rodzina → „Połącz panel w przeglądarce”. Wpisz pokazany kod.</p>
  <form id="f" class="row"><input id="code" class="code" inputmode="numeric" autocomplete="one-time-code" maxlength="6" pattern="[0-9]{6}" required aria-label="Kod 6 cyfr"><button class="primary">Połącz</button></form><p id="msg" class="muted"></p>`;
  $('#f').onsubmit = async e => {
    e.preventDefault();
    const code = $('#code').value.replace(/\D/g, '');
    $('#msg').textContent = 'Sprawdzam kod…';
    const r = await api('panel_redeem', { lookup: await sha256hex('nuvio-panel:v1:' + code) });
    if (r.status !== 200) { $('#msg').textContent = r.status === 429 ? 'Za dużo prób, spróbuj za godzinę.' : 'Zły albo wygasły kod. Pokaż nowy na telewizorze.'; return; }
    try {
      family = JSON.parse(td.decode(await dec(await codeKey(code), unb64(r.wrapped))));
      family.epoch = family.epoch || 0; family.keys = { [family.epoch]: family.key };
      await saveFamily(family); snap = null; toast('Połączono z rodziną'); route('home');
    } catch { $('#msg').textContent = 'Kod nie pasuje. Pokaż nowy na telewizorze.'; }
  };
  $('#unlink')?.addEventListener('click', async () => { await api('panel_unlink', { id: family.id }); await kv('del', 'family'); family = null; snap = null; toast('Przeglądarka odłączona'); linkView(); });
  $('#logout')?.addEventListener('click', async () => { await api('logout', {}); location.reload(); });
}

const ago = (now, t) => { const m = Math.round((now - t) / 60); return m < 2 ? 'przed chwilą' : m < 60 ? `${m} min temu` : m < 2880 ? `${Math.round(m / 60)} godz. temu` : `${Math.round(m / 1440)} dni temu`; };
const hhmm = t => new Date(t * 1000).toLocaleString('pl-PL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });

async function homeView() {
  if (!family) return linkView();
  app.innerHTML = '<p class="muted">Wczytuję telewizory…</p>';
  const r = await api('devices', null, '&id=' + family.id);
  if (r.status === 403) { app.innerHTML = '<p>Ta rodzina nie jest już połączona z panelem. Połącz ponownie.</p>'; return; }
  const evs = (r.events || []).map(e => ({ ...e, ...JSON.parse(e.ev) }));
  const devs = r.devices || [];
  const cards = devs.map(d => {
    const info = JSON.parse(d.info || '{}'), mine = evs.filter(e => e.device === d.device);
    const errs = mine.filter(e => e.k === 'err' || e.k === 'crash' || e.k === 'bad'), day = r.now - 86400;
    const recentErr = errs.filter(e => e.ts > day).length, offline = r.now - d.last_seen > 3 * 86400;
    const cls = offline ? '' : recentErr ? 'bad' : 'ok';
    const state = offline ? 'Dawno offline' : recentErr ? `Ma problem (${recentErr} w 24 godz.)` : 'Działa';
    const plays = mine.filter(e => e.k === 'play' && e.ms > 0).slice(0, 30).reverse(), max = Math.max(1, ...plays.map(p => p.ms));
    const avg = plays.length ? (plays.reduce((s, p) => s + p.ms, 0) / plays.length / 1000).toFixed(1) : null;
    return `<article class="tv ${cls}">
      <h3>${h`${d.name || info.model || 'Telewizor'}`}</h3>
      <p class="state">${state}</p>
      <p class="muted">${h`${info.model || ''} · Android ${info.android || '?'} · ${info.app || ''}`}<br>Widziany ${ago(r.now, d.last_seen)}</p>
      ${avg ? `<p>Start filmu średnio ${avg} s</p><div class="bars" aria-label="Czasy startu ostatnich filmów">${plays.map(p => `<span style="height:${Math.max(4, p.ms / max * 100)}%" title="${(p.ms / 1000).toFixed(1)} s"></span>`).join('')}</div>` : '<p class="muted">Brak odtworzeń</p>'}
      ${errs.slice(0, 3).map(e => `<p class="muted">${hhmm(e.ts)}: ${h`${label(e)}`}</p>`).join('')}
      <div class="row"><button data-name="${d.device}">Zmień nazwę</button><button data-forget="${d.device}">Usuń z listy</button><button data-remove="${d.device}">Odbierz dostęp</button></div>
    </article>`;
  }).join('');
  // problemy wspólne: to samo źródło zawodzi na kilku telewizorach
  const bySrc = {};
  for (const e of evs) if ((e.k === 'err' || e.k === 'bad') && e.src) (bySrc[e.src] ??= new Set()).add(e.device);
  const problems = Object.entries(bySrc).filter(([, s]) => s.size >= 1).sort((a, b) => b[1].size - a[1].size)
    .map(([src, s]) => `<li>Źródło „${h`${src}`}” nie startuje na ${s.size} ${s.size === 1 ? 'telewizorze' : 'telewizorach'}. ${s.size > 1 ? 'Rozważ wyłączenie tego dodatku w Ustawieniach.' : 'Jeśli się powtarza, wyłącz ten dodatek.'}</li>`).join('');
  const crashes = evs.filter(e => e.k === 'crash').length;
  const notice = revoked ? `<details open><summary>Odebrano dostęp: co jeszcze zrobić</summary><div class="pref" style="display:block">
    <p>Odłączony telewizor skasuje ustawienia rodziny (klucze do serwisów, konto, dodatki), gdy tylko połączy się z internetem z aktualną aplikacją.
    Jeśli był offline, ma starą wersję albo nie ufasz osobie, zmień u dostawców te klucze i wpisz nowe w Ustawieniach → Klucze do serwisów:</p>
    ${revoked.length ? `<ul>${revoked.map(k => `<li>${h`${k}`}</li>`).join('')}</ul>` : '<p class="muted">Nie znalazłem kluczy w ustawieniach.</p>'}
    <p>Konto Nuvio: zmień hasło na stronie konta, żeby wylogować wszystkie urządzenia.</p>
    <div class="row"><button id="revokedOk">Zrobione</button></div></div></details>` : '';
  app.innerHTML = notice + `<h2>Telewizory rodziny</h2>
    ${devs.length ? `<div class="tvs">${cards}</div>` : '<p>Żaden telewizor jeszcze się nie zgłosił. Telewizory zgłaszają się po aktualizacji aplikacji, przy starcie i co 15 minut.</p>'}
    <h2>Problemy (30 dni)</h2>
    ${problems || crashes ? `<ul class="problems">${problems}${crashes ? `<li>Aplikacja zamknęła się z błędem ${crashes} razy. Jeśli to się powtarza, zaktualizuj aplikację.</li>` : ''}</ul>` : '<p>Bez problemów.</p>'}
    <div class="row"><button id="clear">Wyczyść statystyki</button></div>`;
  app.querySelectorAll('[data-name]').forEach(b => b.onclick = async () => {
    const n = prompt('Nazwa telewizora (np. „Mama, salon”)'); if (n == null) return;
    await api('device_name', { id: family.id, device: b.dataset.name, name: n }); homeView();
  });
  app.querySelectorAll('[data-forget]').forEach(b => b.onclick = async () => {
    if (!confirm('Usunąć ten telewizor z listy i jego statystyki? (Ustawienia na nim zostają; wróci na listę, jeśli znów się zgłosi.)')) return;
    await api('device_forget', { id: family.id, device: b.dataset.forget }); homeView();
  });
  $('#revokedOk')?.addEventListener('click', () => { revoked = null; homeView(); });
  app.querySelectorAll('[data-remove]').forEach(b => b.onclick = () => removeFromFamily(b.dataset.remove, devs));
  $('#clear').onclick = async () => { if (confirm('Wyczyścić wszystkie statystyki?')) { await api('events_clear', { id: family.id }); homeView(); } };
}
function label(e) {
  if (e.k === 'err') return `błąd odtwarzania${e.src ? ' (' + e.src + ')' : ''}: ${e.msg || ''}`;
  if (e.k === 'bad') return `złe źródło odłożone${e.src ? ' (' + e.src + ')' : ''}: ${e.reason || ''}`;
  if (e.k === 'crash') return `aplikacja zamknęła się z błędem (${e.cls || ''})`;
  return e.k;
}

// ---------- ustawienia ----------
const GROUPS = [
  ['Moduły forka', /fork_features|forge|kicia|reshaped/i],
  ['Klucze do serwisów', null],
  ['Konto i profile', /account|auth|profile|session|user|trakt|supabase/i],
  ['Dodatki', /addon|plugin|catalog/i],
  ['Źródła i Live TV', /live_tv|iptv|epg|source|stream|torrent|debrid/i],
  ['Napisy i lektor', /subtitle|lektor|tts|voice|asr|whisper/i],
  ['Odtwarzanie', /player|playback|audio|video|decoder/i],
  ['Wygląd', /theme|layout|skin|home|appear|locale|ui/i],
  ['Inne', /./],
];
const SECRET = /api_?key|token|secret|password|auth|cookie|session/i;
const nice = k => k.replace(/^fork_|^pref_/, '').replace(/([a-z])([A-Z])/g, '$1 $2').replace(/[_.-]+/g, ' ').replace(/^./, c => c.toUpperCase());

async function loadSnap() {
  const r = await api('get', null, '&id=' + family.id);
  if (r.status !== 200 || !r.data) throw new Error(r.status === 404 ? 'Rodzina nie udostępnia ustawień.' : 'Brak połączenia z serwerem.');
  const entries = await unzip(await dec(await aesKey(unb64(family.key)), unb64(r.data)));
  const files = entries.map(entry => {
    let kind = 'raw', prefs = null;
    try {
      if (entry.name.endsWith('.preferences_pb')) { prefs = decodePrefs(entry.data); kind = same(encodePrefs(prefs), entry.data) ? 'pb' : 'ro'; }
      else if (entry.name.endsWith('.xml')) { prefs = decodeXml(entry.data); kind = 'xml'; }
    } catch { kind = 'ro'; }
    return { name: entry.name, entry, kind, prefs, orig: prefs && JSON.stringify(prefs) };
  });
  snap = { version: r.version, updated: r.updated, src: r.src, files };
}

async function settingsView() {
  if (!family) return linkView();
  app.innerHTML = '<p class="muted">Odszyfrowuję ustawienia…</p>';
  try { if (!snap) await loadSnap(); } catch (e) { app.innerHTML = `<p>${h`${e.message}`}</p>`; return; }
  const adv = settingsView.adv || false;
  const items = [];
  snap.files.forEach((f, fi) => (f.kind === 'pb' || f.kind === 'xml') && f.prefs.forEach((p, pi) => {
    if (p.type === 'bytes' || p.type === 'null') return;
    if (!adv && typeof p.value === 'string' && p.value.length > 200) return; // duże bloki JSON tylko w trybie zaawansowanym
    const secret = SECRET.test(p.key);
    const group = secret ? 'Klucze do serwisów' : GROUPS.find(([, re]) => re && re.test(f.name + ' ' + p.key))[0];
    items.push({ f, fi, p, pi, group, secret });
  }));
  const file = n => n.replace(/^.*\//, '').replace(/\.(preferences_pb|xml)$/, '');
  const field = ({ fi, pi, p, secret }) => {
    const id = `p${fi}_${pi}`;
    const input = p.type === 'bool' ? `<input type="checkbox" id="${id}" ${p.value ? 'checked' : ''}>`
      : p.type === 'set' ? `<input id="${id}" value="${h`${p.value.join(', ')}`}">`
      : `<input id="${id}" ${secret ? 'type="password" autocomplete="off"' : ''} ${['int', 'long', 'float', 'double'].includes(p.type) ? 'inputmode="decimal"' : ''} value="${h`${p.value}`}">`;
    return `<div class="pref"><label for="${id}">${h`${nice(p.key)}`}${adv ? `<small>${h`${file(snap.files[fi].name)} · ${p.key} · ${p.type}`}</small>` : ''}</label><div class="row">${input}${secret ? `<button data-show="${id}">Pokaż</button>` : ''}</div></div>`;
  };
  const groups = GROUPS.map(([g]) => [g, items.filter(i => i.group === g)]).filter(([, l]) => l.length);
  const ro = snap.files.filter(f => f.kind === 'ro').length;
  app.innerHTML = `<h2>Ustawienia rodziny</h2>
    <p class="muted">Wspólne dla wszystkich telewizorów rodziny. Wersja ${snap.version}, ostatnia zmiana ${snap.src === 'panel' ? 'w panelu' : 'na telewizorze'} ${hhmm(snap.updated)}.${ro ? ` ${ro} plików tylko do odczytu (nieznany format, zostają bez zmian).` : ''}</p>
    <div class="row"><label><input type="checkbox" id="adv" ${adv ? 'checked' : ''}> Zaawansowane (surowe nazwy)</label><button id="reload">Wczytaj ponownie</button></div>
    ${groups.map(([g, l]) => `<details><summary>${g} (${l.length})</summary>${l.map(field).join('')}</details>`).join('')}
    <div class="save row"><button class="primary" id="send">Wyślij na telewizory</button><span id="msg" class="muted"></span></div>`;
  $('#adv').onchange = e => { settingsView.adv = e.target.checked; settingsView(); };
  $('#reload').onclick = () => { snap = null; settingsView(); };
  app.querySelectorAll('[data-show]').forEach(b => b.onclick = () => { const i = $('#' + b.dataset.show); i.type = i.type === 'password' ? 'text' : 'password'; b.textContent = i.type === 'password' ? 'Pokaż' : 'Ukryj'; });
  app.addEventListener('input', e => e.target.closest('.pref')?.classList.add('changed'));
  $('#send').onclick = () => send(items);
}

async function send(items) {
  const msg = $('#msg');
  for (const { fi, pi, p } of items) {
    const el = $(`#p${fi}_${pi}`); if (!el) continue;
    if (p.type === 'bool') p.value = el.checked;
    else if (p.type === 'set') p.value = el.value.split(',').map(s => s.trim()).filter(Boolean);
    else if (['int', 'long', 'float', 'double'].includes(p.type)) {
      const v = el.value.trim();
      if (!/^-?\d+([.,]\d+)?$/.test(v) || ((p.type === 'int' || p.type === 'long') && /[.,]/.test(v))) { msg.textContent = `„${nice(p.key)}”: wpisz liczbę${p.type === 'int' || p.type === 'long' ? ' całkowitą' : ''}.`; el.focus(); return; }
      p.value = p.type === 'long' ? v : Number(v.replace(',', '.'));
    } else p.value = el.value;
  }
  // Lista zmian (plik, klucz, wartość): przy kolizji z telewizorem nakładana na nowszą wersję.
  const edits = [];
  for (const f of snap.files) {
    if (!f.prefs) continue;
    const orig = JSON.parse(f.orig);
    f.prefs.forEach((p, i) => { if (JSON.stringify(p) !== JSON.stringify(orig[i])) edits.push({ file: f.name, ...p }); });
  }
  if (!edits.length) { msg.textContent = 'Nic nie zmieniono.'; return; }
  msg.textContent = 'Szyfruję i wysyłam…';
  let r = await upload(edits), merged = false;
  if (r.status === 409) {
    // Telewizor wysłał w międzyczasie nowsze ustawienia: wczytaj je i nałóż tylko zmiany z panelu (scalanie).
    try { await loadSnap(); } catch (e) { msg.textContent = e.message; return; }
    r = await upload(edits); merged = true;
  }
  if (r.status !== 200) { msg.textContent = r.status === 409 ? 'Telewizor właśnie wysyła ustawienia. Spróbuj za minutę.' : 'Nie udało się wysłać (serwer: ' + (r.error || r.status) + '). Spróbuj ponownie.'; return; }
  const n = (await api('devices', null, '&id=' + family.id)).devices?.length || 0;
  snap = null; toast(`Wysłano${merged ? ' (połączone z nowszymi zmianami z telewizora)' : ''}. Telewizory pobiorą zmiany w ciągu 15 minut${n ? ` (${n} na liście)` : ''}.`); settingsView();
}

async function upload(edits) {
  for (const e of edits) {
    const f = snap.files.find(x => x.name === e.file && x.prefs); if (!f) continue;
    const p = f.prefs.find(x => x.key === e.key);
    if (p) { p.value = e.value; p.type = e.type; } else f.prefs.push({ key: e.key, type: e.type, value: e.value });
  }
  const entries = snap.files.map(f => f.prefs && JSON.stringify(f.prefs) !== f.orig
    ? { name: f.name, data: f.kind === 'pb' ? encodePrefs(f.prefs) : encodeXml(f.prefs) }
    : f.entry); // nieruszone: bajt w bajt
  const data = b64(await enc(await aesKey(unb64(family.key)), await zip(entries)));
  return api('put', { id: family.id, owner: family.owner, data, base: snap.version, src: 'panel', epoch: family.epoch || 0 });
}

// ---------- odebranie dostępu: nowy klucz rodziny dla wszystkich oprócz usuniętego ----------
async function removeFromFamily(dev, devs) {
  if (!confirm('Odebrać temu telewizorowi dostęp? Przestanie dostawać ustawienia, a po połączeniu z internetem sam skasuje klucze do serwisów, konto i dodatki od rodziny. Pozostałe telewizory dostaną nowy klucz automatycznie.')) return;
  const r = await api('get', null, '&id=' + family.id);
  if (r.status !== 200 || !r.data) { toast('Brak połączenia z serwerem.'); return; }
  if ((r.epoch || 0) !== (family.epoch || 0)) { toast('Klucz w tej przeglądarce jest nieaktualny. Połącz panel ponownie kodem z telewizora.'); return; }
  const plain = await dec(await aesKey(unb64(family.key)), unb64(r.data));
  const newRaw = crypto.getRandomValues(new Uint8Array(32)), newKey = b64(newRaw), epoch = (family.epoch || 0) + 1;
  const keys = family.keys || { [family.epoch || 0]: family.key };
  const wraps = {}, lost = [];
  for (const d of devs) {
    if (d.device === dev) continue;
    try {
      const { dkey } = JSON.parse(td.decode(await dec(await aesKey(unb64(keys[d.box_epoch])), unb64(d.box))));
      wraps[d.device] = b64(await enc(await aesKey(unb64(dkey)), te.encode(JSON.stringify({ key: newKey, epoch }))));
    } catch { lost.push(d.name || JSON.parse(d.info || '{}').model || d.device.slice(0, 6)); }
  }
  if (lost.length && !confirm(`Te telewizory też stracą dostęp (stara wersja aplikacji albo dawno się nie zgłaszały): ${lost.join(', ')}. Później połączysz je nowym kodem. Kontynuować?`)) return;
  const res = await api('rotate', { id: family.id, owner: family.owner, data: b64(await enc(await aesKey(newRaw), plain)), epoch, wraps, remove: [dev], base: r.version });
  if (res.status !== 200) { toast(res.status === 409 ? 'Telewizor właśnie wysłał zmiany. Spróbuj ponownie.' : 'Nie udało się (serwer: ' + (res.error || res.status) + ').'); return; }
  family.key = newKey; family.epoch = epoch; family.keys = { ...keys, [epoch]: newKey }; await saveFamily(family);
  // Klucze do serwisów, które usunięty telewizor już ma: sam je skasuje po połączeniu z internetem, ale pewność daje
  // tylko zmiana klucza u dostawcy (telewizor mógł być offline albo mieć starą aplikację).
  revoked = [];
  try {
    for (const e of await unzip(plain)) {
      let prefs = null;
      try { prefs = e.name.endsWith('.preferences_pb') ? decodePrefs(e.data) : e.name.endsWith('.xml') ? decodeXml(e.data) : null; } catch {}
      for (const p of prefs || []) if (SECRET.test(p.key) && typeof p.value === 'string' && p.value.length >= 8 && p.value.length < 400) revoked.push(nice(p.key));
    }
  } catch {}
  revoked = [...new Set(revoked)];
  snap = null; toast('Dostęp odebrany. Pozostałe telewizory dostaną nowy klucz w ciągu 15 minut.'); homeView();
}

start();
