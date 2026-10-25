package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Sends the remote's Home button to this launcher. Two uses:
 * - remotes that emit KEYCODE_ALL_APPS for Home (Xiaomi sticks and boxes): Android TV 9 routes that key only
 *   to the system-image launcher, so once the stock launcher is disabled the button does nothing;
 * - keeping the stock launcher as the system's Home (build-and-install -KeepStockHome): Glass TV opens over it
 *   on Home and after boot, and nothing the TV needs is disabled. On Google TV an update can re-enable the
 *   setup wizard, which then takes Home from any launcher but the stock one and crash-loops the system.
 * Pressing Home twice quickly lets the second press through to the system's own Home, a way out if this
 * launcher ever fails. Where the TV handles Home before this service sees it, the stock home screen's
 * window is watched instead (onAccessibilityEvent). Every other key passes through untouched.
 *
 * Enable over adb:
 *   settings put secure enabled_accessibility_services com.gathin.tvhome/.HomeKeyService
 *   settings put secure accessibility_enabled 1
 */
public class HomeKeyService extends AccessibilityService {

    private static final long DOUBLE_PRESS_MS = 700;
    private static final long STAY_MS = 2500;
    /**
     * The stock home screens' own activities. Only these are covered: the stock launchers also own the
     * Settings panel and its PIN prompt (Google TV), which must stay in front.
     */
    private static final java.util.Set<String> STOCK_HOMES = new java.util.HashSet<>(java.util.Arrays.asList(
            "com.google.android.apps.tv.launcherx.home.HomeActivity",
            "com.google.android.tvlauncher.MainActivity",
            "com.google.android.leanbacklauncher.MainActivity"));

    /** Scancodes of remote buttons that should open Settings (config.xml, settings_key_scancodes). */
    private int[] settingsScancodes = new int[0];
    private long lastHomeUp;
    /** This launcher could not be started: Home goes to the system until it can. */
    private boolean failed;
    private long lastBounce;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        settingsScancodes = getResources().getIntArray(R.array.settings_key_scancodes);
        // Connected at boot (and when enabled): come up in front of whichever launcher the system started.
        openHome();
    }

    private boolean isSettingsKey(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_SETTINGS) return true;
        for (int code : settingsScancodes) if (event.getScanCode() == code) return true;
        return false;
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        // The stock launcher handled the remote's Settings button; with it disabled the button does nothing.
        if (isSettingsKey(event)) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            return true;
        }
        int code = event.getKeyCode();
        if (code != KeyEvent.KEYCODE_ALL_APPS && code != KeyEvent.KEYCODE_HOME) return false;
        if (code == KeyEvent.KEYCODE_HOME && (failed || SystemClock.uptimeMillis() - lastHomeUp < DOUBLE_PRESS_MS)) {
            // The second of two quick presses goes to the system: the stock home screen. After a failed
            // start this one does too, and the press after it tries this launcher again.
            if (event.getAction() == KeyEvent.ACTION_UP) {
                lastHomeUp = 0;
                failed = false;
            }
            return false;
        }
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
            if (code == KeyEvent.KEYCODE_HOME) lastHomeUp = SystemClock.uptimeMillis();
            openHome();
        }
        return true;
    }

    private void openHome() {
        Intent home = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setClassName(getPackageName(), MainActivity.class.getName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(home);
            failed = false;
        } catch (RuntimeException e) {
            // Never swallow Home without a screen to show: the next presses go to the system.
            failed = true;
        }
    }

    /**
     * Some TVs (Panasonic 4KTV-3MUP) handle Home before accessibility services see it, so the stock home
     * screen comes up anyway. Bring this launcher over it as soon as it shows; if it shows again within
     * STAY_MS of that (Home pressed again), leave it: that is the way to the stock home screen.
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence cls = event.getClassName();
        if (cls == null || !STOCK_HOMES.contains(cls.toString())) return;
        long now = SystemClock.uptimeMillis();
        if (now - lastBounce < STAY_MS) return;
        lastBounce = now;
        openHome();
    }

    @Override
    public void onInterrupt() {
    }
}
