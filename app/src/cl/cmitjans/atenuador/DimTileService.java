package cl.cmitjans.atenuador;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Botón en los ajustes rápidos (el panel que bajas desde arriba).
 * Tocarlo enciende o apaga con la última intensidad usada.
 * Mantenerlo presionado abre un panel pequeño para ajustar la intensidad (PanelIntensidad).
 */
public class DimTileService extends TileService {

    @Override
    public void onStartListening() {
        refresh();
    }

    @Override
    public void onClick() {
        if (!Settings.canDrawOverlays(this)) {
            openApp(); // falta el permiso: la app explica cómo darlo
            return;
        }
        if (Prefs.isOn(this)) DimService.stop(this); else DimService.start(this);
        refresh();
    }

    private void openApp() {
        Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, i,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        } else {
            startActivityAndCollapse(i);
        }
    }

    private void refresh() {
        Tile t = getQsTile();
        if (t == null) return;
        boolean on = Prefs.isOn(this);
        t.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        if (Build.VERSION.SDK_INT >= 29) {
            t.setSubtitle(on ? Prefs.level(this) + " %" : "Apagado");
        }
        t.updateTile();
    }
}
