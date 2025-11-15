package com.example.app_pedidos.ui.Pedido;

import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    private String grupoNombreSimple = "";
    private ProgressBar progressGroup;
    private TextView emptyGroup;
    private SwipeRefreshLayout swipeGroup;
    private final BroadcastReceiver pedidoEstadoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadGroupData();
        }
    };

    private void loadGroupData() {
        // Vuelve a consultar el grupo y reconstruye la lista
        if (container == null) return;
        container.removeAllViews();
        if (emptyGroup != null) emptyGroup.setVisibility(View.GONE);
        boolean isRefreshing = (swipeGroup != null && swipeGroup.isRefreshing());
        if (!isRefreshing && progressGroup != null) progressGroup.setVisibility(View.VISIBLE);
        String url = ApiConfig.BASE_URL + "/Pedidos_GA/App/Consultar.php?grupo_id=" + grupoId + "&limit=1000";
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        org.json.JSONObject g = response.optJSONObject("grupo");
                        if (g != null && headerMeta != null) {
                            String meta = (g.optString("sucursal", "") + " • " + g.optString("chofer_asignado", "") + " • " + g.optString("estado", "")).trim();
                            headerMeta.setText(meta);
                        }
                        org.json.JSONArray arr = response.optJSONArray("pedidos");
                        if (arr != null) {
                            java.util.ArrayList<org.json.JSONObject> lista = new java.util.ArrayList<>();
                            for (int i = 0; i < arr.length(); i++) lista.add(arr.getJSONObject(i));
                            java.util.Collections.sort(lista, new java.util.Comparator<org.json.JSONObject>() {
                                @Override public int compare(org.json.JSONObject a, org.json.JSONObject b) {
                                    int oa = extraerOrden(a);
                                    int ob = extraerOrden(b);
                                    if (oa != ob) return Integer.compare(oa, ob);
                                    long ida = safeLong(a.optString("ID", "0"));
                                    long idb = safeLong(b.optString("ID", "0"));
                                    return Long.compare(ida, idb);
                                }
                            });
                            if (lista.isEmpty()) {
                                if (emptyGroup != null) emptyGroup.setVisibility(View.VISIBLE);
                            } else {
                                java.util.HashSet<String> seen = new java.util.HashSet<>();
                                for (org.json.JSONObject p : lista) {
                                    String id = p.optString("ID", "");
                                    if (!id.isEmpty() && !seen.contains(id)) {
                                        seen.add(id);
                                        addPedidoCard(p);
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) { }
                    if (swipeGroup != null) swipeGroup.setRefreshing(false);
                    if (progressGroup != null) progressGroup.setVisibility(View.GONE);
                },
                error -> {
                    if (swipeGroup != null) swipeGroup.setRefreshing(false);
                    if (progressGroup != null) progressGroup.setVisibility(View.GONE);
                    if (emptyGroup != null) emptyGroup.setVisibility(View.VISIBLE);
                });
        Volley.newRequestQueue(this).add(req);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Registrar receptor y refrescar al volver desde detalle
        IntentFilter filter = new IntentFilter(com.example.app_pedidos.util.Events.ACTION_PEDIDO_ESTADO_ACTUALIZADO);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pedidoEstadoReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(pedidoEstadoReceiver, filter);
        }
        loadGroupData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try { unregisterReceiver(pedidoEstadoReceiver); } catch (Exception ignore) {}
    }
    

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grupo_ruta);

        container = findViewById(R.id.linearLayoutContainerGroup);
        headerNombre = findViewById(R.id.textGrupoNombreHeader);
        headerMeta = findViewById(R.id.textGrupoMetaHeader);
        progressGroup = findViewById(R.id.progressGroup);
        emptyGroup = findViewById(R.id.textEmptyGroup);
        swipeGroup = findViewById(R.id.swipeGroup);
        if (swipeGroup != null) {
            swipeGroup.setOnRefreshListener(() -> {
                loadGroupData();
            });
            swipeGroup.setColorSchemeResources(
                    android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light
            );
        }

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
        this.grupoNombreSimple = grupoNombre; // guardar nombre simple para tarjetas
        headerNombre.setText(titulo);

        // Carga inicial se realiza en onStart() mediante loadGroupData() para evitar duplicados

        // Botón para abrir mapa web del grupo
        Button btnMap = findViewById(R.id.btnVerMapaGrupo);
        if (btnMap != null) {
            btnMap.setOnClickListener(v -> {
                GrupoMapaDialogFragment.newWithGroupId(grupoId)
                        .show(getSupportFragmentManager(), "grupo_mapa");
            });
        }

        // Si viene desde una notificación y se solicita abrir el mapa directamente
        boolean openMap = getIntent().getBooleanExtra("OPEN_MAP", false);
        if (openMap && grupoId > 0) {
            btnMap.post(() -> {
                try {
                    GrupoMapaDialogFragment.newWithGroupId(grupoId)
                            .show(getSupportFragmentManager(), "grupo_mapa_from_notif");
                } catch (Exception ignored) {}
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
        // Mismos colores que Home/Historial (otros estados en blanco)
        int bgColor;
        String estadoU = estado == null ? "" : estado.toUpperCase(java.util.Locale.ROOT);
        switch (estadoU) {
            case "ACTIVO": bgColor = Color.parseColor("#CCE5FF"); break;
            case "EN RUTA": bgColor = Color.parseColor("#FFD699"); break;
            case "REPROGRAMADO": bgColor = Color.parseColor("#E6CCFF"); break;
            case "EN TIENDA": bgColor = Color.parseColor("#FFFFCC"); break;
            case "ENTREGADO": bgColor = Color.parseColor("#C8E6C9"); break; // Verde claro
            case "CANCELADO": bgColor = Color.parseColor("#FFCDD2"); break; // Rojo claro
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

        // Texto de grupo igual a Home: "Grupo #id: nombre (orden N)"
        int orden = extraerOrden(pedido);
        TextView textGroup = null;
        try {
            JSONObject grupoObj = pedido.optJSONObject("grupo");
            if (grupoObj != null || grupoId > 0) {
                textGroup = new TextView(this);
                String nombreGrupo = (grupoObj != null) ? grupoObj.optString("nombre", "") : grupoNombreSimple;
                int gid = (grupoObj != null) ? grupoObj.optInt("id", grupoId) : grupoId;
                String label = "Grupo" + (gid > 0 ? " #" + gid : "") + ": " + nombreGrupo + (orden != Integer.MAX_VALUE ? " (orden " + orden + ")" : "");
                textGroup.setText(label);
                textGroup.setTextSize(14);
                textGroup.setTypeface(textGroup.getTypeface(), Typeface.BOLD);
                textGroup.setTextColor(Color.parseColor("#1565C0"));
            }
        } catch (Exception ignore) {}

        TextView tCliente = new TextView(this);
        tCliente.setText("Cliente: " + pedido.optString("NOMBRE_CLIENTE", ""));
        tCliente.setTextSize(16);

        TextView tEstado = new TextView(this);
        tEstado.setText("Estado: " + estado);
        tEstado.setTextSize(16);
        TextView tFechaRecep = new TextView(this);
        tFechaRecep.setText("Fecha Recepcion: " + pedido.optString("FECHA_RECEPCION_FACTURA", ""));
        tFechaRecep.setTextSize(16);

        // Hacer toda la tarjeta clicable como en Home/Historial
        cardView.setClickable(true);
        cardView.setOnClickListener(v -> {
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
        // Igual que Home: ID y luego línea de Grupo (si existe)
        card.addView(tId);
        if (textGroup != null) card.addView(textGroup);
        card.addView(tCliente);
        card.addView(tEstado);
        card.addView(tFechaRecep);
        cardView.addView(card);
        container.addView(cardView);
    }
}
