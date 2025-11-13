package com.example.app_pedidos.ui.Inspeccion;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.Volley;
import com.example.app_pedidos.ApiConfig;
import com.example.app_pedidos.R;
import com.example.app_pedidos.network.Utf8JsonObjectRequest;
import com.example.app_pedidos.ui.common.Notifier;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InspeccionVehicularActivity extends AppCompatActivity {
    public static final String EXTRA_KILOMETRAJE = "extra_kilometraje";
    private LinearLayout sectionsContainer;
    private Button btnGuardar;
    private final List<Seccion> secciones = new ArrayList<>();
    private final Map<Item, RadioGroup> itemRadioMap = new LinkedHashMap<>();
    private final Map<Item, EditText> itemObsMap = new LinkedHashMap<>();
    private final Map<Item, Boolean> itemAutoMap = new LinkedHashMap<>();
    private final Map<Item, View> itemViewMap = new LinkedHashMap<>();
    

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspeccion_vehicular);
        sectionsContainer = findViewById(R.id.sections_container);
        btnGuardar = findViewById(R.id.btn_guardar_inspeccion);
        buildModel();
        buildUi();
        btnGuardar.setOnClickListener(v -> { if (validarTodo()) enviarChecklist(); });

    }

    private void buildModel() {
        secciones.clear();
        secciones.add(new Seccion("SISTEMA DE LUCES", new String[]{
                "Luz Delantera alta (NN)*",
                "Luz Delantera baja (NN)*",
                "Luces de emergencia (NN)*",
                "Luces neblineros",
                "Luz direccional",
                "Luz de freno posterior"
        }));
        secciones.add(new Seccion("PARTE EXTERNA", new String[]{
                "Parabrisas delantera",
                "Parabrisas posterior",
                "Limpia parabrisas",
                "Vidrio de parabrisas",
                "Espejo retrovisor",
                "Espejos laterales",
                "Orden y limpieza de cabina",
                "Limpieza exterior"
        }));
        secciones.add(new Seccion("PARTE INTERNA", new String[]{
                "Estado de Tablero / Indicadores operativos",
                "Freno de mano (NN)*",
                "Freno de servicio (NN)*",
                "Cinturón de seguridad Chofer (NN)*",
                "Cinturón de seguridad copiloto (NN)*",
                "Cinturón de seguridad asiento posterior (NN)*",
                "Espejo retrovisor antideslumbrante",
                "Linterna de mano"
        }));
        secciones.add(new Seccion("ESTADO DE LLANTAS", new String[]{
                "Llantas",
                "Herramientas y palanca de ruedas"
        }));
        secciones.add(new Seccion("ACCESORIOS DE SEGURIDAD", new String[]{
                "Conos de Seguridad (2)",
                "Extintor"
        }));
        secciones.add(new Seccion("TAPAS Y OTROS", new String[]{
                "Tapa de tanque de gasolina y/o petróleo",
                "Gata hidráulica"
        }));
        secciones.add(new Seccion("ROTULADO", new String[]{
                "Rotulado general"
        }));
    }

    private void buildUi() {
        getLayoutInflater().inflate(R.layout.layout_inspeccion_header, sectionsContainer, true);
        for (Seccion s : secciones) {
            View secView = getLayoutInflater().inflate(R.layout.layout_inspeccion_section, sectionsContainer, false);
            TextView tvTitle = secView.findViewById(R.id.section_title);
            RadioGroup groupAll = secView.findViewById(R.id.group_all_section);
            LinearLayout itemsContainer = secView.findViewById(R.id.items_container);
            tvTitle.setText(s.nombre);

            for (String nombreItem : s.items) {
                View itemView = getLayoutInflater().inflate(R.layout.layout_inspeccion_item, itemsContainer, false);
                TextView tv = itemView.findViewById(R.id.item_label);
                RadioGroup rg = itemView.findViewById(R.id.item_group);
                EditText obs = itemView.findViewById(R.id.item_observacion);
                tv.setText(nombreItem);
                Item item = new Item(s.nombre, nombreItem, esCritico(nombreItem));
                s.itemList.add(item);
                itemRadioMap.put(item, rg);
                itemObsMap.put(item, obs);
                itemAutoMap.put(item, false);
                itemViewMap.put(item, itemView.findViewById(R.id.item_root));
                rg.setOnCheckedChangeListener((group, checkedId) -> {
                    RadioButton rbSel = group.findViewById(checkedId);
                    String cal = rbSel == null ? null : rbSel.getText().toString();
                    if ("Mal".equals(cal)) { obs.setVisibility(View.VISIBLE); }
                    else { obs.setText(""); obs.setVisibility(View.GONE); }
                    // Edición manual desmarca el flag auto
                    itemAutoMap.put(item, false);
                    applyAutoStyle(item, false);
                });
                itemsContainer.addView(itemView);
            }

            groupAll.setOnCheckedChangeListener((g, checkedId) -> {
                String val = checkedIdToValue(checkedId);
                if (val == null) return;
                for (Item it : s.itemList) {
                    RadioGroup rg = itemRadioMap.get(it);
                    if (rg != null) {
                        checkValueOnGroup(rg, val);
                        EditText obs = itemObsMap.get(it);
                        if (obs != null) {
                            if ("Mal".equals(val)) { obs.setVisibility(View.VISIBLE); }
                            else { obs.setText(""); obs.setVisibility(View.GONE); }
                        }
                        // Marcar como asignado por sección
                        itemAutoMap.put(it, true);
                        applyAutoStyle(it, true);
                    }
                }
            });

            sectionsContainer.addView(secView);
        }
    }

    private boolean esCritico(String nombre) { return nombre != null && nombre.contains("(NN)*"); }

    private void checkValueOnGroup(RadioGroup rg, String val) {
        for (int i = 0; i < rg.getChildCount(); i++) {
            View v = rg.getChildAt(i);
            if (v instanceof RadioButton) {
                RadioButton rb = (RadioButton) v;
                if (val.equals(rb.getText().toString())) { rb.setChecked(true); return; }
            }
        }
    }

    private String checkedIdToValue(int id) {
        if (id == R.id.opt_all_bien) return "Bien";
        if (id == R.id.opt_all_mal) return "Mal";
        if (id == R.id.opt_all_na) return "N/A";
        return null;
    }

    private boolean validarTodo() {
        boolean ok = true; Item first = null; String firstMsg = null;
        for (Map.Entry<Item, RadioGroup> e : itemRadioMap.entrySet()) {
            Item it = e.getKey(); RadioGroup rg = e.getValue();
            int sel = rg.getCheckedRadioButtonId();
            if (sel == -1) { ok = false; if (first == null) { first = it; firstMsg = "Hay ítems sin calificación. Revisa: "+it.item; } continue; }
            RadioButton rb = rg.findViewById(sel);
            String cal = rb==null?null:rb.getText().toString();
            if ("Mal".equals(cal)) {
                EditText obs = itemObsMap.get(it);
                String txt = obs==null?null:obs.getText().toString().trim();
                if (txt==null || txt.isEmpty()) {
                    ok = false; if (first == null) { first = it; firstMsg = "Falta observación para ítem en estado 'Mal': "+it.item; }
                    if (obs != null) obs.setError("Requerido si es 'Mal'");
                }
            }
        }
        if (!ok && first != null) Notifier.error(this, firstMsg);
        return ok;
    }

    private void enviarChecklist() {
        String url = ApiConfig.BASE_URL + "/Pedidos_GA/App/guardar_checklist.php";
        SharedPreferences sp = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String username = sp.getString("username", "");
        int km = getIntent().getIntExtra(EXTRA_KILOMETRAJE, -1);
        if (username.isEmpty()) { Notifier.error(this, "Usuario no disponible"); return; }
        JSONObject body = new JSONObject(); JSONArray items = new JSONArray();
        String fecha = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
        try {
            body.put("username", username); if (km>=0) body.put("kilometraje", km); body.put("fecha_inspeccion", fecha);
            for (Map.Entry<Item, RadioGroup> e : itemRadioMap.entrySet()) {
                Item it = e.getKey(); RadioGroup rg = e.getValue(); int sel = rg.getCheckedRadioButtonId();
                RadioButton rb = rg.findViewById(sel); String cal = rb==null?null:rb.getText().toString();
                JSONObject o = new JSONObject(); o.put("seccion", it.seccion); o.put("item", it.item); o.put("calificacion", cal);
                if ("Mal".equals(cal)) {
                    EditText obs = itemObsMap.get(it);
                    String txt = obs==null?null:obs.getText().toString().trim();
                    if (txt != null && !txt.isEmpty()) o.put("observacion", txt);
                }
                items.put(o);
            }
            body.put("items", items);
        } catch (JSONException ex) { Notifier.error(this, "Error al preparar datos"); return; }
        Utf8JsonObjectRequest req = new Utf8JsonObjectRequest(
                Request.Method.POST, url, body,
                response -> { if (response.optBoolean("ok", false)) { Notifier.success(this, "Inspección guardada"); setResult(RESULT_OK); finish(); } else { Notifier.error(this, response.optString("error","No se pudo guardar")); } },
                error -> Notifier.connectionLost(this, "Error de conexión", "Reintentar", this::enviarChecklist)
        );
        Volley.newRequestQueue(this).add(req);
    }

    private void applyAutoStyle(Item it, boolean isAuto) {
        View row = itemViewMap.get(it);
        if (row == null) return;
        if (isAuto) {
            row.setBackgroundColor(0xFFEFF3FF); // azul muy sutil
        } else {
            row.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    static class Seccion {
        final String nombre; final String[] items; final List<Item> itemList = new ArrayList<>();
        Seccion(String n, String[] it){ nombre=n; items=it; }
    }
    static class Item { final String seccion,item; final boolean critico; Item(String s,String i,boolean c){ seccion=s; item=i; critico=c; } }
}
