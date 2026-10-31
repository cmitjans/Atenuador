package cl.cmitjans.atenuador;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.StatusBarManager;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.widget.Toast;

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
    private LinearLayout notifCard;
    private Switch lockToggle;
    private Switch sysToggle;
    private TextView sysStatus;
    private LinearLayout sysSetup;
    private TextView a11yStatus;
    private Button a11yBtn;
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

        // Aviso si las notificaciones están bloqueadas: sin ellas no hay ícono que recuerde que está encendido
        notifCard = card();
        notifCard.addView(text("Las notificaciones de Atenuador están desactivadas. Si es a propósito, está bien: "
                + "el botón de ajustes rápidos muestra si está encendido. Agrégalo para no olvidarlo activado.", 15, TEXT));
        Button notifBtn = button("Activar notificaciones");
        notifBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())));
        notifCard.addView(notifBtn, marginTop(dp(14)));
        root.addView(notifCard, marginBottom(dp(16)));

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
        slider.setMax(Prefs.maxLevel(this));
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
                + "El máximo es 80 %, el límite que pone Android para que siempre puedas ver y tocar la pantalla; "
                + "con el modo \"cubrir barras del sistema\" llega a 95 %.", 13, MUTED);
        hint.setPadding(0, dp(10), 0, 0);
        main.addView(hint);

        LinearLayout lockRow = new LinearLayout(this);
        lockRow.setGravity(Gravity.CENTER_VERTICAL);
        lockRow.setPadding(0, dp(18), 0, 0);
        TextView lockLabel = text("Apagar al bloquear el teléfono", 16, TEXT);
        lockRow.addView(lockLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        lockToggle = new Switch(this);
        lockToggle.setOnCheckedChangeListener((b, checked) -> { if (!updating) Prefs.setOffOnLock(this, checked); });
        lockRow.addView(lockToggle);
        main.addView(lockRow);
        TextView lockHint = text("Así no queda encendido sin que te des cuenta. Al desbloquear, la pantalla vuelve a su brillo normal.", 13, MUTED);
        lockHint.setPadding(0, dp(4), 0, 0);
        main.addView(lockHint);
        root.addView(main, marginBottom(dp(16)));

        // Acceso rápido
        LinearLayout quick = card();
        quick.addView(text("Acceso rápido", 18, TEXT));
        TextView qt = text("Agrega el botón Atenuador al panel que bajas desde arriba. Un toque lo enciende o apaga con la última "
                + "intensidad; manteniéndolo presionado aparece un panel pequeño para ajustarla sobre lo que estás viendo. "
                + "Si prefieres no ver la notificación, puedes desactivarla en los ajustes de Android: el botón muestra si está "
                + "encendido y con qué intensidad.", 14, MUTED);
        qt.setPadding(0, dp(6), 0, 0);
        quick.addView(qt);
        if (Build.VERSION.SDK_INT >= 33) {
            Button add = button("Agregar a los ajustes rápidos");
            add.setOnClickListener(v -> requestTile());
            quick.addView(add, marginTop(dp(14)));
        }
        root.addView(quick, marginBottom(dp(16)));

        // Modo accesibilidad: cubrir barra de estado y panel
        LinearLayout bars = card();
        bars.addView(text("Cubrir barras del sistema", 18, TEXT));
        TextView bt = text("Android no deja que una app normal oscurezca la barra de hora y batería ni el panel que bajas "
                + "desde arriba. Si activas Atenuador en Accesibilidad, la capa cubre también esas partes. "
                + "Está configurado para no leer la pantalla, no recibir eventos de otras apps y no tocar por ti: "
                + "solo dibuja la capa.", 14, MUTED);
        bt.setPadding(0, dp(6), 0, dp(8));
        bars.addView(bt);
        a11yStatus = text("", 15, TEXT);
        bars.addView(a11yStatus);
        a11yBtn = button("Abrir ajustes de Accesibilidad");
        a11yBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        bars.addView(a11yBtn, marginTop(dp(14)));
        TextView restricted = text("Si Atenuador aparece bloqueado (\"Ajuste restringido\"): abre la información de la app, "
                + "toca el menú ⋮ de arriba a la derecha y elige \"Permitir ajustes restringidos\". Luego vuelve a Accesibilidad.", 13, MUTED);
        restricted.setPadding(0, dp(12), 0, 0);
        bars.addView(restricted);
        Button info = button("Abrir información de la app");
        info.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()))));
        bars.addView(info, marginTop(dp(10)));
        root.addView(bars, marginBottom(dp(16)));

        // Modo sistema (avanzado): la atenuación extra de Android, sin capa
        LinearLayout sys = card();
        sys.addView(text("Modo sistema (avanzado)", 18, TEXT));
        TextView st = text("Usa la \"atenuación extra\" que trae Android en vez de una capa encima. El sistema oscurece la "
                + "imagen por su cuenta: cubre también la barra de la hora y el panel, no parpadea al abrir apps ni al girar, "
                + "y no necesita el modo de accesibilidad.", 14, MUTED);
        st.setPadding(0, dp(6), 0, dp(8));
        sys.addView(st);
        sysStatus = text("", 15, TEXT);
        sys.addView(sysStatus);

        LinearLayout sysRow = new LinearLayout(this);
        sysRow.setGravity(Gravity.CENTER_VERTICAL);
        sysRow.setPadding(0, dp(12), 0, 0);
        sysRow.addView(text("Usar modo sistema", 16, TEXT), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        sysToggle = new Switch(this);
        sysToggle.setOnCheckedChangeListener((b, on) -> {
            if (updating) return;
            Prefs.setSystemMode(this, on);
            refresh();
        });
        sysRow.addView(sysToggle);
        sys.addView(sysRow);

        sysSetup = new LinearLayout(this);
        sysSetup.setOrientation(LinearLayout.VERTICAL);
        sysSetup.addView(text("Android solo deja cambiar este ajuste a apps con un permiso especial que se da una vez desde un "
                + "computador (no se puede pedir en pantalla). Ese permiso solo permite cambiar ajustes del sistema: "
                + "no da acceso a tus archivos, mensajes ni internet.", 14, MUTED));
        TextView cmd = text(Sistema.COMANDO, 13, TEXT);
        cmd.setTypeface(Typeface.MONOSPACE);
        cmd.setTextIsSelectable(true);
        cmd.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable cbg = new GradientDrawable();
        cbg.setColor(BG);
        cbg.setCornerRadius(dp(10));
        cmd.setBackground(cbg);
        sysSetup.addView(cmd, marginTop(dp(12)));
        Button copy = button("Copiar comando");
        copy.setOnClickListener(v -> {
            getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("comando", Sistema.COMANDO));
            Toast.makeText(this, "Comando copiado", Toast.LENGTH_SHORT).show();
        });
        sysSetup.addView(copy, marginTop(dp(10)));
        sysSetup.setPadding(0, dp(12), 0, 0);
        sys.addView(sysSetup);
        root.addView(sys, marginBottom(dp(16)));

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
        boolean allowed = Settings.canDrawOverlays(this) || Sistema.enUso(this);
        permCard.setVisibility(allowed ? View.GONE : View.VISIBLE);
        boolean notifOk = getSystemService(NotificationManager.class).areNotificationsEnabled();
        notifCard.setVisibility(allowed && !notifOk ? View.VISIBLE : View.GONE);
        updating = true;
        lockToggle.setChecked(Prefs.offOnLock(this));
        boolean sysOk = Sistema.permitido(this);
        sysToggle.setEnabled(sysOk);
        sysToggle.setChecked(sysOk && Prefs.systemMode(this));
        sysSetup.setVisibility(sysOk ? View.GONE : View.VISIBLE);
        sysStatus.setText(!sysOk ? "Falta el permiso (ver abajo)"
                : Prefs.systemMode(this) ? "✓ En uso. Si al encender el atenuador no cambia nada, tu teléfono no trae esta función: desactiva este modo."
                : "Permiso dado. Activa el interruptor para usarlo.");
        sysStatus.setTextColor(sysOk ? ACCENT : MUTED);
        toggle.setEnabled(allowed);
        toggle.setChecked(allowed && Prefs.isOn(this));
        updating = false;
        slider.setMax(Prefs.maxLevel(this));
        slider.setProgress(Prefs.level(this));
        boolean a11y = DimAccessibilityService.isActive();
        boolean enSettings = a11yEnabledInSettings();
        if (a11y) {
            a11yStatus.setText("✓ Activo: la capa cubre toda la pantalla");
            a11yStatus.setTextColor(ACCENT);
        } else if (enSettings) {
            // Android lo dejó marcado como activo pero no lo volvió a conectar (pasa en Xiaomi cuando el
            // sistema cierra la app, por ejemplo al cambiar un permiso como las notificaciones)
            a11yStatus.setText("⚠ Activado en Ajustes, pero Android lo desconectó. Desactívalo y vuelve a activarlo en Accesibilidad.");
            a11yStatus.setTextColor(Color.rgb(240, 180, 90));
        } else {
            a11yStatus.setText("Desactivado");
            a11yStatus.setTextColor(MUTED);
        }
        a11yBtn.setText(a11y ? "Desactivar en Accesibilidad" : "Abrir ajustes de Accesibilidad");
        showLevel(Prefs.level(this));
    }

    /** ¿Está Atenuador marcado como activo en Ajustes → Accesibilidad? (aunque no esté conectado) */
    private boolean a11yEnabledInSettings() {
        String list = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (list == null) return false;
        String mine = new ComponentName(this, DimAccessibilityService.class).flattenToString();
        String mineShort = new ComponentName(this, DimAccessibilityService.class).flattenToShortString();
        for (String s : list.split(":")) if (s.equalsIgnoreCase(mine) || s.equalsIgnoreCase(mineShort)) return true;
        return false;
    }

    private void turnOn() {
        if (!Settings.canDrawOverlays(this) && !Sistema.enUso(this)) { refresh(); return; }
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
