package com.example.app_pedidos.ui.Pedido;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.app_pedidos.ApiConfig;
import com.example.app_pedidos.R;

import org.json.JSONArray;
import org.json.JSONObject;

public class GrupoRutaActivity extends AppCompatActivity {

    private LinearLayout container;
    private TextView headerNombre;
    private TextView headerMeta;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grupo_ruta);

        container = findViewById(R.id.linearLayoutContainerGroup);
        headerNombre = findViewById(R.id.textGrupoNombreHeader);
        headerMeta = findViewById(R.id.textGrupoMetaHeader);

        int grupoId = getIntent().getIntExtra("GRUPO_ID", 0);
        String grupoNombre = getIntent().getStringExtra("GRUPO_NOMBRE");
        if (grupoNombre == null) grupoNombre = "";
        headerNombre.setText(grupoNombre.isEmpty() ? "Grupo" : grupoNombre);

        String url = ApiConfig.BASE_URL + "/Pedidos_GA/App/Consultar.php?grupo_id=" + grupoId + "&limit=1000";
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject g = response.optJSONObject("grupo");
                        if (g != null) {
                            String meta = (g.optString("sucursal", "") + " • " + g.optString("chofer_asignado", "") + " • " + g.optString("estado", "")).trim();
                            headerMeta.setText(meta);
                        }
                        JSONArray arr = response.optJSONArray("pedidos");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject p = arr.getJSONObject(i);
                                addPedidoCard(p);
                            }
                        }
                    } catch (Exception ignored) { }
                },
                error -> { /* ignore simple errors visually */ });
        Volley.newRequestQueue(this).add(req);
    }

    private void addPedidoCard(final JSONObject pedido) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0,0,0,16);
        card.setLayoutParams(params);
        card.setPadding(16,16,16,16);
        card.setBackgroundColor(Color.parseColor("#F5F5F5"));

        String estado = pedido.optString("ESTADO", "");
        ImageView logo = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(200,200);
        logo.setLayoutParams(imgParams);
        String sucursal = pedido.optString("SUCURSAL", "");
        int imgRes;
        switch (sucursal) {
            case "DEASA": imgRes = R.drawable.deasaazz; break;
            case "DIMEGSA": imgRes = R.drawable.dimegsa; break;
            case "AIESA": imgRes = R.drawable.aiesa; break;
            case "SEGSA": imgRes = R.drawable.segsa; break;
            case "FESA": imgRes = R.drawable.fesa; break;
            case "TAPATIA": imgRes = R.drawable.eitsa; break;
            case "GABSA": imgRes = R.drawable.gabl; break;
            case "ILUMINACION": imgRes = R.drawable.ilum; break;
            case "VALLARTA": imgRes = R.drawable.gabl; break;
            default: imgRes = R.drawable.gabl; break;
        }
        logo.setImageResource(imgRes);
        int filterColor;
        switch (estado) {
            case "ACTIVO": filterColor = Color.parseColor("#576977"); break;
            case "EN RUTA": filterColor = Color.parseColor("#7A5D3D"); break;
            case "REPROGRAMADO": filterColor = Color.parseColor("#715C5B"); break;
            case "EN TIENDA": filterColor = Color.parseColor("#78785E"); break;
            case "ENTREGADO": filterColor = Color.parseColor("#5A705B"); break;
            case "CANCELADO": filterColor = Color.parseColor("#6B5B76"); break;
            default: filterColor = Color.WHITE; break;
        }
        logo.setColorFilter(filterColor, PorterDuff.Mode.SRC_IN);

        TextView tId = new TextView(this);
        tId.setText("ID: " + pedido.optString("ID", ""));
        tId.setTextSize(16);
        tId.setTypeface(tId.getTypeface(), Typeface.BOLD);

        TextView tCliente = new TextView(this);
        tCliente.setText("Cliente: " + pedido.optString("NOMBRE_CLIENTE", ""));
        tCliente.setTextSize(16);

        TextView tEstado = new TextView(this);
        tEstado.setText("Estado: " + estado);
        tEstado.setTextSize(16);

        Button btn = new Button(this);
        btn.setText("Ver Detalles");
        btn.setOnClickListener(v -> {
            Intent intent = new Intent(GrupoRutaActivity.this, DetallePedidoActivity.class);
            intent.putExtra("ID", pedido.optString("ID", ""));
            intent.putExtra("SUCURSAL", pedido.optString("SUCURSAL", ""));
            intent.putExtra("NOMBRE_CLIENTE", pedido.optString("NOMBRE_CLIENTE", ""));
            intent.putExtra("ESTADO", pedido.optString("ESTADO", ""));
            intent.putExtra("FECHA_RECEPCION_FACTURA", pedido.optString("FECHA_RECEPCION_FACTURA", ""));
            intent.putExtra("FECHA_ENTREGA_CLIENTE", pedido.optString("FECHA_ENTREGA_CLIENTE", ""));
            intent.putExtra("CHOFER_ASIGNADO", pedido.optString("CHOFER_ASIGNADO", ""));
            intent.putExtra("VENDEDOR", pedido.optString("VENDEDOR", ""));
            intent.putExtra("FACTURA", pedido.optString("FACTURA", ""));
            intent.putExtra("DIRECCION", pedido.optString("DIRECCION", ""));
            intent.putExtra("FECHA_MIN_ENTREGA", pedido.optString("FECHA_MIN_ENTREGA", ""));
            intent.putExtra("FECHA_MAX_ENTREGA", pedido.optString("FECHA_MAX_ENTREGA", ""));
            intent.putExtra("MIN_VENTANA_HORARIA_1", pedido.optString("MIN_VENTANA_HORARIA_1", ""));
            intent.putExtra("MAX_VENTANA_HORARIA_1", pedido.optString("MAX_VENTANA_HORARIA_1", ""));
            intent.putExtra("TELEFONO", pedido.optString("TELEFONO", ""));
            intent.putExtra("CONTACTO", pedido.optString("CONTACTO", ""));
            intent.putExtra("COMENTARIOS", pedido.optString("COMENTARIOS", ""));
            intent.putExtra("Ruta", pedido.optString("Ruta", ""));
            intent.putExtra("Coord_Origen", pedido.optString("Coord_Origen", ""));
            intent.putExtra("Coord_Destino", pedido.optString("Coord_Destino", ""));
            // Pasar datos de grupo si están
            JSONObject g = pedido.optJSONObject("grupo");
            if (g != null) {
                intent.putExtra("GRUPO_ID", g.optInt("id", 0));
                intent.putExtra("GRUPO_NOMBRE", g.optString("nombre", ""));
                intent.putExtra("GRUPO_ORDEN", g.has("orden_entrega") && !g.isNull("orden_entrega") ? g.optInt("orden_entrega") : -1);
            }
            startActivity(intent);
        });

        card.addView(logo);
        card.addView(tId);
        card.addView(tCliente);
        card.addView(tEstado);
        card.addView(btn);
        container.addView(card);
    }
}

