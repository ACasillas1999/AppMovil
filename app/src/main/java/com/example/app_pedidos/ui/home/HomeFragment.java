package com.example.app_pedidos.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.app_pedidos.ApiConfig;
import com.example.app_pedidos.R;
import com.example.app_pedidos.ui.Login.LoginActivity;
import com.example.app_pedidos.ui.Pedido.DetallePedidoActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String URL = ApiConfig.BASE_URL + "/Pedidos_GA/App/Consultar.php";
    private final long interval = 5000; // 5 segundos
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    private RecyclerView recyclerPedidos;
    private TextView emptyView;
    private PedidosAdapter adapter;
    private JSONArray pedidosArray;
    private AlertDialog noVehiculoDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        recyclerPedidos = root.findViewById(R.id.recyclerPedidos);
        emptyView = root.findViewById(R.id.emptyView);

        adapter = new PedidosAdapter(position -> {
            try {
                if (pedidosArray == null) return;
                JSONObject pedidoSeleccionado = pedidosArray.getJSONObject(position);
                if (pedidoSeleccionado.has("ID") && pedidoSeleccionado.has("SUCURSAL")
                        && pedidoSeleccionado.has("NOMBRE_CLIENTE") && pedidoSeleccionado.has("ESTADO")
                        && pedidoSeleccionado.has("FECHA_RECEPCION_FACTURA")) {
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
            } catch (JSONException e) {
                Log.e("HomeFragment", "Error al procesar JSON: " + e.getMessage());
            }
        });

        recyclerPedidos.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerPedidos.setAdapter(adapter);

        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                obtenerPedidosV2();
                refreshHandler.postDelayed(this, interval);
            }
        };

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHandler.removeCallbacksAndMessages(null);
        refreshHandler.post(refreshRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacksAndMessages(null);
    }

    private void obtenerPedidosV2() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");

        String urlWithParams = URL + "?username=" + encode(username) + "&v2=1";

        JsonObjectRequest jsonObjectRequest = new com.example.app_pedidos.network.Utf8JsonObjectRequest(
                Request.Method.GET,
                urlWithParams,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            boolean vehiculoAsignado = response.optBoolean("vehiculo_asignado", true);
                            if (!vehiculoAsignado) {
                                mostrarBloqueoSinVehiculo();
                                return;
                            } else {
                                if (noVehiculoDialog != null && noVehiculoDialog.isShowing()) {
                                    noVehiculoDialog.dismiss();
                                }
                            }

                            JSONArray arr = response.optJSONArray("pedidos");
                            if (arr == null || arr.length() == 0) {
                                mostrarListaVacia();
                            } else {
                                mostrarPedidos(arr);
                            }
                        } catch (Exception e) {
                            Log.e("HomeFragment", "Error al procesar la respuesta", e);
                            mostrarMensaje("Error al procesar datos");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Log.e("HomeFragment", "Error en la solicitud HTTP: " + error.toString());
                        if (error.networkResponse != null) {
                            int code = error.networkResponse.statusCode;
                            if (code == 204 || code == 404) {
                                mostrarListaVacia();
                                return;
                            }
                        }
                        mostrarMensaje("Problema de conexión. Intenta de nuevo.");
                    }
                }
        );

        Volley.newRequestQueue(requireContext()).add(jsonObjectRequest);
    }

    private void obtenerPedidos() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");

        String urlWithParams = URL + "?username=" + encode(username);

        JsonArrayRequest jsonArrayRequest = new com.example.app_pedidos.network.Utf8JsonArrayRequest(
                Request.Method.GET,
                urlWithParams,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if (response == null || response.length() == 0) {
                            mostrarListaVacia();
                        } else {
                            mostrarPedidos(response);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Log.e("HomeFragment", "Error en la solicitud HTTP: " + error.toString());
                        if (error.networkResponse != null) {
                            int code = error.networkResponse.statusCode;
                            if (code == 204 || code == 404) {
                                mostrarListaVacia();
                                return;
                            }
                        }
                        mostrarMensaje("Problema de conexión. Intenta de nuevo.");
                    }
                }
        );

        Volley.newRequestQueue(requireContext()).add(jsonArrayRequest);
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private void mostrarListaVacia() {
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            emptyView.setVisibility(View.VISIBLE);
            recyclerPedidos.setVisibility(View.GONE);
            if (adapter != null) adapter.submitList(java.util.Collections.emptyList());
        });
    }

    private void mostrarPedidos(JSONArray response) {
        pedidosArray = response; // Guardar JSON para detalles
        List<Pedido> list = new ArrayList<>();
        try {
            for (int i = 0; i < response.length(); i++) {
                final JSONObject pedido = response.getJSONObject(i);
                String estado = pedido.optString("ESTADO", "");
                if ("ACTIVO".equals(estado) || "EN RUTA".equals(estado) || "REPROGRAMADO".equals(estado) || "EN TIENDA".equals(estado)) {
                    list.add(new Pedido(
                            pedido.optString("ID", ""),
                            pedido.optString("SUCURSAL", ""),
                            pedido.optString("NOMBRE_CLIENTE", ""),
                            estado,
                            pedido.optString("FECHA_RECEPCION_FACTURA", "")
                    ));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final List<Pedido> finalList = list;
        requireActivity().runOnUiThread(() -> {
            if (finalList.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recyclerPedidos.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerPedidos.setVisibility(View.VISIBLE);
            }
            adapter.submitList(finalList);
        });
    }

    private void mostrarMensaje(String mensaje) {
        com.example.app_pedidos.ui.common.Notifier.error(requireActivity(), mensaje);
    }

    private void mostrarBloqueoSinVehiculo() {
        if (noVehiculoDialog != null && noVehiculoDialog.isShowing()) {
            return;
        }
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
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("username");
        editor.apply();

        com.example.app_pedidos.ui.common.Notifier.info(requireActivity(), "Sesión cerrada exitosamente");
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        refreshHandler.removeCallbacksAndMessages(null);
        if (noVehiculoDialog != null && noVehiculoDialog.isShowing()) {
            noVehiculoDialog.dismiss();
        }
    }
}

