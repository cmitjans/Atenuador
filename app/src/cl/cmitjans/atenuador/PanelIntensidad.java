package cl.cmitjans.atenuador;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Panel pequeño que aparece al mantener presionado el botón de ajustes rápidos.
 * Flota sobre lo que estabas viendo (sin oscurecer el fondo), así ves la intensidad real
 * mientras mueves la barra. Tocar fuera del panel lo cierra.
 */
public class PanelIntensidad extends Activity {
    private static final int CARD = Color.rgb(28, 32, 39);
    private static final int TEXT = Color.rgb(231, 235, 240);
    private static final int MUTED = Color.rgb(154, 165, 179);
    private static final int ACCENT = Color.rgb(110, 162, 239);

    private Switch sw;
    private SeekBar slider;
    private TextView level;
    private boolean updating;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        if (!Settings.canDrawOverlays(this) && !Sistema.enUso(this)) {
            // Sin el permiso no hay nada que ajustar: llevar a la app, que explica cómo darlo
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Fondo transparente que cierra el panel al tocarlo
        FrameLayout root = new FrameLayout(this);
        root.setOnClickListener(v -> finish());

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setClickable(true); // los toques dentro del panel no lo cierran
        box.setPadding(dp(22), dp(18), dp(22), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(24));
        box.setBackground(bg);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text("Atenuador", 20, TEXT), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        sw = new Switch(this);
        row.addView(sw);
        box.addView(row);

        level = text("", 15, MUTED);
        level.setPadding(0, dp(14), 0, dp(4));
        box.addView(level);

        slider = new SeekBar(this);
        box.addView(slider);

        Button more = new Button(this);
        more.setText("Más opciones");
        more.setAllCaps(false);
        more.setTextColor(ACCENT);
        more.setBackgroundColor(Color.TRANSPARENT);
        more.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mlp.gravity = Gravity.END;
        box.addView(more, mlp);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        lp.setMargins(dp(12), 0, dp(12), dp(28));
        root.addView(box, lp);
        setContentView(root);

        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                level.setText("Intensidad: " + p + " %");
                if (!user || updating) return;
                Prefs.setLevel(PanelIntensidad.this, p);
                // Mover la barra con el atenuador apagado lo enciende
                if (!Prefs.isOn(PanelIntensidad.this)) sw.setChecked(true);
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        sw.setOnCheckedChangeListener((b, on) -> {
            if (updating || on == Prefs.isOn(this)) return;
            if (on) DimService.start(this); else DimService.stop(this);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sw == null) return;
        updating = true;
        sw.setChecked(Prefs.isOn(this));
        slider.setMax(Prefs.maxLevel(this));
        slider.setProgress(Prefs.level(this));
        level.setText("Intensidad: " + Prefs.level(this) + " %");
        updating = false;
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
