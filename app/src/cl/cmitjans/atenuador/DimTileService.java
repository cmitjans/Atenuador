package cl.cmitjans.atenuador;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Botón en los ajustes rápidos (el panel que bajas desde arriba).
 * Al tocarlo muestra un cuadro pequeño sobre el mismo panel, sin abrir la app,
 * para encender, apagar y ajustar la intensidad. Mantenerlo presionado abre la app.
 */
public class DimTileService extends TileService {
    private static final int CARD = Color.rgb(28, 32, 39);
    private static final int TEXT = Color.rgb(231, 235, 240);
    private static final int MUTED = Color.rgb(154, 165, 179);
    private static final int ACCENT = Color.rgb(110, 162, 239);

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
        try {
            showDialog(buildDialog());
        } catch (RuntimeException e) {
            // Si el sistema no deja mostrar el cuadro, al menos enciende o apaga
            toggle();
        }
    }

    private void toggle() {
        if (Prefs.isOn(this)) DimService.stop(this); else DimService.start(this);
        refresh();
    }

    private Dialog buildDialog() {
        Dialog d = new Dialog(this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(20), dp(22), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(24));
        box.setBackground(bg);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Atenuador", 20, TEXT);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch sw = new Switch(this);
        sw.setChecked(Prefs.isOn(this));
        row.addView(sw);
        box.addView(row);

        TextView level = text("", 15, MUTED);
        level.setPadding(0, dp(16), 0, dp(4));
        box.addView(level);

        SeekBar slider = new SeekBar(this);
        slider.setMax(Prefs.maxLevel());
        slider.setProgress(Prefs.level(this));
        level.setText("Intensidad: " + Prefs.level(this) + " %");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                level.setText("Intensidad: " + p + " %");
                if (!user) return;
                Prefs.setLevel(DimTileService.this, p);
                // Mover la barra con el atenuador apagado lo enciende
                if (!Prefs.isOn(DimTileService.this)) sw.setChecked(true);
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        box.addView(slider);

        sw.setOnCheckedChangeListener((b, on) -> {
            if (on == Prefs.isOn(this)) return;
            if (on) DimService.start(this); else DimService.stop(this);
            refresh();
        });

        Button more = new Button(this);
        more.setText("Más opciones");
        more.setAllCaps(false);
        more.setTextColor(ACCENT);
        more.setBackgroundColor(Color.TRANSPARENT);
        more.setOnClickListener(v -> { d.dismiss(); openApp(); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.END;
        lp.topMargin = dp(6);
        box.addView(more, lp);

        d.setContentView(box);
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        d.setOnDismissListener(x -> refresh());
        return d;
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

    private TextView text(String s, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(color);
        return v;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
