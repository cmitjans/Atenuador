package cl.cmitjans.atenuador;

import android.Manifest;
import android.app.Activity;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** Pantalla principal: encender, elegir intensidad y dar el permiso necesario. */
public class MainActivity extends Activity {
    private static final int BG = Color.rgb(14, 16, 20);
    private static final int CARD = Color.rgb(28, 32, 39);
    private static final int TEXT = Color.rgb(231, 235, 240);
    private static final int MUTED = Color.rgb(154, 165, 179);
    private static final int ACCENT = Color.rgb(110, 162, 239);

    private Switch toggle;
    private SeekBar slider;
    private TextView levelLabel;
    private LinearLayout permCard;
    private boolean updating;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(28));

        TextView title = text("Atenuador", 28, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);
        TextView sub = text("Oscurece la pantalla por debajo del brillo mínimo. Funciona sobre todas las apps.", 15, MUTED);
        sub.setPadding(0, dp(6), 0, dp(20));
        root.addView(sub);

        // Permiso que falta
        permCard = card();
        TextView permText = text("Para oscurecer la pantalla, Android necesita que permitas a Atenuador "
                + "mostrarse sobre otras apps. Es el único permiso que se aprueba a mano, y la app no tiene acceso a internet.", 15, TEXT);
        permCard.addView(permText);
        Button permBtn = button("Dar permiso");
        permBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()))));
        permCard.addView(permBtn, marginTop(dp(14)));
        root.addView(permCard, marginBottom(dp(16)));

        // Encendido e intensidad
        LinearLayout main = card();
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView onLabel = text("Atenuar pantalla", 18, TEXT);
        row.addView(onLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        toggle = new Switch(this);
        toggle.setOnCheckedChangeListener((b, checked) -> {
            if (updating) return;
            if (checked) turnOn(); else DimService.stop(this);
        });
        row.addView(toggle);
        main.addView(row);

        levelLabel = text("", 15, MUTED);
        levelLabel.setPadding(0, dp(18), 0, dp(4));
        main.addView(levelLabel);
        slider = new SeekBar(this);
        slider.setMax(Prefs.MAX_LEVEL);
        slider.setProgress(Prefs.level(this));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (user) Prefs.setLevel(MainActivity.this, p);
                showLevel(p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        main.addView(slider);
        TextView hint = text("Puedes ajustarla con la app encendida y ver el efecto al instante. "
                + "El máximo es 80 %, el límite que permite Android para que siempre puedas ver y tocar la pantalla.", 13, MUTED);
        hint.setPadding(0, dp(10), 0, 0);
        main.addView(hint);
        root.addView(main, marginBottom(dp(16)));

        // Acceso rápido
        LinearLayout quick = card();
        quick.addView(text("Acceso rápido", 18, TEXT));
        TextView qt = text("Agrega el botón Atenuador al panel que bajas desde arriba para encenderlo con un toque. "
                + "Mientras está activo, la notificación tiene botones para aclarar, oscurecer o apagar.", 14, MUTED);
        qt.setPadding(0, dp(6), 0, 0);
        quick.addView(qt);
        if (Build.VERSION.SDK_INT >= 33) {
            Button add = button("Agregar a los ajustes rápidos");
            add.setOnClickListener(v -> requestTile());
            quick.addView(add, marginTop(dp(14)));
        }
        root.addView(quick, marginBottom(dp(16)));

        TextView xiaomi = text("Si se apaga solo después de un rato: en Ajustes → Apps → Atenuador → Ahorro de batería, "
                + "elige \"Sin restricciones\".", 13, MUTED);
        root.addView(xiaomi);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean allowed = Settings.canDrawOverlays(this);
        permCard.setVisibility(allowed ? View.GONE : View.VISIBLE);
        updating = true;
        toggle.setEnabled(allowed);
        toggle.setChecked(allowed && Prefs.isOn(this));
        updating = false;
        slider.setProgress(Prefs.level(this));
        showLevel(Prefs.level(this));
    }

    private void turnOn() {
        if (!Settings.canDrawOverlays(this)) { refresh(); return; }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        DimService.start(this);
    }

    private void requestTile() {
        if (Build.VERSION.SDK_INT < 33) return;
        StatusBarManager sbm = getSystemService(StatusBarManager.class);
        sbm.requestAddTileService(new ComponentName(this, DimTileService.class), "Atenuador",
                Icon.createWithResource(this, R.drawable.ic_tile), getMainExecutor(), r -> { });
    }

    private void showLevel(int p) {
        levelLabel.setText("Intensidad: " + p + " %");
    }

    // --- pequeñas ayudas de diseño ---

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(16));
        c.setBackground(bg);
        return c;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(Color.rgb(13, 21, 32));
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ACCENT);
        bg.setCornerRadius(dp(24));
        b.setBackground(bg);
        b.setPadding(dp(18), dp(10), dp(18), dp(10));
        return b;
    }

    private LinearLayout.LayoutParams marginTop(int m) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = m;
        return lp;
    }

    private LinearLayout.LayoutParams marginBottom(int m) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = m;
        return lp;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
