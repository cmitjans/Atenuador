package cl.cmitjans.atenuador;

import android.content.Context;
import android.content.SharedPreferences;

/** Guarda la intensidad elegida y si el atenuador está encendido. */
final class Prefs {
    static final String NAME = "atenuador";
    static final String KEY_LEVEL = "level";
    static final String KEY_ON = "on";

    /** Android no deja que una capa que deja pasar los toques tape más del 80 %. */
    static final int MAX_LEVEL = 80;
    static final int DEFAULT_LEVEL = 50;

    private Prefs() {}

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    static int level(Context c) {
        int v = get(c).getInt(KEY_LEVEL, DEFAULT_LEVEL);
        return Math.max(0, Math.min(MAX_LEVEL, v));
    }

    static void setLevel(Context c, int level) {
        get(c).edit().putInt(KEY_LEVEL, Math.max(0, Math.min(MAX_LEVEL, level))).apply();
    }

    static boolean isOn(Context c) {
        return get(c).getBoolean(KEY_ON, false);
    }

    static void setOn(Context c, boolean on) {
        get(c).edit().putBoolean(KEY_ON, on).apply();
    }
}
