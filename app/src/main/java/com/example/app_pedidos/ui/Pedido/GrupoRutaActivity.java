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
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.app_pedidos.ApiConfig;
import com.example.app_pedidos.R;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import android.net.Uri;
import android.widget.Button;

public class GrupoRutaActivity extends AppCompatActivity {

    private LinearLayout container;
    private TextView headerNombre;
    private TextView headerMeta;
    private int grupoId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grupo_ruta);

        container = findViewById(R.id.linearLayoutContainerGroup);
        headerNombre = findViewById(R.id.textGrupoNombreHeader);
        headerMeta = findViewById(R.id.textGrupoMetaHeader);

        // Back button in toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_Pedidos);
        if (toolbar != null) {
            View back = toolbar.findViewById(R.id.VOlverBton);
            if (back != null) {
                back.setOnClickListener(v -> finish());
            }
        }

        grupoId = getIntent().getIntExtra("GRUPO_ID", 0);
        String grupoNombre = getIntent().getStringExtra("GRUPO_NOMBRE");
        if (grupoNombre == null) grupoNombre = "";
        String titulo = (grupoId > 0 ? ("Grupo #" + grupoId + ": ") : "Grupo: ") + (grupoNombre.isEmpty() ? "" : grupoNombre);
        headerNombre.setText(titulo);

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
                            // Ordenar por orden_entrega asc y luego por ID asc
                            ArrayList<JSONObject> lista = new ArrayList<>();
                            for (int i = 0; i < arr.length(); i++) lista.add(arr.getJSONObject(i));
                            Collections.sort(lista, new Comparator<JSONObject>() {
                                @Override public int compare(JSONObject a, JSONObject b) {
                                    int oa = extraerOrden(a);
                                    int ob = extraerOrden(b);
                                    if (oa != ob) return Integer.compare(oa, ob);
                                    long ida = safeLong(a.optString("ID", "0"));
                                    long idb = safeLong(b.optString("ID", "0"));
                                    return Long.compare(ida, idb);
                                }
                            });
                            for (JSONObject p : lista) addPedidoCard(p);
                        }
                    } catch (Exception ignored) { }
                },
                error -> { /* ignore simple errors visually */ });
        Volley.newRequestQueue(this).add(req);

        // Botón para abrir mapa web del grupo
        Button btnMap = findViewById(R.id.btnVerMapaGrupo);
        if (btnMap != null) {
            btnMap.setOnClickListener(v -> {
                GrupoMapaDialogFragment.newWithGroupId(grupoId)
                        .show(getSupportFragmentManager(), "grupo_mapa");
            });
        }
    }

    private long safeLong(String s) { try { return Long.parseLong(s); } catch (Exception e) { return 0L; } }
    private int extraerOrden(JSONObject o) {
        try {
            if (o.has("orden_entrega") && !o.isNull("orden_entrega")) return o.optInt("orden_entrega");
            JSONObject g = o.optJSONObject("grupo");
            if (g != null && g.has("orden_entrega") && !g.isNull("orden_entrega")) return g.optInt("orden_entrega");
        } catch (Exception ignored) { }
        return Integer.MAX_VALUE; // sin orden al final
    }

    private void addPedidoCard(final JSONObject pedido) {
        // Contenedor estilo CardView como en las otras vistas
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0,0,0,16);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(16);
        cardView.setCardElevation(8);

        String estado = pedido.optString("ESTADO", "");
        // Mismos colores que Home/Historial
        int bgColor;
        switch (estado) {
            case "ACTIVO": bgColor = Color.parseColor("#CCE5FF"); break;
            case "EN RUTA": bgColor = Color.parseColor("#FFD699"); break;
            case "REPROGRAMADO": bgColor = Color.parseColor("#E6CCFF"); break;
            case "EN TIENDA": bgColor = Color.parseColor("#FFFFCC"); break;
            case "ENTREGADO": bgColor = Color.parseColor("#C8E6C9"); break;
            case "CANCELADO": bgColor = Color.parseColor("#FFCDD2"); break;
            default: bgColor = Color.WHITE; break;
        }
        cardView.setCardBackgroundColor(bgColor);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16,16,16,16);
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

        // Mostrar Orden dentro del grupo si existe
        int orden = extraerOrden(pedido);
        TextView tOrden = null;
        if (orden != Integer.MAX_VALUE) {
            tOrden = new TextView(this);
            tOrden.setText("Orden: " + orden);
            tOrden.setTextSize(14);
            tOrden.setTextColor(Color.parseColor("#1565C0"));
            tOrden.setTypeface(tOrden.getTypeface(), Typeface.BOLD);
        }

        // Chip con ID del grupo
        TextView chipGrupo = new TextView(this);
        chipGrupo.setText("Grupo #" + Math.max(grupoId, 0));
        chipGrupo.setTextSize(12);
        chipGrupo.setTextColor(Color.WHITE);
        chipGrupo.setPadding(20, 8, 20, 8);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1565C0"));
        bg.setCornerRadius(24);
        chipGrupo.setBackground(bg);

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
        // Fila horizontal con ID y chip del grupo
        LinearLayout rowTop = new LinearLayout(this);
        rowTop.setOrientation(LinearLayout.HORIZONTAL);
        rowTop.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        tId.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        chipParams.setMargins(12, 0, 0, 0);
        chipGrupo.setLayoutParams(chipParams);
        rowTop.addView(tId);
        rowTop.addView(chipGrupo);
        card.addView(rowTop);
        if (tOrden != null) card.addView(tOrden);
        card.addView(tCliente);
        card.addView(tEstado);
        card.addView(btn);
        cardView.addView(card);
        container.addView(cardView);
    }
}
