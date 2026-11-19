package com.nuvio.tv.zonos32;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.StatFs;
import android.os.SystemClock;
import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Optional ANDROID 32-BIT companion, installed through the system package installer.
 * The service is restricted to same-signer callers by manifest signature permission.
 * Runs the actual NDK-built ZONOS3 worker from its nativeLibraryDir, not from filesDir.
 */
public final class ZonosWorkerService extends Service {
    private static final String START = "com.nuvio.tv.zonos32.START";
    private static final String STOP = "com.nuvio.tv.zonos32.STOP";
    private static final String CLEAR = "com.nuvio.tv.zonos32.CLEAR_MODELS";
    private static final String BACKBONE_URI = "com.nuvio.tv.zonos32.backbone_uri";
    private static final String DECODER_URI = "com.nuvio.tv.zonos32.decoder_uri";
    private static final String RECEIVER = "com.nuvio.tv.zonos32.receiver";
    private static final int READY = 1;
    private static final int ERROR = 2;
    private static final String CHANNEL = "zonos32_local_tts";
    private static final int NOTIFICATION_ID = 32743;
    private static final long MIB = 1024L * 1024L;

    private static final String BACKBONE_SHA = "66fceb6d73a8c30bde80249e4112bb308380ab7f3384fa11c8e4048115e4ceec";
    private static final String DECODER_SHA = "834e5b70b88294afc3c702a478487e0664dcc6b574c16fc6167d72444aabeafd";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile Process process;
    private volatile int port;
    private volatile String accessToken;
    private volatile boolean stopped;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (STOP.equals(action)) {
            stopWorker();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (CLEAR.equals(action)) {
            stopWorker();
            worker.execute(() -> {
                deleteModels();
                stopForeground(true);
                stopSelf();
            });
            return START_NOT_STICKY;
        }
        if (!START.equals(action)) return START_NOT_STICKY;
        createNotification();
        startForeground(NOTIFICATION_ID,
            new Notification.Builder(this, CHANNEL)
                .setContentTitle("ZONOS3 — synteza offline")
                .setContentText("Przygotowanie audio na tym urządzeniu")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(true).build());
        @SuppressWarnings("deprecation")
        ResultReceiver receiver = (ResultReceiver) intent.getParcelableExtra(RECEIVER);
        @SuppressWarnings("deprecation")
        Uri backbone = (Uri) intent.getParcelableExtra(BACKBONE_URI);
        @SuppressWarnings("deprecation")
        Uri decoder = (Uri) intent.getParcelableExtra(DECODER_URI);
        stopped = false;
        worker.execute(() -> prepareAndRun(backbone, decoder, receiver));
        return START_NOT_STICKY;
    }

