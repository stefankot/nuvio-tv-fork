<?php
// Panel „Nuvio Rodzina” (19.11.2026): logowanie linkiem e-mail, połączenie przeglądarki kodem z telewizora,
// raporty telewizorów (anonimowe). Dołączany z api.php. Wersja minimalna dla kilku osób.
if (!defined('MAX_BLOB')) exit;

const ADMIN_EMAIL = 'brzostkowski@gmail.com';
const SESSION_DAYS = 30;
const EVENT_DAYS = 30;

function session_ok() {
  $sid = $_COOKIE['nsid'] ?? '';
  if (!is_string($sid) || !preg_match('/^[0-9a-f]{64}$/', $sid)) return false;
  $st = db()->prepare("SELECT 1 FROM admin_tokens WHERE h=? AND kind='session' AND expires>?");
  $st->execute([hash('sha256', 'nuvio-sess:' . $sid), time()]);
  return (bool)$st->fetchColumn();
}
function need_session() {
  if (!session_ok()) out(401, ['error' => 'login']);
  $origin = $_SERVER['HTTP_ORIGIN'] ?? '';
  if ($origin !== '' && $origin !== 'https://' . ($_SERVER['HTTP_HOST'] ?? '')) out(403, ['error' => 'origin']);
}
function body($max = 65536) { return json_decode(file_get_contents('php://input', false, null, 0, $max), true) ?: []; }
function admin_family($id) {
  if (!validId($id)) out(400, ['error' => 'bad_id']);
  $st = db()->prepare('SELECT 1 FROM admin_families WHERE family_id=?');
  $st->execute([$id]);
  if (!$st->fetchColumn()) out(403, ['error' => 'not_linked']);
}

// ---------- logowanie ----------
if ($a === 'login_start') {
  if (count_rate('mail') >= 5) out(429, ['error' => 'too_many']);
  note('mail');
  $in = body();
  if (strtolower(trim($in['email'] ?? '')) !== ADMIN_EMAIL) out(200, ['ok' => true]); // nie zdradzaj, czy adres jest właściwy
  $t = bin2hex(random_bytes(32));
  db()->prepare("INSERT INTO admin_tokens (h, expires, kind) VALUES (?,?, 'link')")->execute([hash('sha256', 'nuvio-link:' . $t), time() + 900]);
  $link = 'https://' . $_SERVER['HTTP_HOST'] . dirname($_SERVER['SCRIPT_NAME']) . '/api.php?a=login_finish&t=' . $t;
  $link = str_replace('//api.php', '/api.php', $link);
  $msg = "Kliknij, aby zalogować się do panelu Nuvio Rodzina (link ważny 15 minut, jeden raz):\r\n\r\n$link\r\n\r\nJeśli to nie Ty, zignoruj tę wiadomość.";
  $from = 'no-reply@' . preg_replace('/^[^.]+\./', '', $_SERVER['HTTP_HOST']);
  $ok = mail(ADMIN_EMAIL, '=?UTF-8?B?' . base64_encode('Logowanie do Nuvio Rodzina') . '?=', $msg,
    "From: Nuvio Rodzina <$from>\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: 8bit", "-f$from");
  out($ok ? 200 : 500, $ok ? ['ok' => true] : ['error' => 'mail']);
}
if ($a === 'login_finish') {
  $t = $_GET['t'] ?? '';
  $h = hash('sha256', 'nuvio-link:' . (is_string($t) ? $t : ''));
  $st = db()->prepare("DELETE FROM admin_tokens WHERE h=? AND kind='link' AND expires>?");
  $st->execute([$h, time()]);
  header('Content-Type: text/html; charset=utf-8');
  if ($st->rowCount() !== 1) { http_response_code(400); echo '<meta charset="utf-8"><p>Link wygasł albo był już użyty. <a href="panel/">Wyślij nowy</a></p>'; exit; }
  $sid = bin2hex(random_bytes(32));
  db()->prepare("INSERT INTO admin_tokens (h, expires, kind) VALUES (?,?, 'session')")->execute([hash('sha256', 'nuvio-sess:' . $sid), time() + SESSION_DAYS * 86400]);
  db()->prepare('DELETE FROM admin_tokens WHERE expires<?')->execute([time()]);
  setcookie('nsid', $sid, ['expires' => time() + SESSION_DAYS * 86400, 'path' => '/', 'secure' => true, 'httponly' => true, 'samesite' => 'Strict']);
  // SameSite=Strict: ciasteczko z linku z poczty nie wróci przy przekierowaniu, więc przejście przez stronę na tej samej domenie.
  echo '<meta charset="utf-8"><meta http-equiv="refresh" content="0;url=panel/"><p>Zalogowano. <a href="panel/">Przejdź do panelu</a></p>';
  exit;
}
if ($a === 'logout') {
  $sid = $_COOKIE['nsid'] ?? '';
  if (is_string($sid)) db()->prepare('DELETE FROM admin_tokens WHERE h=?')->execute([hash('sha256', 'nuvio-sess:' . $sid)]);
  setcookie('nsid', '', ['expires' => 1, 'path' => '/', 'secure' => true, 'httponly' => true, 'samesite' => 'Strict']);
  out(200, ['ok' => true]);
}
if ($a === 'me') { out(session_ok() ? 200 : 401, session_ok() ? ['email' => ADMIN_EMAIL] : ['error' => 'login']); }

