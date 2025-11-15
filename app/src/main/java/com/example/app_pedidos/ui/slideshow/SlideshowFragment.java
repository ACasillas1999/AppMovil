package com.example.app_pedidos.ui.slideshow;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.app_pedidos.R;
import com.example.app_pedidos.ApiConfig;
import com.example.app_pedidos.ui.Login.LoginActivity;
import com.example.app_pedidos.ui.Pedido.DetallePedidoActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class SlideshowFragment extends Fragment {

    // private static final String URL = "https://pedidos.grupoascencio.com.mx/Pedidos_GA/App/Consultar.php";
    private static final String URL = ApiConfig.BASE_URL + "/Pedidos_GA/App/Consultar.php";
    private final long interval = 5000; // 5 segundos
    private Timer timer;
    private LinearLayout linearLayoutContainer;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeHome;
    private JSONArray pedidosArray;
    private AlertDialog noVehiculoDialog;
    private final BroadcastReceiver pedidoEstadoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            obtenerPedidosV2();
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        linearLayoutContainer = root.findViewById(R.id.linearLayoutContainer);
        swipeHome = root.findViewById(R.id.swipeHome);
        if (swipeHome != null) {
            swipeHome.setOnRefreshListener(this::obtenerPedidosV2);
            swipeHome.setColorSchemeResources(
                    android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light
            );
        }

        // Iniciar la actualización automática al crear la vista
        iniciarActualizacionPeriodica();
        return root;
    }

    private void iniciarActualizacionPeriodica() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                obtenerPedidosV2();
            }
        }, 0, interval);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(com.example.app_pedidos.util.Events.ACTION_PEDIDO_ESTADO_ACTUALIZADO);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(pedidoEstadoReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(pedidoEstadoReceiver, filter);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        try { requireContext().unregisterReceiver(pedidoEstadoReceiver); } catch (Exception ignore) {}
    }

    private void obtenerPedidosV2() {
        if (getActivity() == null) {
            return;
        }

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");

        String urlWithParams = URL + "?username=" + username + "&v2=1";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                urlWithParams,
                null,
                response -> {
                    if (!isAdded()) return;
                    try {
                        boolean vehiculoAsignado = response.optBoolean("vehiculo_asignado", true);
                        if (!vehiculoAsignado) {
                            mostrarBloqueoSinVehiculo();
                            return;
                        } else if (noVehiculoDialog != null && noVehiculoDialog.isShowing()) {
                            noVehiculoDialog.dismiss();
                        }

                        JSONArray arr = response.optJSONArray("pedidos");
                        if (arr != null && arr.length() > 0) {
                            mostrarPedidos(arr);
                        } else {
                            mostrarListaVacia();
                        }
                    } catch (Exception e) {
                        Log.e("SlideshowFragment", "Error al procesar la respuesta", e);
                        mostrarMensaje("Error al procesar datos");
                    }
                    try { if (swipeHome != null) swipeHome.setRefreshing(false); } catch (Exception ignore) {}
                },
                error -> {
                    error.printStackTrace();
                    Log.e("SlideshowFragment", "Error en la solicitud HTTP: " + error);
                    if (isAdded()) {
                        mostrarMensaje("Error en la solicitud HTTP");
                    }
                    try { if (swipeHome != null) swipeHome.setRefreshing(false); } catch (Exception ignore) {}
                }
        );

        if (getContext() != null) {
            Volley.newRequestQueue(getContext()).add(jsonObjectRequest);
        }
    }

    private void mostrarListaVacia() {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (linearLayoutContainer == null) return;
            linearLayoutContainer.removeAllViews();
            TextView empty = new TextView(requireContext());
            empty.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            empty.setText("No hay pedidos para mostrar.");
            empty.setTextSize(16);
            empty.setTextColor(Color.DKGRAY);
            empty.setPadding(24,24,24,24);
            linearLayoutContainer.addView(empty);
            try { if (swipeHome != null) swipeHome.setRefreshing(false); } catch (Exception ignore) {}
        });
    }

    private void obtenerPedidos() {
        if (getActivity() == null) {
            return;
        }

        // Obtener el nombre de usuario de SharedPreferences
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");

        // Agregar el nombre de usuario a la URL como parámetro de consulta
        String urlWithParams = URL + "?username=" + username;

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                urlWithParams,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if (isAdded()) {
                            mostrarPedidos(response);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Log.e("SlideshowFragment", "Error en la solicitud HTTP: " + error.toString());
                        if (isAdded()) {
                            mostrarMensaje("Error en la solicitud HTTP");
                        }
                    }
                }
        );

        if (getContext() != null) {
            Volley.newRequestQueue(getContext()).add(jsonArrayRequest);
        }
    }

    private void mostrarPedidos(JSONArray response) {
        pedidosArray = response; // Almacenar el JSONArray globalmente

        try {
            linearLayoutContainer.removeAllViews();

            int agregados = 0;
            for (int i = 0; i < response.length(); i++) {
                final JSONObject pedido = response.getJSONObject(i);
                String estado = pedido.getString("ESTADO");
                // Mostrar solo los pedidos entregados o cancelados
                if (estado.equals("ENTREGADO") || estado.equals("CANCELADO")) {
                    agregarPedidoALayout(pedido);
                    agregados++;
                }
            }
            if (agregados == 0) {
                mostrarListaVacia();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void agregarPedidoALayout(final JSONObject pedido) throws JSONException {
        if (!isAdded() || getContext() == null) {
            return;
        }

        // Crear un CardView para cada pedido
        CardView cardView = new CardView(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16); // Agregar margen inferior entre los pedidos
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(16); // Establecer el radio de las esquinas del CardView

        // Obtener el estado del pedido
        String estado = pedido.getString("ESTADO");

        // Establecer el color de fondo del CardView según el estado
        if (estado.equals("ENTREGADO")) {
            cardView.setCardBackgroundColor(Color.parseColor("#C8E6C9")); // Verde claro
        } else if (estado.equals("CANCELADO")) {
            cardView.setCardBackgroundColor(Color.parseColor("#FFCDD2")); // Rojo claro
        } else {
            cardView.setCardBackgroundColor(Color.WHITE); // Fondo blanco por defecto
        }

        // Establecer la elevación para que los pedidos tengan sombra
        // Alinear colores de fondo con Home (sobrescribe si aplica)
        if ("ACTIVO".equals(estado)) { cardView.setCardBackgroundColor(Color.parseColor("#CCE5FF")); }
        else if ("EN RUTA".equals(estado)) { cardView.setCardBackgroundColor(Color.parseColor("#FFD699")); }
        else if ("REPROGRAMADO".equals(estado)) { cardView.setCardBackgroundColor(Color.parseColor("#E6CCFF")); }
        else if ("EN TIENDA".equals(estado)) { cardView.setCardBackgroundColor(Color.parseColor("#FFFFCC")); }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cardView.setElevation(8);
        }

        // Crear un LinearLayout para contener la información del pedido
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(16, 16, 16, 16);

        // TextView para mostrar el ID del pedido
        TextView textOrderId = new TextView(getContext());
        textOrderId.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        textOrderId.setText("ID: " + pedido.getString("ID"));
        textOrderId.setTextSize(16);

        // TextView para mostrar la sucursal del pedido
        TextView textOrderTitle = new TextView(getContext());
        textOrderTitle.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Crear y configurar imageOrderTitle
        ImageView imageOrderTitle = new ImageView(requireContext());
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                200, // Ancho en píxeles
                200  // Alto en píxeles

        );
        imageParams.setMargins(0,0,0,0);
        imageOrderTitle.setLayoutParams(imageParams);

// Obtener el valor del texto de textOrderTitle
        String sucursal = pedido.getString("SUCURSAL");

// Comparar el valor de sucursal y establecer la imagen
        switch (sucursal) {
            case "DEASA":
                imageOrderTitle.setImageResource(R.drawable.deasaazz);
                break;
            case "DIMEGSA":
                imageOrderTitle.setImageResource(R.drawable.dimegsa);
                break;
            case "AIESA":
                imageOrderTitle.setImageResource(R.drawable.aiesa);
                break;
            case "SEGSA":
                imageOrderTitle.setImageResource(R.drawable.segsa);
                break;
            case "FESA":
                imageOrderTitle.setImageResource(R.drawable.fesa);
                break;
            case "TAPATIA":
                imageOrderTitle.setImageResource(R.drawable.eitsa);
                break;
            case "GABSA":
                imageOrderTitle.setImageResource(R.drawable.gabl);
                break;
            case "ILUMINACION":
                imageOrderTitle.setImageResource(R.drawable.ilum);
                break;
            case "VALLARTA":
                imageOrderTitle.setImageResource(R.drawable.gabl);
                break;
            default:
                imageOrderTitle.setImageResource(R.drawable.gabl);
                break;
        }

        // Aplicar el filtro de color según el estado
        int filterColor;
        switch (estado) {
            case "ENTREGADO":
                filterColor = Color.parseColor("#5A705B"); // Verde
                break;
            case "CANCELADO":
                filterColor = Color.parseColor("#6B5B76"); // Rojo
                break;

            default:
                filterColor = Color.WHITE; // Fondo blanco por defecto
                break;
        }

        imageOrderTitle.setColorFilter(filterColor, PorterDuff.Mode.SRC_IN);

        // Grupo (si existe)
        TextView textGroup = null;
        try {
            JSONObject grupoObj = pedido.optJSONObject("grupo");
            if (grupoObj != null) {
                textGroup = new TextView(getContext());
                textGroup.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                String nombreGrupo = grupoObj.optString("nombre", "");
                int orden = (grupoObj.has("orden_entrega") && !grupoObj.isNull("orden_entrega")) ? grupoObj.optInt("orden_entrega") : -1;
                int gid = grupoObj.optInt("id", 0);
                String label = "Grupo" + (gid > 0 ? " #" + gid : "") + ": " + nombreGrupo + (orden >= 0 ? " (orden " + orden + ")" : "");
                textGroup.setText(label);
                textGroup.setTextSize(14);
                textGroup.setTypeface(null, Typeface.BOLD);
                textGroup.setTextColor(Color.parseColor("#1565C0"));
            }
        } catch (Exception ignore) { }

        // Configurar textOrderTitle
          textOrderTitle.setText("Sucursal: " + pedido.getString("SUCURSAL"));
        //  textOrderTitle.setText("Sucursal:"+ imageParams);
        textOrderTitle.setTextSize(18);
        textOrderTitle.setTypeface(null, Typeface.BOLD);


        /*
        textOrderTitle.setText("Sucursal: " + pedido.getString("SUCURSAL"));
        textOrderTitle.setTextSize(18);
        textOrderTitle.setTypeface(null, Typeface.BOLD);*/

        // TextView para mostrar el nombre del cliente
        TextView textOrderDetails = new TextView(getContext());
        textOrderDetails.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        textOrderDetails.setText("Cliente: " + pedido.getString("NOMBRE_CLIENTE"));
        textOrderDetails.setTextSize(16);

        // TextView para mostrar el estado del pedido
        TextView textOrderState = new TextView(getContext());
        textOrderState.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        textOrderState.setText("Estado: " + pedido.getString("ESTADO"));
        textOrderState.setTextSize(16);

        // TextView para mostrar la fecha de recepción del pedido
        TextView textOrderDate = new TextView(getContext());
        textOrderDate.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        textOrderDate.setText("Fecha Recepción: " + pedido.getString("FECHA_RECEPCION_FACTURA"));
        textOrderDate.setTextSize(16);

        // Hacer clickable toda la tarjeta
        cardView.setOnClickListener(v -> {
            if (!isAdded()) return;
            try {
                JSONObject g = pedido.optJSONObject("grupo");
                if (g != null && g.optInt("id", 0) > 0) {
                    Intent gi = new Intent(getContext(), com.example.app_pedidos.ui.Pedido.GrupoRutaActivity.class);
                    gi.putExtra("GRUPO_ID", g.optInt("id", 0));
                    gi.putExtra("GRUPO_NOMBRE", g.optString("nombre", ""));
                    startActivity(gi);
                    return;
                }
                Intent intent = new Intent(getContext(), DetallePedidoActivity.class);
                intent.putExtra("ID", pedido.getString("ID"));
                intent.putExtra("SUCURSAL", pedido.getString("SUCURSAL"));
                intent.putExtra("NOMBRE_CLIENTE", pedido.getString("NOMBRE_CLIENTE"));
                intent.putExtra("ESTADO", pedido.getString("ESTADO"));
                intent.putExtra("FECHA_RECEPCION_FACTURA", pedido.getString("FECHA_RECEPCION_FACTURA"));
                intent.putExtra("FECHA_ENTREGA_CLIENTE", pedido.optString("FECHA_ENTREGA_CLIENTE", ""));
                intent.putExtra("CHOFER_ASIGNADO", pedido.optString("CHOFER_ASIGNADO", ""));
                intent.putExtra("VENDEDOR", pedido.optString("VENDEDOR", ""));
                intent.putExtra("FACTURA", pedido.getString("FACTURA"));
                intent.putExtra("DIRECCION", pedido.getString("DIRECCION"));
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
                startActivity(intent);
            } catch (JSONException e) {
                e.printStackTrace();
                if (isAdded()) mostrarMensaje("Error al procesar JSON");
            }
        });


        // Agregar vistas al contenedor de la tarjeta (alineado con Home)
        linearLayout.addView(imageOrderTitle);
        linearLayout.addView(textOrderId);
        if (textGroup != null) { linearLayout.addView(textGroup); }
        linearLayout.addView(textOrderDetails);
        linearLayout.addView(textOrderState);
        linearLayout.addView(textOrderDate);

        // Agregar el LinearLayout al CardView
        cardView.addView(linearLayout);

        // Agregar el CardView al contenedor principal
        linearLayoutContainer.addView(cardView);
    }

    private void mostrarMensaje(String mensaje) {
        com.example.app_pedidos.ui.common.Notifier.error(requireActivity(), mensaje);
    }

    private void mostrarBloqueoSinVehiculo() {
        if (noVehiculoDialog != null && noVehiculoDialog.isShowing()) {
            return;
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_Pedidos_MaterialAlertDialog);
        builder.setTitle("Vehículo no asignado");
        builder.setMessage("Solicita a tu Jefe de choferes de Sucursal que te asigne un vehículo para continuar");
        builder.setCancelable(false);
        builder.setPositiveButton("Cerrar sesión", (dialog, which) -> cerrarSesionDesdeFragment());
        noVehiculoDialog = builder.create();
        noVehiculoDialog.setCanceledOnTouchOutside(false);
        noVehiculoDialog.show();
    }

    private void cerrarSesionDesdeFragment() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("username");
        editor.apply();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (noVehiculoDialog != null && noVehiculoDialog.isShowing()) {
            noVehiculoDialog.dismiss();
        }
    }
}

