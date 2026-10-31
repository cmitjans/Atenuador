package cl.cmitjans.atenuador;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/** Guarda la intensidad elegida y si el atenuador está encendido. */
final class Prefs {
    static final String NAME = "atenuador";
    static final String KEY_LEVEL = "level";
    static final String KEY_ON = "on";
    static final String KEY_OFF_ON_LOCK = "off_on_lock";
    static final String KEY_SYSTEM = "system_mode";

    /** Android no deja que una capa normal que deja pasar los toques tape más del 80 %. */
    static final int MAX_LEVEL = 80;
    /** Con el modo de accesibilidad activo ese límite no aplica; se deja un margen para seguir viendo. */
    static final int MAX_LEVEL_A11Y = 95;
    static final int DEFAULT_LEVEL = 50;

    private Prefs() {}

    /**
     * Lado de la capa: un cuadrado del tamaño del lado más largo de la pantalla (más un margen).
     * Así, al girar el teléfono la capa ya cubre la nueva orientación y no aparece un borde
     * sin oscurecer mientras Android recalcula el tamaño.
     */
    @SuppressWarnings("deprecation")
    static int overlaySide(Context c) {
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) c.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(dm);
        return Math.max(dm.widthPixels, dm.heightPixels) + 200;
    }

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    /** En modo sistema la escala es la de Android (0 a 100); con la capa hay límites para seguir viendo. */
    static int maxLevel(Context c) {
        if (Sistema.enUso(c)) return 100;
        return DimAccessibilityService.isActive() ? MAX_LEVEL_A11Y : MAX_LEVEL;
    }

    static int level(Context c) {
        int v = get(c).getInt(KEY_LEVEL, DEFAULT_LEVEL);
        return Math.max(0, Math.min(maxLevel(c), v));
    }

    static void setLevel(Context c, int level) {
        get(c).edit().putInt(KEY_LEVEL, Math.max(0, Math.min(maxLevel(c), level))).apply();
    }

    static boolean systemMode(Context c) {
        return get(c).getBoolean(KEY_SYSTEM, false);
    }

    static void setSystemMode(Context c, boolean v) {
        get(c).edit().putBoolean(KEY_SYSTEM, v).apply();
    }

    static boolean isOn(Context c) {
        return get(c).getBoolean(KEY_ON, false);
    }

    static boolean offOnLock(Context c) {
        return get(c).getBoolean(KEY_OFF_ON_LOCK, false);
    }

    static void setOffOnLock(Context c, boolean v) {
        get(c).edit().putBoolean(KEY_OFF_ON_LOCK, v).apply();
    }

    static void setOn(Context c, boolean on) {
        get(c).edit().putBoolean(KEY_ON, on).apply();
    }
}
