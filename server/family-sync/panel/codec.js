// Zrzut ustawień telewizora: ZIP (FamilySnapshot.kt) z plikami DataStore (protobuf) i SharedPreferences (XML).
// Bez zależności: ZIP przez (De)CompressionStream, protobuf PreferencesProto ręcznie. Nieruszone pliki zostają bajt w bajt.

const te = new TextEncoder(), td = new TextDecoder();

// ---------- CRC32 ----------
const CRC = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
export function crc32(b) { let c = 0xffffffff; for (let i = 0; i < b.length; i++) c = CRC[(c ^ b[i]) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; }

async function stream(bytes, ts) { return new Uint8Array(await new Response(new Blob([bytes]).stream().pipeThrough(ts)).arrayBuffer()); }
const inflate = b => stream(b, new DecompressionStream('deflate-raw'));
const deflate = b => stream(b, new CompressionStream('deflate-raw'));

// ---------- ZIP ----------
/** → [{name, data (rozpakowane), raw (skompresowane), method, crc}] według katalogu centralnego. */
export async function unzip(buf) {
  const u = new Uint8Array(buf), v = new DataView(u.buffer, u.byteOffset, u.byteLength);
  let eocd = -1;
  for (let i = u.length - 22; i >= 0; i--) if (v.getUint32(i, true) === 0x06054b50) { eocd = i; break; }
  if (eocd < 0) throw new Error('To nie jest plik ZIP');
  const count = v.getUint16(eocd + 10, true); let p = v.getUint32(eocd + 16, true);
  const out = [];
  for (let n = 0; n < count; n++) {
    if (v.getUint32(p, true) !== 0x02014b50) throw new Error('Uszkodzony ZIP');
    const method = v.getUint16(p + 10, true), crc = v.getUint32(p + 16, true), csize = v.getUint32(p + 20, true);
    const nl = v.getUint16(p + 28, true), xl = v.getUint16(p + 30, true), cl = v.getUint16(p + 32, true), off = v.getUint32(p + 42, true);
    const name = td.decode(u.subarray(p + 46, p + 46 + nl));
    const start = off + 30 + v.getUint16(off + 26, true) + v.getUint16(off + 28, true);
    const raw = u.slice(start, start + csize);
    const data = method === 8 ? await inflate(raw) : raw;
    out.push({ name, data, raw, method, crc });
    p += 46 + nl + xl + cl;
  }
  return out;
}

/** Entries: {name, data, raw?, method?, crc?}; gdy raw jest (plik nieruszony), kopiuje skompresowane bajty. */
export async function zip(entries) {
  const parts = [], central = []; let off = 0;
  for (const e of entries) {
    const name = te.encode(e.name);
    let raw = e.raw, method = e.method ?? 8, crc = e.crc;
    if (!raw) { raw = await deflate(e.data); method = 8; crc = crc32(e.data); }
    const h = new DataView(new ArrayBuffer(30));
    h.setUint32(0, 0x04034b50, true); h.setUint16(4, 20, true); h.setUint16(6, 0x800, true); h.setUint16(8, method, true);
    h.setUint16(10, 0, true); h.setUint16(12, 0x21, true); h.setUint32(14, crc, true);
    h.setUint32(18, raw.length, true); h.setUint32(22, e.data.length, true); h.setUint16(26, name.length, true);
    const c = new DataView(new ArrayBuffer(46));
    c.setUint32(0, 0x02014b50, true); c.setUint16(4, 20, true); c.setUint16(6, 20, true); c.setUint16(8, 0x800, true);
    c.setUint16(10, method, true); c.setUint16(14, 0x21, true); c.setUint32(16, crc, true); c.setUint32(20, raw.length, true);
    c.setUint32(24, e.data.length, true); c.setUint16(28, name.length, true); c.setUint32(42, off, true);
    parts.push(new Uint8Array(h.buffer), name, raw); central.push(new Uint8Array(c.buffer), name);
    off += 30 + name.length + raw.length;
  }
  const csize = central.reduce((s, b) => s + b.length, 0);
  const end = new DataView(new ArrayBuffer(22));
  end.setUint32(0, 0x06054b50, true); end.setUint16(8, entries.length, true); end.setUint16(10, entries.length, true);
  end.setUint32(12, csize, true); end.setUint32(16, off, true);
  return new Uint8Array(await new Blob([...parts, ...central, new Uint8Array(end.buffer)]).arrayBuffer());
}

// ---------- protobuf: androidx.datastore.preferences PreferenceMap ----------
function reader(b) {
  let i = 0;
  const varint = () => { let r = 0n, s = 0n, x; do { x = b[i++]; r |= BigInt(x & 127) << s; s += 7n; } while (x & 128); return r; };
  return { get done() { return i >= b.length; }, varint, bytes() { const n = Number(varint()); const r = b.subarray(i, i + n); i += n; return r; },
    fixed(n) { const r = new DataView(b.buffer, b.byteOffset + i, n); i += n; return r; } };
}
const TYPES = { 1: 'bool', 2: 'float', 3: 'int', 4: 'long', 5: 'string', 6: 'set', 7: 'double', 8: 'bytes' };
const s64 = x => BigInt.asIntN(64, x);

/** → [{key, type, value}] w kolejności z pliku. */
export function decodePrefs(b) {
  const r = reader(b), out = [];
  while (!r.done) {
    const tag = Number(r.varint());
    if (tag !== 10) throw new Error('Nieznane pole ' + tag);
    const e = reader(r.bytes()); let key = '', val = null;
    while (!e.done) {
      const t = Number(e.varint());
      if (t === 10) key = td.decode(e.bytes());
      else if (t === 18) {
        const v = reader(e.bytes());
        while (!v.done) {
          const vt = Number(v.varint()), f = vt >> 3, w = vt & 7, type = TYPES[f];
          let value;
          if (f === 1) value = v.varint() !== 0n;
          else if (f === 2) value = v.fixed(4).getFloat32(0, true);
          else if (f === 3) value = Number(BigInt.asIntN(32, v.varint()));
          else if (f === 4) value = s64(v.varint()).toString();
          else if (f === 5) value = td.decode(v.bytes());
          else if (f === 6) { const ss = reader(v.bytes()); value = []; while (!ss.done) { ss.varint(); value.push(td.decode(ss.bytes())); } }
          else if (f === 7) value = v.fixed(8).getFloat64(0, true);
          else if (f === 8) value = Array.from(v.bytes());
          else throw new Error('Nieznany typ ' + vt + '/' + w);
          val = { type, value };
        }
      } else throw new Error('Nieznane pole wpisu');
    }
    out.push({ key, ...(val || { type: 'string', value: '' }) });
  }
  return out;
}

function wVarint(arr, x) { x = BigInt.asUintN(64, BigInt(x)); do { let b = Number(x & 127n); x >>= 7n; if (x) b |= 128; arr.push(b); } while (x); }
function wBytes(arr, tag, bytes) { arr.push(tag); wVarint(arr, bytes.length); for (const x of bytes) arr.push(x); }
export function encodePrefs(list) {
  const out = [];
  for (const p of list) {
    const v = [];
    switch (p.type) {
      case 'bool': v.push(8); wVarint(v, p.value ? 1 : 0); break;
      case 'float': { const d = new DataView(new ArrayBuffer(4)); d.setFloat32(0, Number(p.value), true); v.push(21, ...new Uint8Array(d.buffer)); break; }
      case 'int': v.push(24); wVarint(v, BigInt.asIntN(64, BigInt(Math.trunc(Number(p.value))))); break;
      case 'long': v.push(32); wVarint(v, BigInt(p.value)); break;
      case 'string': wBytes(v, 42, te.encode(p.value)); break;
      case 'set': { const s = []; for (const x of p.value) wBytes(s, 10, te.encode(x)); wBytes(v, 50, s); break; }
      case 'double': { const d = new DataView(new ArrayBuffer(8)); d.setFloat64(0, Number(p.value), true); v.push(57, ...new Uint8Array(d.buffer)); break; }
      case 'bytes': wBytes(v, 66, p.value); break;
    }
    const e = []; wBytes(e, 10, te.encode(p.key)); wBytes(e, 18, v);
    wBytes(out, 10, e);
  }
  return new Uint8Array(out);
}

// ---------- SharedPreferences XML ----------
export function decodeXml(b) {
  const doc = new DOMParser().parseFromString(td.decode(b), 'application/xml');
  if (doc.querySelector('parsererror')) throw new Error('Zły XML');
  const out = [];
  for (const el of doc.documentElement.children) {
    const key = el.getAttribute('name'), tag = el.tagName;
    if (tag === 'string') out.push({ key, type: 'string', value: el.textContent });
    else if (tag === 'boolean') out.push({ key, type: 'bool', value: el.getAttribute('value') === 'true' });
    else if (tag === 'set') out.push({ key, type: 'set', value: [...el.children].map(c => c.textContent) });
    else if (tag === 'null') out.push({ key, type: 'null', value: null });
    else out.push({ key, type: tag, value: el.getAttribute('value') }); // int, long, float
  }
  return out;
}
const esc = s => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
export function encodeXml(list) {
  let s = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n";
  for (const p of list) {
    const n = esc(p.key);
    if (p.type === 'string') s += `    <string name="${n}">${esc(p.value)}</string>\n`;
    else if (p.type === 'bool') s += `    <boolean name="${n}" value="${p.value ? 'true' : 'false'}" />\n`;
    else if (p.type === 'set') s += `    <set name="${n}">\n${p.value.map(x => `        <string>${esc(x)}</string>\n`).join('')}    </set>\n`;
    else if (p.type === 'null') s += `    <null name="${n}" />\n`;
    else s += `    <${p.type} name="${n}" value="${esc(p.value)}" />\n`;
  }
  return te.encode(s + '</map>\n');
}

export const same = (a, b) => a.length === b.length && a.every((x, i) => x === b[i]);
