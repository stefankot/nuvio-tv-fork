import android.app.UiAutomation;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.concurrent.TimeoutException;

/**
 * Stały czytnik ekranu dla testów UX na JMGO (24.11.2026, właściciel: „testy połowę czasu stoją bezczynnie”).
 *
 * Zwykłe `uiautomator dump` za każdym razem: start procesu, połączenie z UiAutomation i `waitForIdle(1000, 10000)`
 * (czeka na 1 s ciszy na ekranie, na ruchomym ekranie do 10 s) — telewizor stoi w miejscu. Ten proces łączy się raz,
 * wciska klawisze bez uruchamiania `input`, czeka tylko tyle ciszy, ile zażądano (domyślnie ~200 ms) i zwraca ten sam XML
 * co `uiautomator dump --compressed` (ten sam AccessibilityNodeInfoDumper z /system/framework/uiautomator.jar).
 *
 * Uruchomienie (classpath z uiautomator.jar!):
 *   app_process -Djava.class.path=/data/local/tmp/fastdump.jar:/system/framework/uiautomator.jar /system/bin FastDump 1920 1080
 *
 * Protokół (stdin, jedna linia = jedno polecenie; odpowiedź na stdout kończy wiersz „#END <idle_ms> <dump_ms>”):
 *   D <cisza_ms> <max_ms>                               — odczyt ekranu
 *   K <kod> <n> <przerwa_ms> <cisza_ms> <max_ms>        — n naciśnięć klawisza (wciśnij+puść), potem odczyt
 *   X <kod> <n> <przerwa_ms>                            — tylko naciśnięcia, bez odczytu (odpowiedź: samo „#END”)
 *   Q                                                   — koniec
 * Błąd: wiersz „#ERR <powód>” przed „#END”. EOF na stdin = koniec.
 */
public class FastDump {
    static UiAutomation ua;
    static Method dumper;
    static final File TMP = new File("/data/local/tmp/fd_dump.xml");
    static int W = 1920, H = 1080;
    static PrintStream out;

    public static void main(String[] args) throws Exception {
        if (args.length >= 2) {
            W = Integer.parseInt(args[0]);
            H = Integer.parseInt(args[1]);
        }
        out = new PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), false, "UTF-8");
        Class<?> wc = Class.forName("com.android.uiautomator.core.UiAutomationShellWrapper");
        Object wrapper = wc.getConstructor().newInstance();
        wc.getMethod("connect").invoke(wrapper);
        wc.getMethod("setCompressedLayoutHierarchy", boolean.class).invoke(wrapper, true);
        ua = (UiAutomation) wc.getMethod("getUiAutomation").invoke(wrapper);
        Class<?> dc = Class.forName("com.android.uiautomator.core.AccessibilityNodeInfoDumper");
        dumper = dc.getMethod("dumpWindowToFile", AccessibilityNodeInfo.class, File.class, int.class, int.class, int.class);
        out.println("#READY");
        out.flush();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        try {
            while ((line = in.readLine()) != null) {
                String[] p = line.trim().split("\\s+");
                if (p.length == 0 || p[0].isEmpty()) continue;
                try {
                    if (p[0].equals("Q")) break;
                    if (p[0].equals("D")) {
                        dumpScreen(num(p, 1, 200), num(p, 2, 3000));
                    } else if (p[0].equals("K")) {
                        press(num(p, 1, 20), num(p, 2, 1), num(p, 3, 250));
                        dumpScreen(num(p, 4, 200), num(p, 5, 3000));
                    } else if (p[0].equals("X")) {
                        press(num(p, 1, 20), num(p, 2, 1), num(p, 3, 250));
                        out.println("#END 0 0");
                    } else {
                        out.println("#ERR polecenie?");
                        out.println("#END 0 0");
                    }
                } catch (Throwable t) {
                    out.println("#ERR " + t.getClass().getSimpleName() + ": " + t.getMessage());
                    out.println("#END 0 0");
                }
                out.flush();
            }
        } finally {
            try {
                wc.getMethod("disconnect").invoke(wrapper);
            } catch (Throwable ignored) {
            }
        }
    }

    static int num(String[] p, int i, int def) {
        return i < p.length ? Integer.parseInt(p[i]) : def;
    }

    static void press(int code, int n, int pauseMs) {
        for (int i = 0; i < n; i++) {
            long t = SystemClock.uptimeMillis();
            KeyEvent down = new KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
            KeyEvent up = new KeyEvent(t, t + 20, KeyEvent.ACTION_UP, code, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
            ua.injectInputEvent(down, true);
            ua.injectInputEvent(up, true);
            if (pauseMs > 0 && i < n - 1) SystemClock.sleep(pauseMs);
        }
    }

    static void dumpScreen(int quietMs, int maxMs) throws Exception {
        long t0 = SystemClock.uptimeMillis();
        if (quietMs > 0) {
            try {
                ua.waitForIdle(quietMs, maxMs);
            } catch (TimeoutException ignored) {
                // ruchomy ekran (karuzela, animacje) — czytamy mimo to
            }
        }
        long t1 = SystemClock.uptimeMillis();
        AccessibilityNodeInfo root = ua.getRootInActiveWindow();
        if (root == null) {
            out.println("#ERR null root");
            out.println("#END " + (t1 - t0) + " 0");
            return;
        }
        if (TMP.exists()) TMP.delete();
        dumper.invoke(null, root, TMP, 0, W, H);
        byte[] buf = new byte[(int) TMP.length()];
        try (FileInputStream f = new FileInputStream(TMP)) {
            int off = 0;
            while (off < buf.length) {
                int r = f.read(buf, off, buf.length - off);
                if (r < 0) break;
                off += r;
            }
        }
        out.write(buf, 0, buf.length);
        out.println();
        out.println("#END " + (t1 - t0) + " " + (SystemClock.uptimeMillis() - t1));
    }
}
