package cl.cmitjans.atenuador;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/**
 * Mantiene una capa negra semitransparente sobre toda la pantalla.
 * La capa no recibe toques: todo lo que tocas llega a la app de abajo.
 */
public class DimService extends Service {
    static final String ACTION_STOP = "cl.cmitjans.atenuador.STOP";
    static final String ACTION_DARKER = "cl.cmitjans.atenuador.DARKER";
    static final String ACTION_LIGHTER = "cl.cmitjans.atenuador.LIGHTER";

    private static final String CHANNEL = "atenuador";
    private static final int NOTIF_ID = 1;
    private static final int STEP = 10;

    private WindowManager wm;
    private View overlay;
    private WindowManager.LayoutParams lp;

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (p, key) -> {
        if (Prefs.KEY_LEVEL.equals(key)) {
            applyLevel();
            showNotification();
        }
    };

    static void start(Context c) {
        Prefs.setOn(c, true);
        Intent i = new Intent(c, DimService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
    }

    static void stop(Context c) {
        Prefs.setOn(c, false);
        c.stopService(new Intent(c, DimService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createChannel();
        startInForeground();
        Prefs.get(this).registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            Prefs.setOn(this, false);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_DARKER.equals(action)) Prefs.setLevel(this, Prefs.level(this) + STEP);
        if (ACTION_LIGHTER.equals(action)) Prefs.setLevel(this, Prefs.level(this) - STEP);

        if (!Settings.canDrawOverlays(this)) {
            Prefs.setOn(this, false);
            stopSelf();
            return START_NOT_STICKY;
        }
        Prefs.setOn(this, true);
        addOverlay();
        updateTile();
        return START_STICKY;
    }

    private void addOverlay() {
        if (overlay != null) return;
        overlay = new View(this);
        overlay.setBackgroundColor(Color.BLACK);

        lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode = Build.VERSION.SDK_INT >= 30
                    ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        if (Build.VERSION.SDK_INT >= 30) lp.setFitInsetsTypes(0);
        lp.alpha = Prefs.level(this) / 100f;
        try {
            wm.addView(overlay, lp);
        } catch (RuntimeException e) {
            overlay = null;
            Prefs.setOn(this, false);
            stopSelf();
        }
    }

    private void applyLevel() {
        if (overlay == null || lp == null) return;
        lp.alpha = Prefs.level(this) / 100f;
        try { wm.updateViewLayout(overlay, lp); } catch (RuntimeException ignored) { }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Atenuador activo",
                NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Aviso mientras la pantalla está atenuada, con botones para ajustarla");
        ch.setShowBadge(false);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    private Notification buildNotification() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), flags);
        PendingIntent stop = PendingIntent.getService(this, 1,
                new Intent(this, DimService.class).setAction(ACTION_STOP), flags);
        PendingIntent darker = PendingIntent.getService(this, 2,
                new Intent(this, DimService.class).setAction(ACTION_DARKER), flags);
        PendingIntent lighter = PendingIntent.getService(this, 3,
                new Intent(this, DimService.class).setAction(ACTION_LIGHTER), flags);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        return b.setSmallIcon(R.drawable.ic_tile)
                .setContentTitle("Pantalla atenuada al " + Prefs.level(this) + " %")
                .setContentText("Toca para ajustar")
                .setContentIntent(open)
                .setOngoing(true)
                .setShowWhen(false)
                .addAction(new Notification.Action.Builder(null, "Más claro", lighter).build())
                .addAction(new Notification.Action.Builder(null, "Más oscuro", darker).build())
                .addAction(new Notification.Action.Builder(null, "Apagar", stop).build())
                .build();
    }

    private void startInForeground() {
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIF_ID, n);
        }
    }

    private void showNotification() {
        try {
            getSystemService(NotificationManager.class).notify(NOTIF_ID, buildNotification());
        } catch (RuntimeException ignored) { }
    }

    private void updateTile() {
        TileService.requestListeningState(this, new ComponentName(this, DimTileService.class));
    }

    @Override
    public void onDestroy() {
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(listener);
        if (overlay != null) {
            try { wm.removeView(overlay); } catch (RuntimeException ignored) { }
            overlay = null;
        }
        updateTile();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
