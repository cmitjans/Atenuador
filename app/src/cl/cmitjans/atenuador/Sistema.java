package cl.cmitjans.atenuador;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

/**
 * Modo sistema: usa la "atenuación extra" que trae Android (desde la versión 12) en vez de una capa.
 * La aplica el propio sistema al componer la imagen, así que cubre todo (barra de estado, panel,
 * animaciones al abrir apps) sin necesitar el modo de accesibilidad.
 *
 * Para cambiar ese ajuste hace falta el permiso WRITE_SECURE_SETTINGS, que ninguna app puede pedir
 * en pantalla: se da una sola vez desde un computador con
 *   adb shell pm grant cl.cmitjans.atenuador android.permission.WRITE_SECURE_SETTINGS
 * Ese permiso solo deja cambiar ajustes del sistema; no da acceso a archivos, mensajes ni internet.
 */
final class Sistema {
    static final String COMANDO = "adb shell pm grant cl.cmitjans.atenuador android.permission.WRITE_SECURE_SETTINGS";
    private static final String ACTIVO = "reduce_bright_colors_activated";
    private static final String NIVEL = "reduce_bright_colors_level";

    private Sistema() {}

    /** ¿Se dio el permiso desde el computador? */
    static boolean permitido(Context c) {
        return c.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
    }

    /** Modo sistema elegido y con permiso. */
    static boolean enUso(Context c) {
        return Prefs.systemMode(c) && permitido(c);
    }

    /** Enciende o apaga la atenuación extra con la intensidad indicada (0 a 100). */
    static boolean aplicar(Context c, boolean encender, int nivel) {
        if (!permitido(c)) return false;
        ContentResolver cr = c.getContentResolver();
        try {
            Settings.Secure.putInt(cr, NIVEL, Math.max(0, Math.min(100, nivel)));
            Settings.Secure.putInt(cr, ACTIVO, encender ? 1 : 0);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