    private void prepareAndRun(Uri backbone, Uri decoder, ResultReceiver receiver) {
        try {
            if (stopped) throw new IOException("Silnik zatrzymany");
            if (backbone == null || decoder == null) throw new IOException("Brak dostępu do plików modeli");
            if (android.os.Build.VERSION.SUPPORTED_32_BIT_ABIS.length == 0 ||
                !Arrays.asList(android.os.Build.SUPPORTED_32_BIT_ABIS).contains("armeabi-v7a")) {
                throw new IOException("System nie obsługuje armeabi-v7a");
            }
            synchronized (this) {
                if (process != null && process.isAlive() && port > 0 && accessToken != null) {
                    replyReady(receiver, port, accessToken);
                    return;
                }
            }
            if (isMemoryLow(true)) throw new IOException("Za mało wolnej RAM na model 32-bitowy");
            File modelDir = new File(getFilesDir(), "models-v1");
            if (!modelDir.isDirectory() && !modelDir.mkdirs()) throw new IOException("Brak katalogu modelu");
            File core = verifiedCopy(backbone, new File(modelDir, "backbone-ptq.gguf"),
                BACKBONE_SHA, 663150624L);
            File zac = verifiedCopy(decoder, new File(modelDir, "zac-keep2.gguf"),
                DECODER_SHA, 452325440L);
            if (stopped || isMemoryLow(true)) throw new IOException("Zbyt mało pamięci RAM");
            File executable = new File(getApplicationInfo().nativeLibraryDir, "libzonos3_server.so");
            if (!executable.isFile() || !executable.canExecute()) {
                throw new IOException("Pakiet ARM32 nie zawiera działającego silnika NDK");
            }
            int freePort;
            try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
                freePort = socket.getLocalPort();
            }
            byte[] secret = new byte[32];
            new SecureRandom().nextBytes(secret);
            String localToken = Base64.encodeToString(secret, Base64.URL_SAFE | Base64.NO_WRAP);
            File output = new File(getCacheDir(), "zonos32.log");
            if (output.length() > 2L * MIB) output.delete();
            ProcessBuilder builder = new ProcessBuilder(
                executable.getAbsolutePath(), core.getAbsolutePath(),
                "--zac", zac.getAbsolutePath(),
                "--cpu", "--batch", "1", "--zac-threads", "1",
                "--host", "127.0.0.1", "--port", Integer.toString(freePort)
            ).redirectErrorStream(true).redirectOutput(output);
            builder.environment().put("ZONOS3_ACCESS_TOKEN", localToken);
            Process started = builder.start();
            synchronized (this) {
                process = started;
                port = freePort;
                accessToken = localToken;
            }
            long began = SystemClock.elapsedRealtime();
            while (!stopped && SystemClock.elapsedRealtime() - began < 100000L) {
                if (!started.isAlive()) throw new IOException("ZONOS3 zakończył się przed uruchomieniem");
                if (isMemoryLow(false)) throw new IOException("Niski poziom pamięci RAM");
                if (isHealthy(freePort, localToken)) {
                    replyReady(receiver, freePort, localToken);
                    startWatchdog(started, began);
                    return;
                }
                Thread.sleep(600);
            }
            throw new IOException("ZONOS3 nie uruchomił się w ciągu 100 s");
        } catch (Exception e) {
            stopWorker();
            Bundle data = new Bundle();
            data.putString("error", e.getMessage() == null ? "Błąd silnika" : e.getMessage());
            if (receiver != null) receiver.send(ERROR, data);
            stopForeground(true);
            stopSelf();
        }
    }

    private File verifiedCopy(Uri uri, File target, String expected, long size) throws Exception {
        if (target.isFile() && target.length() == size && sameHash(target, expected)) return target;
        StatFs stat = new StatFs(target.getParentFile().getAbsolutePath());
        if (stat.getAvailableBytes() < size + 500L * MIB) {
            throw new IOException("Brak miejsca na kopię modelu w komponencie ARM32");
        }
        File temp = File.createTempFile("zonos-model-", ".part", target.getParentFile());
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            long copied = 0;
            try (InputStream input = getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(temp)) {
                if (input == null) throw new IOException("Model niedostępny");
                byte[] buf = new byte[128 * 1024];
                while (true) {
                    if (stopped) throw new IOException("Przerwano przekazywanie modelu");
                    int count = input.read(buf);
                    if (count < 0) break;
                    copied += count;
                    if (copied > size) throw new IOException("Model za duży");
                    hash.update(buf, 0, count);
                    output.write(buf, 0, count);
                }
                output.getFD().sync();
            }
            if (copied != size || !hex(hash.digest()).equals(expected)) {
                throw new IOException("Błędna suma SHA-256 modelu ARM32");
            }
            if (target.exists() && !target.delete()) throw new IOException("Nie można zastąpić modelu");
            if (!temp.renameTo(target)) throw new IOException("Nie można zatwierdzić modelu");
            return target;
        } finally { temp.delete(); }
    }

    private boolean sameHash(File file, String expected) throws Exception {
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buf = new byte[128 * 1024];
            int count;
            while ((count = input.read(buf)) >= 0) hash.update(buf, 0, count);
        }
        return hex(hash.digest()).equals(expected);
    }

    private String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return result.toString();
    }

    private boolean isMemoryLow(boolean starting) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        manager.getMemoryInfo(info);
        long threshold = info.totalMem < 2500L * MIB ? 450L * MIB :
            info.totalMem < 3500L * MIB ? 520L * MIB : 700L * MIB;
        long startup = info.totalMem < 2500L * MIB ? 1250L * MIB :
            info.totalMem < 3500L * MIB ? 1500L * MIB : 1750L * MIB;
        return info.lowMemory || info.availMem <
            Math.max(info.threshold + 256L * MIB, starting ? startup : threshold);
    }

    private boolean isHealthy(int atPort, String secret) {
        try {
            HttpURLConnection conn = (HttpURLConnection)
                new URL("http://127.0.0.1:" + atPort + "/v2/health").openConnection();
            conn.setConnectTimeout(300);
            conn.setReadTimeout(300);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Authorization", "Bearer " + secret);
            try { return conn.getResponseCode() >= 200 && conn.getResponseCode() <= 299; }
            finally { conn.disconnect(); }
        } catch (Exception ex) { return false; }
    }

    private void replyReady(ResultReceiver receiver, int atPort, String secret) {
        if (receiver == null) return;
        Bundle data = new Bundle();
        data.putInt("port", atPort);
        data.putString("token", secret);
        receiver.send(READY, data);
    }

    private void startWatchdog(Process child, long startedAt) {
        Thread watchdog = new Thread(() -> {
            while (!stopped && child.isAlive()) {
                if (isMemoryLow(false) ||
                    SystemClock.elapsedRealtime() - startedAt > 16L * 60L * 1000L) {
                    stopWorker();
                    stopForeground(true);
                    stopSelf();
                    return;
                }
                try { Thread.sleep(1000L); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }, "zonos32-memory-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private synchronized void stopWorker() {
        stopped = true;
        Process previous = process;
        process = null;
        port = 0;
        accessToken = null;
        if (previous != null) {
            previous.destroy();
            if (previous.isAlive()) previous.destroyForcibly();
        }
    }

    private void deleteModels() {
        File dir = new File(getFilesDir(), "models-v1");
        File[] files = dir.listFiles();
        if (files != null) for (File file : files) if (file.isFile()) file.delete();
    }

    private void createNotification() {
        NotificationManager notifications = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifications.createNotificationChannel(new NotificationChannel(
            CHANNEL, "Lokalna synteza ZONOS3", NotificationManager.IMPORTANCE_LOW));
    }

    @Override public void onDestroy() {
        stopWorker();
        worker.shutdownNow();
        super.onDestroy();
    }
}
