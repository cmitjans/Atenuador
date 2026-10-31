package cl.cmitjans.atenuador;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

/**
 * Modo opcional "cubrir barras del sistema".
 *
 * Android solo deja dibujar sobre la barra de hora y batería y sobre el panel de notificaciones
 * a los servicios de accesibilidad. Este servicio usa únicamente eso: no recibe eventos, no lee
 * la pantalla y no hace gestos (ver res/xml/accessibility_config.xml). Mientras está activo,
 * la capa la dibuja él en lugar de DimService.
 */
public class DimAccessibilityService extends AccessibilityService {
    private static volatile boolean active;

    private WindowManager wm;
    private View overlay;
    private WindowManager.LayoutParams lp;

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (p, key) -> sync();

    static boolean isActive() {
        return active;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        active = true;
        Prefs.get(this).registerOnSharedPreferenceChangeListener(listener);
        sync();
        DimService.onAccessibilityChanged();
    }

    /** Muestra, ajusta o quita la capa según lo guardado en Prefs. */
    private void sync() {
        if (!Prefs.isOn(this)) {
            removeOverlay();
            return;
        }
        float alpha = Prefs.level(this) / 100f;
        if (overlay == null) {
            overlay = new View(this);
            overlay.setBackgroundColor(Color.BLACK);
            lp = new WindowManager.LayoutParams(
                    Prefs.overlaySide(this),
                    Prefs.overlaySide(this),
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
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
            lp.alpha = alpha;
            try {
                wm.addView(overlay, lp);
            } catch (RuntimeException e) {
                overlay = null;
            }
        } else {
            lp.alpha = alpha;
            try { wm.updateViewLayout(overlay, lp); } catch (RuntimeException ignored) { }
        }
    }

    private void removeOverlay() {
        if (overlay == null) return;
        try { wm.removeView(overlay); } catch (RuntimeException ignored) { }
        overlay = null;
    }

    private void shutDown() {
        if (!active) return;
        active = false;
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(listener);
        removeOverlay();
        DimService.onAccessibilityChanged();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        shutDown();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        shutDown();
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { /* no recibe eventos */ }

    @Override
    public void onInterrupt() { }
}
