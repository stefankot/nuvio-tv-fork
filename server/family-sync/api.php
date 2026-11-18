<?php
// Nuvio fork: synchronizacja ustawień rodziny. Serwer przechowuje tylko zaszyfrowane bloki (klucz zna tylko rodzina).
// Rodzina = losowy identyfikator + klucz (klucz zna tylko rodzina). Zapis tylko z sekretem właściciela.
// Parowanie: telewizor właściciela pokazuje kod 6 cyfr (10 min, jednorazowy); serwer zna tylko skrót kodu
// i klucz rodziny zaszyfrowany tym kodem. Udane użycie kasuje kod.
header('Content-Type: application/json');
header('Cache-Control: no-store');

const MAX_BLOB = 8 * 1024 * 1024;     // 8 MB zaszyfrowanych ustawień
const MISS_LIMIT = 20;                // nieudanych odczytów z jednego IP na godzinę (zgadywanie kodu)
const REQ_LIMIT = 300;                // wszystkich zapytań z jednego IP na godzinę
const PAIR_TTL = 600;                 // kod parowania ważny 10 minut

function out($code, $data) { http_response_code($code); echo json_encode($data); exit; }
function db() {
  static $pdo = null;
  if ($pdo) return $pdo;
  // Plik bazy SQLite w katalogu data/ (zablokowany dla internetu przez .htaccess); bez haseł do bazy.
  $dir = __DIR__ . '/data';
  if (!is_dir($dir)) { mkdir($dir, 0700); file_put_contents($dir . '/.htaccess', "Require all denied\n"); }
  $pdo = new PDO('sqlite:' . $dir . '/family.sqlite', null, null, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
  $pdo->exec('PRAGMA journal_mode=WAL');
  $pdo->exec('PRAGMA busy_timeout=5000');
  $pdo->exec('CREATE TABLE IF NOT EXISTS family_blobs (id TEXT PRIMARY KEY, owner_hash TEXT NOT NULL,
    version INTEGER NOT NULL, data TEXT NOT NULL, updated INTEGER NOT NULL)');
  $pdo->exec('CREATE TABLE IF NOT EXISTS family_pairs (lookup TEXT PRIMARY KEY, family_id TEXT NOT NULL,
    wrapped TEXT NOT NULL, expires INTEGER NOT NULL)');
  $pdo->exec('CREATE TABLE IF NOT EXISTS family_rate (ip_hash TEXT NOT NULL, kind TEXT NOT NULL, ts INTEGER NOT NULL)');
  $pdo->exec('CREATE INDEX IF NOT EXISTS family_rate_i ON family_rate (ip_hash, kind, ts)');
  // Panel (19.11): logowanie linkiem, rodziny panelu, raporty telewizorów.
  $pdo->exec('CREATE TABLE IF NOT EXISTS admin_tokens (h TEXT PRIMARY KEY, expires INTEGER NOT NULL, kind TEXT NOT NULL)');
  $pdo->exec('CREATE TABLE IF NOT EXISTS admin_families (family_id TEXT PRIMARY KEY, added INTEGER NOT NULL)');
  $pdo->exec('CREATE TABLE IF NOT EXISTS family_devices (family_id TEXT NOT NULL, device TEXT NOT NULL, name TEXT, info TEXT,
    last_seen INTEGER NOT NULL, PRIMARY KEY (family_id, device))');
  $pdo->exec('CREATE TABLE IF NOT EXISTS family_events (family_id TEXT NOT NULL, device TEXT NOT NULL, ts INTEGER NOT NULL, ev TEXT NOT NULL)');
  $pdo->exec('CREATE INDEX IF NOT EXISTS family_events_i ON family_events (family_id, ts)');
  foreach (['ALTER TABLE family_blobs ADD COLUMN src TEXT', "ALTER TABLE family_pairs ADD COLUMN kind TEXT DEFAULT 'device'",
    'ALTER TABLE family_blobs ADD COLUMN epoch INTEGER DEFAULT 0', 'ALTER TABLE family_devices ADD COLUMN box TEXT',
    'ALTER TABLE family_devices ADD COLUMN box_epoch INTEGER', 'ALTER TABLE family_devices ADD COLUMN wrap TEXT',
    'ALTER TABLE family_devices ADD COLUMN removed INTEGER DEFAULT 0'] as $q) {
    try { $pdo->exec($q); } catch (PDOException $e) {}
  }
  return $pdo;
}
function ipHash() { return hash('sha256', 'nuvio-rate:' . ($_SERVER['REMOTE_ADDR'] ?? '')); }
function count_rate($kind) {
  $st = db()->prepare('SELECT COUNT(*) FROM family_rate WHERE ip_hash=? AND kind=? AND ts>?');
  $st->execute([ipHash(), $kind, time() - 3600]);
  return (int)$st->fetchColumn();
}
function note($kind) {
  db()->prepare('INSERT INTO family_rate (ip_hash, kind, ts) VALUES (?,?,?)')->execute([ipHash(), $kind, time()]);
  if (mt_rand(0, 50) === 0) db()->prepare('DELETE FROM family_rate WHERE ts<?')->execute([time() - 7200]);
}
function validId($id) { return is_string($id) && preg_match('/^[0-9a-f]{64}$/', $id); }

if (count_rate('req') > REQ_LIMIT) out(429, ['error' => 'too_many']);
note('req');
$a = $_GET['a'] ?? '';

if ($a === 'get') {
  if (count_rate('miss') >= MISS_LIMIT) out(429, ['error' => 'too_many']);
  $id = $_GET['id'] ?? '';
  if (!validId($id)) out(400, ['error' => 'bad_id']);
  $st = db()->prepare('SELECT version, data, updated, src, epoch FROM family_blobs WHERE id=?');
  $st->execute([$id]);
  $row = $st->fetch(PDO::FETCH_ASSOC);
  if (!$row) { note('miss'); out(404, ['error' => 'not_found']); }
  $since = (int)($_GET['since'] ?? -1);
  $meta = ['version' => (int)$row['version'], 'updated' => (int)$row['updated'], 'src' => $row['src'] ?: 'tv', 'epoch' => (int)$row['epoch']];
  if ($since === (int)$row['version']) out(200, $meta);
  out(200, $meta + ['data' => $row['data']]);
}

if ($a === 'redeem') {
  if (count_rate('miss') >= MISS_LIMIT) out(429, ['error' => 'too_many']);
  $in = json_decode(file_get_contents('php://input', false, null, 0, 4096), true);
  $lookup = $in['lookup'] ?? '';
  if (!validId($lookup)) out(400, ['error' => 'bad_request']);
  db()->prepare('DELETE FROM family_pairs WHERE expires<?')->execute([time()]);
  $st = db()->prepare("SELECT wrapped FROM family_pairs WHERE lookup=? AND COALESCE(kind,'device')='device'");
  $st->execute([$lookup]);
  $w = $st->fetchColumn();
  if ($w === false) { note('miss'); out(404, ['error' => 'bad_code']); }
  db()->prepare('DELETE FROM family_pairs WHERE lookup=?')->execute([$lookup]);
  out(200, ['wrapped' => $w]);
}

if ($a === 'put' || $a === 'delete' || $a === 'pair') {
  $in = json_decode(file_get_contents('php://input', false, null, 0, MAX_BLOB + 4096), true);
  $id = $in['id'] ?? ''; $owner = $in['owner'] ?? '';
  if (!validId($id) || !is_string($owner) || strlen($owner) < 32) out(400, ['error' => 'bad_request']);
  $ownerHash = hash('sha256', 'nuvio-owner:' . $owner);
  $st = db()->prepare('SELECT owner_hash, version, epoch FROM family_blobs WHERE id=?');
  $st->execute([$id]);
  $row = $st->fetch(PDO::FETCH_ASSOC);
  if ($row && !hash_equals($row['owner_hash'], $ownerHash)) { note('miss'); out(403, ['error' => 'not_owner']); }
  if ($a === 'pair') {
    if (!$row) out(404, ['error' => 'no_family']);
    $lookup = $in['lookup'] ?? ''; $wrapped = $in['wrapped'] ?? '';
    if (!validId($lookup) || !is_string($wrapped) || strlen($wrapped) > 2048) out(400, ['error' => 'bad_request']);
    $kind = ($in['kind'] ?? '') === 'panel' ? 'panel' : 'device';
    db()->prepare("DELETE FROM family_pairs WHERE (family_id=? AND COALESCE(kind,'device')=?) OR expires<?")->execute([$id, $kind, time()]);
    try {
      db()->prepare('INSERT INTO family_pairs (lookup, family_id, wrapped, expires, kind) VALUES (?,?,?,?,?)')
        ->execute([$lookup, $id, $wrapped, time() + PAIR_TTL, $kind]);
    } catch (PDOException $e) { out(409, ['error' => 'code_taken']); }
    out(200, ['expires_in' => PAIR_TTL]);
  }
  if ($a === 'delete') {
    db()->prepare('DELETE FROM family_pairs WHERE family_id=?')->execute([$id]);
    if ($row) db()->prepare('DELETE FROM family_blobs WHERE id=?')->execute([$id]);
    out(200, ['ok' => true]);
  }
  $data = $in['data'] ?? '';
  if (!is_string($data) || $data === '' || strlen($data) > MAX_BLOB) out(413, ['error' => 'too_large']);
  if (!$row) {
    if (count_rate('new') >= 3) out(429, ['error' => 'too_many']); // nowe rodziny: najwyżej 3 na godzinę z jednego IP
    note('new');
    db()->prepare('INSERT INTO family_blobs (id, owner_hash, version, data, updated) VALUES (?,?,?,?,?)')
      ->execute([$id, $ownerHash, 1, $data, time()]);
    out(200, ['version' => 1]);
  }
  // Panel wysyła base = wersję, którą edytował; jeśli w międzyczasie telewizor wysłał nowszą, panel musi ją wczytać.
  if (isset($in['base']) && (int)$in['base'] !== (int)$row['version']) out(409, ['error' => 'newer', 'version' => (int)$row['version']]);
  // Po wymianie klucza zapis starym kluczem zepsułby ustawienia rodzinie: telewizor musi najpierw pobrać nowy klucz.
  if ((int)$row['epoch'] !== (int)($in['epoch'] ?? 0)) out(409, ['error' => 'rekey', 'epoch' => (int)$row['epoch']]);
  $src = ($in['src'] ?? '') === 'panel' ? 'panel' : 'tv';
  $v = (int)$row['version'] + 1;
  db()->prepare('UPDATE family_blobs SET version=?, data=?, updated=?, src=? WHERE id=?')->execute([$v, $data, time(), $src, $id]);
  out(200, ['version' => $v]);
}
require __DIR__ . '/panel_api.php';
out(400, ['error' => 'bad_action']);
