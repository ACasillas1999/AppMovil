package com.example.app_pedidos.ui.Inspeccion;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.Volley;
import com.example.app_pedidos.ApiConfig;
import com.example.app_pedidos.MainActivity;
import com.example.app_pedidos.R;
import com.example.app_pedidos.network.Utf8JsonObjectRequest;
import com.example.app_pedidos.ui.common.Notifier;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class InspeccionHoyActivity extends AppCompatActivity {
    private LinearLayout container;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspeccion_hoy);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        SharedPreferences sp = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String username = sp.getString("username", "");
        TextView navHeaderSubtitle = navigationView.getHeaderView(0).findViewById(R.id.NombreLogin);
        if (navHeaderSubtitle != null) navHeaderSubtitle.setText(username);
        navigationView.setNavigationItemSelectedListener(item -> {
            handleNavigation(item);
            return true;
        });

        container = findViewById(R.id.container_hoy);
        cargarDatos();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void handleNavigation(MenuItem item) {
        int id = item.getItemId();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("OPEN_DEST", id);
        startActivity(intent);
        finish();
    }

    private void cargarDatos() {
        SharedPreferences sp = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String username = sp.getString("username", "");
        if (username.isEmpty()) { Notifier.error(this, "Usuario no disponible"); return; }
        String url = ApiConfig.BASE_URL + "/Pedidos_GA/App/obtener_checklist_hoy.php?username=" + android.net.Uri.encode(username);
        Utf8JsonObjectRequest req = new Utf8JsonObjectRequest(
                Request.Method.GET, url, null,
                resp -> {
                    if (!resp.optBoolean("ok", false)) { Notifier.info(this, "Sin inspección de hoy"); return; }
                    JSONArray items = resp.optJSONArray("items");
                    if (items == null || items.length() == 0) { Notifier.info(this, "Sin inspección de hoy"); return; }
                    render(items);
                },
                err -> Notifier.connectionLost(this, "Error de conexión", "Reintentar", this::cargarDatos)
        );
        Volley.newRequestQueue(this).add(req);
    }

    private void render(JSONArray items) {
        container.removeAllViews();
        Map<String, LinearLayout> bySection = new LinkedHashMap<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject it = items.optJSONObject(i);
            if (it == null) continue;
            String seccion = it.optString("seccion", "?");
            String item = it.optString("item", "?");
            String cal = it.optString("calificacion", "?");
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
            if ("Mal".equalsIgnoreCase(cal)) { val.setTextColor(0xFFAA0000); }
            else if ("Bien".equalsIgnoreCase(cal)) { val.setTextColor(0xFF0A7D00); }
            else { val.setTextColor(0xFF666666); }
            if (auto) { lbl.setText(lbl.getText() + " (marcado por sección)"); lbl.setTextColor(0xFF5555AA); }
            TextView tvObs = row.findViewById(R.id.item_obs);
            String obsTrim = (obs == null) ? "" : obs.trim();
            boolean hasObs = !(obsTrim.isEmpty() || "null".equalsIgnoreCase(obsTrim));
            if ("Mal".equalsIgnoreCase(cal)) {
                tvObs.setText("Observación: " + (hasObs ? obsTrim : ""));
                tvObs.setVisibility(View.VISIBLE);
            } else {
                tvObs.setVisibility(View.GONE);
            }
            sec.addView(row);
        }
    }
}