// ---------- połączenie przeglądarki (kod z telewizora właściciela, typ „panel”) ----------
if ($a === 'panel_redeem') {
  need_session();
  if (count_rate('miss') >= MISS_LIMIT) out(429, ['error' => 'too_many']);
  $lookup = body()['lookup'] ?? '';
  if (!validId($lookup)) out(400, ['error' => 'bad_request']);
  $st = db()->prepare("SELECT family_id, wrapped FROM family_pairs WHERE lookup=? AND kind='panel' AND expires>?");
  $st->execute([$lookup, time()]);
  $row = $st->fetch(PDO::FETCH_ASSOC);
  if (!$row) { note('miss'); out(404, ['error' => 'bad_code']); }
  db()->prepare('DELETE FROM family_pairs WHERE lookup=?')->execute([$lookup]);
  db()->prepare('INSERT OR IGNORE INTO admin_families (family_id, added) VALUES (?,?)')->execute([$row['family_id'], time()]);
  out(200, ['wrapped' => $row['wrapped']]);
}
if ($a === 'panel_unlink') {
  need_session();
  db()->prepare('DELETE FROM admin_families WHERE family_id=?')->execute([body()['id'] ?? '']);
  out(200, ['ok' => true]);
}

// ---------- raporty z telewizorów (bez sesji; trzeba znać identyfikator rodziny) ----------
if ($a === 'report') {
  $in = body(131072);
  $id = $in['id'] ?? ''; $dev = $in['device'] ?? '';
  if (!validId($id) || !is_string($dev) || !preg_match('/^[0-9a-f]{32}$/', $dev)) out(400, ['error' => 'bad_request']);
  $st = db()->prepare('SELECT 1 FROM family_blobs WHERE id=?');
  $st->execute([$id]);
  if (!$st->fetchColumn()) { note('miss'); out(404, ['error' => 'no_family']); }
  $st = db()->prepare('SELECT removed FROM family_devices WHERE family_id=? AND device=?');
  $st->execute([$id, $dev]);
  if ((int)$st->fetchColumn() === 1) out(200, ['removed' => true]);
  $info = json_encode(array_slice((array)($in['info'] ?? []), 0, 20));
  // box = klucz urządzenia zaszyfrowany kluczem rodziny (serwer go nie odczyta); panel używa go przy wymianie klucza.
  $box = is_string($in['box'] ?? null) && strlen($in['box']) < 512 ? $in['box'] : null;
  db()->prepare('INSERT INTO family_devices (family_id, device, info, last_seen, box, box_epoch) VALUES (?,?,?,?,?,?)
    ON CONFLICT(family_id, device) DO UPDATE SET info=excluded.info, last_seen=excluded.last_seen,
    box=COALESCE(excluded.box, box), box_epoch=COALESCE(excluded.box_epoch, box_epoch)')
    ->execute([$id, $dev, $info, time(), $box, $box ? (int)($in['epoch'] ?? 0) : null]);
  $ins = db()->prepare('INSERT INTO family_events (family_id, device, ts, ev) VALUES (?,?,?,?)');
  foreach (array_slice((array)($in['events'] ?? []), 0, 200) as $e) {
    $ts = min(time(), max(time() - EVENT_DAYS * 86400, (int)(($e['t'] ?? 0) / 1000) ?: time()));
    $j = json_encode($e);
    if (strlen($j) > ((($e['k'] ?? '') === 'usage') ? 60000 : 1000)) continue; // za duże: pomiń (obcięty JSON byłby zepsuty)
    $ins->execute([$id, $dev, $ts, $j]);
  }
  if (mt_rand(0, 20) === 0) db()->prepare('DELETE FROM family_events WHERE ts<?')->execute([time() - EVENT_DAYS * 86400]);
  out(200, ['ok' => true]);
}

// ---------- dane dla panelu ----------
if ($a === 'families') {
  need_session();
  $rows = db()->query('SELECT f.family_id AS id, b.version, b.updated, b.src, b.epoch FROM admin_families f LEFT JOIN family_blobs b ON b.id=f.family_id')->fetchAll(PDO::FETCH_ASSOC);
  out(200, ['families' => $rows]);
}
if ($a === 'devices') {
  need_session();
  $id = $_GET['id'] ?? ''; admin_family($id);
  $st = db()->prepare('SELECT device, name, info, last_seen, box, box_epoch FROM family_devices WHERE family_id=? AND COALESCE(removed,0)=0 ORDER BY last_seen DESC');
  $st->execute([$id]);
  $devs = $st->fetchAll(PDO::FETCH_ASSOC);
  $st = db()->prepare('SELECT device, ts, ev FROM family_events WHERE family_id=? AND ts>? ORDER BY ts DESC LIMIT 3000');
  $st->execute([$id, time() - EVENT_DAYS * 86400]);
  out(200, ['devices' => $devs, 'events' => $st->fetchAll(PDO::FETCH_ASSOC), 'now' => time()]);
}
if ($a === 'device_name') {
  need_session();
  $in = body(); admin_family($in['id'] ?? '');
  db()->prepare('UPDATE family_devices SET name=? WHERE family_id=? AND device=?')
    ->execute([mb_substr((string)($in['name'] ?? ''), 0, 60), $in['id'], (string)($in['device'] ?? '')]);
  out(200, ['ok' => true]);
}
if ($a === 'device_forget') {
  need_session();
  $in = body(); admin_family($in['id'] ?? '');
  $dev = (string)($in['device'] ?? '');
  db()->prepare('DELETE FROM family_devices WHERE family_id=? AND device=?')->execute([$in['id'], $dev]);
  db()->prepare('DELETE FROM family_events WHERE family_id=? AND device=?')->execute([$in['id'], $dev]);
  out(200, ['ok' => true]);
}
if ($a === 'events_clear') {
  need_session();
  $in = body(); admin_family($in['id'] ?? '');
  db()->prepare('DELETE FROM family_events WHERE family_id=?')->execute([$in['id']]);
  out(200, ['ok' => true]);
}

// ---------- wymiana klucza rodziny (usunięcie telewizora z rodziny) ----------
// Telewizor: nowa epoka w a=get → a=rekey → dostaje nowy klucz zaszyfrowany swoim kluczem urządzenia.
if ($a === 'rekey') {
  $in = body(); $id = $in['id'] ?? ''; $dev = $in['device'] ?? '';
  if (!validId($id) || !is_string($dev)) out(400, ['error' => 'bad_request']);
  $st = db()->prepare('SELECT d.wrap, d.removed, b.epoch FROM family_devices d JOIN family_blobs b ON b.id=d.family_id WHERE d.family_id=? AND d.device=?');
  $st->execute([$id, $dev]);
  $row = $st->fetch(PDO::FETCH_ASSOC);
  if (!$row) { note('miss'); out(404, ['error' => 'unknown_device']); }
  if ((int)$row['removed'] === 1) out(403, ['error' => 'removed']);
  if (!$row['wrap']) out(404, ['error' => 'no_wrap']);
  out(200, ['epoch' => (int)$row['epoch'], 'wrap' => $row['wrap']]);
}
// Panel: nowy blok zaszyfrowany nowym kluczem + nowy klucz dla każdego zostającego telewizora; usunięte tracą dostęp.
if ($a === 'rotate') {
  need_session();
  $in = body(MAX_BLOB + 65536); $id = $in['id'] ?? ''; admin_family($id);
  $st = db()->prepare('SELECT owner_hash, version, epoch FROM family_blobs WHERE id=?');
  $st->execute([$id]);
  $row = $st->fetch(PDO::FETCH_ASSOC);
  if (!$row || !hash_equals($row['owner_hash'], hash('sha256', 'nuvio-owner:' . ($in['owner'] ?? '')))) out(403, ['error' => 'not_owner']);
  $epoch = (int)($in['epoch'] ?? 0); $data = $in['data'] ?? '';
  if ($epoch !== (int)$row['epoch'] + 1 || !is_string($data) || $data === '' || strlen($data) > MAX_BLOB) out(400, ['error' => 'bad_request']);
  if (isset($in['base']) && (int)$in['base'] !== (int)$row['version']) out(409, ['error' => 'newer', 'version' => (int)$row['version']]);
  $pdo = db(); $pdo->beginTransaction();
  $pdo->prepare("UPDATE family_blobs SET version=version+1, data=?, updated=?, src='panel', epoch=? WHERE id=?")->execute([$data, time(), $epoch, $id]);
  $pdo->prepare('UPDATE family_devices SET wrap=NULL WHERE family_id=?')->execute([$id]);
  $w = $pdo->prepare('UPDATE family_devices SET wrap=? WHERE family_id=? AND device=?');
  foreach ((array)($in['wraps'] ?? []) as $dev => $wrap) if (is_string($wrap) && strlen($wrap) < 512) $w->execute([$wrap, $id, (string)$dev]);
  $r = $pdo->prepare('UPDATE family_devices SET removed=1, wrap=NULL WHERE family_id=? AND device=?');
  foreach ((array)($in['remove'] ?? []) as $dev) $r->execute([$id, (string)$dev]);
  $pdo->prepare('DELETE FROM family_pairs WHERE family_id=?')->execute([$id]); // stare kody zawierały stary klucz
  $pdo->commit();
  out(200, ['version' => (int)$row['version'] + 1, 'epoch' => $epoch]);
}
