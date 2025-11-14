package com.example.app_pedidos.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.app_pedidos.ApiConfig;
import com.example.app_pedidos.R;
import com.example.app_pedidos.ui.Pedido.DetallePedidoActivity;
import com.example.app_pedidos.ui.Login.LoginActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;

public class HomeFragment extends Fragment {

    private static final String URL = ApiConfig.BASE_URL + "/Pedidos_GA/App/Consultar.php";
    private final long interval = 5000;
    private Timer timer;
    private LinearLayout linearLayoutContainer;
    private JSONArray pedidosArray;
    private AlertDialog noVehiculoDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        linearLayoutContainer = root.findViewById(R.id.linearLayoutContainer);
        iniciarActualizacionPeriodica();
        return root;
    }

    private void iniciarActualizacionPeriodica() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { obtenerPedidosV2(); }
        }, 0, interval);
    }

    private void obtenerPedidosV2() {
        SharedPreferences sp = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        String username = sp.getString("username", "");
        String url = URL + "?username=" + encode(username) + "&v2=1";

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        boolean vehiculoAsignado = response.optBoolean("vehiculo_asignado", true);
                        if (!vehiculoAsignado) { mostrarBloqueoSinVehiculo(); return; }
                        if (noVehiculoDialog != null && noVehiculoDialog.isShowing()) noVehiculoDialog.dismiss();
                        JSONArray arr = response.optJSONArray("pedidos");
                        if (arr == null || arr.length() == 0) mostrarListaVacia(); else {
                            mostrarPedidos(arr);
                            intentarNotificarEnRuta(arr);
                        }
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Error al procesar la respuesta", e);
                        mostrarMensaje("Error al procesar datos");
                    }
                },
                error -> {
                    error.printStackTrace();
                    if (error.networkResponse != null) {
                        int code = error.networkResponse.statusCode;
                        if (code == 204 || code == 404) { mostrarListaVacia(); return; }
                    }
                    mostrarMensaje("Problema de conexión. Intenta de nuevo.");
                });

        Volley.newRequestQueue(requireContext()).add(req);
    }

    private String encode(String v) { try { return URLEncoder.encode(v, "UTF-8"); } catch (Exception e) { return v; } }

    private void mostrarListaVacia() {
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            linearLayoutContainer.removeAllViews();
            TextView empty = new TextView(requireContext());
            empty.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            empty.setText("No hay pedidos para mostrar.");
            empty.setTextSize(16);
            empty.setTextColor(Color.DKGRAY);
            empty.setPadding(24,24,24,24);
            linearLayoutContainer.addView(empty);
        });
    }

    private void solicitarPermisoNotificacionesSiNecesario() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 5001);
            }
        }
    }

    private void intentarNotificarEnRuta(JSONArray arr) {
        try { solicitarPermisoNotificacionesSiNecesario(); } catch (Exception ignore) {}
        try {
            SharedPreferences sp = requireActivity().getSharedPreferences("notif_prefs", Context.MODE_PRIVATE);
            String activeCsv = sp.getString("notif_active_ids", "");
            if (activeCsv.isEmpty()) activeCsv = sp.getString("en_ruta_ids", ""); // compatibilidad
            java.util.HashSet<String> active = new java.util.HashSet<>();
            if (!activeCsv.isEmpty()) for (String s : activeCsv.split(",")) if (!s.isEmpty()) active.add(s);

            java.util.HashSet<String> toKeep = new java.util.HashSet<>(active);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject p = arr.getJSONObject(i);
                String id = p.optString("ID", "");
                if (id.isEmpty()) continue;
                String estado = p.optString("ESTADO", "");
                String lastKey = "notif_last_estado_" + id;
                String lastEstado = sp.getString(lastKey, "");

                boolean isCompleted = "ENTREGADO".equalsIgnoreCase(estado) || "COMPLETADO".equalsIgnoreCase(estado) || "FINALIZADO".equalsIgnoreCase(estado);
                if (isCompleted) {
                    if (active.contains(id)) {
                        try { com.example.app_pedidos.util.NotificationHelper.cancelForPedido(requireContext(), Integer.parseInt(id)); } catch (Exception ignore) {}
                        toKeep.remove(id);
                    }
                    // limpiar estado memorizado
                    sp.edit().remove(lastKey).apply();
                    continue;
                }

                if ("EN RUTA".equalsIgnoreCase(estado)) {
                    if (!active.contains(id)) {
                        com.example.app_pedidos.util.NotificationHelper.notifyEnRuta(requireContext(), p);
                        toKeep.add(id);
                        sp.edit().putString(lastKey, estado).apply();
                    } else {
                        // Actualizar solo si cambió el estado
                        if (!estado.equalsIgnoreCase(lastEstado)) {
                            com.example.app_pedidos.util.NotificationHelper.notifyEstado(requireContext(), p);
                            sp.edit().putString(lastKey, estado).apply();
                        }
                    }
                } else {
                    if (active.contains(id)) {
                        // Actualizar solo si cambió el estado
                        if (!estado.equalsIgnoreCase(lastEstado)) {
                            com.example.app_pedidos.util.NotificationHelper.notifyEstado(requireContext(), p);
                            sp.edit().putString(lastKey, estado).apply();
                        }
                        toKeep.add(id);
                    }
                }
            }

            String nextCsv = String.join(",", toKeep);
            sp.edit().putString("notif_active_ids", nextCsv).apply();
        } catch (Exception e) { /* silencioso */ }
    }

    private void mostrarPedidos(JSONArray response) {
        pedidosArray = response;
        try {
            linearLayoutContainer.removeAllViews();
            for (int i = 0; i < response.length(); i++) {
                JSONObject pedido = response.getJSONObject(i);
                String estado = pedido.optString("ESTADO", "");
                if ("ACTIVO".equals(estado) || "EN RUTA".equals(estado) || "REPROGRAMADO".equals(estado) || "EN TIENDA".equals(estado)) {
                    agregarPedidoALayout(pedido);
                }
            }
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void agregarPedidoALayout(final JSONObject pedido) throws JSONException {
        CardView cardView = new CardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0,0,0,16);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(16);

        String estado = pedido.getString("ESTADO");
        if ("ACTIVO".equals(estado)) cardView.setCardBackgroundColor(Color.parseColor("#CCE5FF"));
        else if ("EN RUTA".equals(estado)) cardView.setCardBackgroundColor(Color.parseColor("#FFD699"));
        else if ("REPROGRAMADO".equals(estado)) cardView.setCardBackgroundColor(Color.parseColor("#E6CCFF"));
        else if ("EN TIENDA".equals(estado)) cardView.setCardBackgroundColor(Color.parseColor("#FFFFCC"));
        else cardView.setCardBackgroundColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) cardView.setElevation(8);

        LinearLayout linearLayout = new LinearLayout(requireContext());
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(16,16,16,16);

        TextView textOrderId = new TextView(requireContext());
        textOrderId.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        textOrderId.setText("ID: " + pedido.getString("ID"));
        textOrderId.setTextSize(16);

        ImageView imageOrderTitle = new ImageView(requireContext());
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(200,200);
        imageOrderTitle.setLayoutParams(imageParams);
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
        imageOrderTitle.setImageResource(imgRes);
        int filterColor;
        switch (estado) {
            case "ACTIVO": filterColor = Color.parseColor("#576977"); break;
            case "EN RUTA": filterColor = Color.parseColor("#7A5D3D"); break;
            case "REPROGRAMADO": filterColor = Color.parseColor("#715C5B"); break;
            case "EN TIENDA": filterColor = Color.parseColor("#78785E"); break;
            default: filterColor = Color.WHITE; break;
        }
        imageOrderTitle.setColorFilter(filterColor, PorterDuff.Mode.SRC_IN);

        // Grupo (si existe)
        TextView textGroup = null;
        try {
            JSONObject grupoObj = pedido.optJSONObject("grupo");
            if (grupoObj != null) {
                textGroup = new TextView(requireContext());
                textGroup.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                String nombreGrupo = grupoObj.optString("nombre", "");
                int orden = grupoObj.has("orden_entrega") && !grupoObj.isNull("orden_entrega") ? grupoObj.optInt("orden_entrega") : -1;
                int gid = grupoObj.optInt("id", 0);
                String label = "Grupo" + (gid > 0 ? " #" + gid : "") + ": " + nombreGrupo + (orden >= 0 ? " (orden " + orden + ")" : "");
                textGroup.setText(label);
                textGroup.setTextSize(14);
                textGroup.setTypeface(textGroup.getTypeface(), Typeface.BOLD);
                textGroup.setTextColor(Color.parseColor("#1565C0"));
            }
        } catch (Exception ignore) { }

        TextView textOrderDetails = new TextView(requireContext());
        textOrderDetails.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        textOrderDetails.setText("Cliente: " + pedido.optString("NOMBRE_CLIENTE", ""));
        textOrderDetails.setTextSize(16);

        TextView textOrderState = new TextView(requireContext());
        textOrderState.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        textOrderState.setText("Estado: " + estado);
        textOrderState.setTextSize(16);

        TextView textOrderDate = new TextView(requireContext());
        textOrderDate.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        textOrderDate.setText("Fecha Recepcion: " + pedido.optString("FECHA_RECEPCION_FACTURA", ""));
        textOrderDate.setTextSize(16);

        // Hacer que toda la tarjeta funcione como botón
        cardView.setOnClickListener(v -> {
            try {
                JSONObject pedidoSeleccionado = pedido; // usar el mismo objeto del cierre
                JSONObject g = pedidoSeleccionado.optJSONObject("grupo");
                if (g != null && g.optInt("id", 0) > 0) {
                    Intent gi = new Intent(requireContext(), com.example.app_pedidos.ui.Pedido.GrupoRutaActivity.class);
                    gi.putExtra("GRUPO_ID", g.optInt("id", 0));
                    gi.putExtra("GRUPO_NOMBRE", g.optString("nombre", ""));
                    startActivity(gi);
                } else if (pedidoSeleccionado.has("ID") && pedidoSeleccionado.has("SUCURSAL") && pedidoSeleccionado.has("NOMBRE_CLIENTE") && pedidoSeleccionado.has("ESTADO") && pedidoSeleccionado.has("FECHA_RECEPCION_FACTURA")) {
                    Intent intent = new Intent(requireContext(), DetallePedidoActivity.class);
                    intent.putExtra("ID", pedidoSeleccionado.getString("ID"));
                    intent.putExtra("SUCURSAL", pedidoSeleccionado.getString("SUCURSAL"));
                    intent.putExtra("NOMBRE_CLIENTE", pedidoSeleccionado.getString("NOMBRE_CLIENTE"));
                    intent.putExtra("ESTADO", pedidoSeleccionado.getString("ESTADO"));
                    intent.putExtra("FECHA_RECEPCION_FACTURA", pedidoSeleccionado.getString("FECHA_RECEPCION_FACTURA"));
                    intent.putExtra("FECHA_ENTREGA_CLIENTE", pedidoSeleccionado.optString("FECHA_ENTREGA_CLIENTE", ""));
                    intent.putExtra("CHOFER_ASIGNADO", pedidoSeleccionado.optString("CHOFER_ASIGNADO", ""));
                    intent.putExtra("VENDEDOR", pedidoSeleccionado.optString("VENDEDOR", ""));
                    intent.putExtra("FACTURA", pedidoSeleccionado.optString("FACTURA", ""));
                    intent.putExtra("DIRECCION", pedidoSeleccionado.optString("DIRECCION", ""));
                    intent.putExtra("FECHA_MIN_ENTREGA", pedidoSeleccionado.optString("FECHA_MIN_ENTREGA", ""));
                    intent.putExtra("FECHA_MAX_ENTREGA", pedidoSeleccionado.optString("FECHA_MAX_ENTREGA", ""));
                    intent.putExtra("MIN_VENTANA_HORARIA_1", pedidoSeleccionado.optString("MIN_VENTANA_HORARIA_1", ""));
                    intent.putExtra("MAX_VENTANA_HORARIA_1", pedidoSeleccionado.optString("MAX_VENTANA_HORARIA_1", ""));
                    intent.putExtra("TELEFONO", pedidoSeleccionado.optString("TELEFONO", ""));
                    intent.putExtra("CONTACTO", pedidoSeleccionado.optString("CONTACTO", ""));
                    intent.putExtra("COMENTARIOS", pedidoSeleccionado.optString("COMENTARIOS", ""));
                    intent.putExtra("Ruta", pedidoSeleccionado.optString("Ruta", ""));
                    intent.putExtra("Coord_Origen", pedidoSeleccionado.optString("Coord_Origen", ""));
                    intent.putExtra("Coord_Destino", pedidoSeleccionado.optString("Coord_Destino", ""));
                    startActivity(intent);
                }
            } catch (JSONException e) { e.printStackTrace(); }
        });

        linearLayout.addView(imageOrderTitle);
        linearLayout.addView(textOrderId);
        if (textGroup != null) { linearLayout.addView(textGroup); }
        linearLayout.addView(textOrderDetails);
        linearLayout.addView(textOrderState);
        linearLayout.addView(textOrderDate);
        cardView.addView(linearLayout);
        linearLayoutContainer.addView(cardView);
    }

    private void mostrarMensaje(String mensaje) {
        com.example.app_pedidos.ui.common.Notifier.error(requireActivity(), mensaje);
    }

    private void mostrarBloqueoSinVehiculo() {
        if (noVehiculoDialog != null && noVehiculoDialog.isShowing()) return;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_Pedidos_MaterialAlertDialog);
        builder.setTitle("Vehiculo no asignado");
        builder.setMessage("Solicita a tu Jefe de choferes de Sucursal que te asigne un vehiculo para continuar");
        builder.setCancelable(false);
        builder.setPositiveButton("Cerrar sesion", (dialog, which) -> cerrarSesionDesdeHome());
        noVehiculoDialog = builder.create();
        noVehiculoDialog.setCanceledOnTouchOutside(false);
        noVehiculoDialog.show();
    }

    private void cerrarSesionDesdeHome() {
        SharedPreferences sp = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove("username");
        editor.apply();
        com.example.app_pedidos.ui.common.Notifier.info(requireActivity(), "Sesi��n cerrada exitosamente");
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timer != null) { timer.cancel(); timer = null; }
        if (noVehiculoDialog != null && noVehiculoDialog.isShowing()) noVehiculoDialog.dismiss();
    }
}
