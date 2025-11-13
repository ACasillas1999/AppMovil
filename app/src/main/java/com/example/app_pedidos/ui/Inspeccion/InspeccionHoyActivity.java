package com.example.app_pedidos.ui.Inspeccion;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
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
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class InspeccionHoyActivity extends AppCompatActivity {
    private LinearLayout container;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspeccion_hoy);
        container = findViewById(R.id.container_hoy);
        cargarDatos();
    }

    private void cargarDatos() {
        SharedPreferences sp = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String username = sp.getString("username", "");
        if (username.isEmpty()) { Notifier.error(this, "Usuario no disponible"); return; }
        String url = ApiConfig.BASE_URL + "/Pedidos_GA/App/obtener_checklist_hoy.php?username=" + android.net.Uri.encode(username);
        Utf8JsonObjectRequest req = new Utf8JsonObjectRequest(
                Request.Method.GET, url, null,
                resp -> {
                    if (!resp.optBoolean("ok", false)) { Notifier.info(this, "Sin inspecciÃ³n de hoy"); return; }
                    JSONArray items = resp.optJSONArray("items");
                    if (items == null || items.length() == 0) { Notifier.info(this, "Sin inspecciÃ³n de hoy"); return; }
                    render(items);
                },
                err -> Notifier.connectionLost(this, "Error de conexiÃ³n", "Reintentar", this::cargarDatos)
        );
        Volley.newRequestQueue(this).add(req);
    }

    private void render(JSONArray items) {
        container.removeAllViews();
        Map<String, LinearLayout> bySection = new LinkedHashMap<>();
        for (int i=0;i<items.length();i++) {
            JSONObject it = items.optJSONObject(i);
            if (it==null) continue;
            String seccion = it.optString("seccion","?");
            String item = it.optString("item","?");
            String cal = it.optString("calificacion","?");
            String obs = it.optString("observacion", "");
            boolean auto = it.optBoolean("auto", false);

            LinearLayout sec = bySection.get(seccion);
            if (sec == null) {
                View secView = getLayoutInflater().inflate(R.layout.layout_inspeccion_section_readonly, container, false);
                TextView title = secView.findViewById(R.id.section_title);
                title.setText(seccion);
                container.addView(secView);
                sec = secView.findViewById(R.id.items_container);
                bySection.put(seccion, sec);
            }

            View row = getLayoutInflater().inflate(R.layout.layout_inspeccion_item_readonly, sec, false);
            TextView lbl = row.findViewById(R.id.item_label);
            TextView val = row.findViewById(R.id.item_value);
            lbl.setText(item);
            val.setText(cal);
            // Colores por calificación
            if ("Mal".equalsIgnoreCase(cal)) { val.setTextColor(0xFFAA0000); }
            else if ("Bien".equalsIgnoreCase(cal)) { val.setTextColor(0xFF0A7D00); }
            else { val.setTextColor(0xFF666666); }
            // Estilo para marcados por secciÃ³n
            if (auto) { lbl.setText(lbl.getText() + " (marcado por secciÃ³n)"); lbl.setTextColor(0xFF5555AA); }
            TextView tvObs = row.findViewById(R.id.item_obs);
            String obsTrim = (obs == null) ? "" : obs.trim();
            boolean hasObs = !(obsTrim.isEmpty() || "null".equalsIgnoreCase(obsTrim));
            if ("Mal".equalsIgnoreCase(cal)) {
                // Mostrar siempre la observación cuando está en "Mal"
                // Usar texto sin acento para evitar símbolos raros en algunas codificaciones
                tvObs.setText("Observacion: " + (hasObs ? obsTrim : ""));
                tvObs.setVisibility(View.VISIBLE);
            } else {
                tvObs.setVisibility(View.GONE);
            }
            sec.addView(row);
        }
    }
}

