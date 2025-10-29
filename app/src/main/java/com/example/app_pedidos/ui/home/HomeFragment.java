package com.example.app_pedidos.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
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

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.app_pedidos.MainActivity;
import com.example.app_pedidos.R;
import com.example.app_pedidos.ui.Pedido.DetallePedidoActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;
import java.net.URLEncoder;

public class HomeFragment extends Fragment {

   // private static final String URL = "https://pedidos.grupoascencio.com.mx/Pedidos_GA/App/Consultar.php";
   private static final String URL = "http://192.168.60.194/Pedidos_GA/App/Consultar.php";
    private final long interval = 5000; // 5 segundos
    private Timer timer;
    private LinearLayout linearLayoutContainer;
    private JSONArray pedidosArray;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        linearLayoutContainer = root.findViewById(R.id.linearLayoutContainer);



        // Iniciar la actualización automática al crear la vista
        iniciarActualizacionPeriodica();

        return root;
    }



    private void iniciarActualizacionPeriodica() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                obtenerPedidos();
            }
        }, 0, interval);
    }

    private void obtenerPedidos() {
        // Obtener el nombre de usuario de SharedPreferences
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");

        // Agregar el nombre de usuario a la URL como parámetro de consulta
        String urlWithParams = URL + "?username=" + encode(username);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                urlWithParams,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        // Si la respuesta es vacía, mostrar estado sin datos
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
                            // Tratar 204/404 como "sin datos" en lugar de error
                            if (code == 204 || code == 404) {
                                mostrarListaVacia();
                                return;
                            }
                        }
                        // Errores de red comunes
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
            linearLayoutContainer.removeAllViews();
            TextView emptyView = new TextView(requireContext());
            emptyView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            emptyView.setText("No hay pedidos para mostrar.");
            emptyView.setTextSize(16);
            emptyView.setTextColor(Color.DKGRAY);
            emptyView.setPadding(24, 24, 24, 24);
            linearLayoutContainer.addView(emptyView);
        });
    }

    /*private void mostrarPedidos(JSONArray response) {
        pedidosArray = response; // Almacenar el JSONArray globalmente

        try {
            linearLayoutContainer.removeAllViews();

            for (int i = 0; i < response.length(); i++) {
                final JSONObject pedido = response.getJSONObject(i);
                agregarPedidoALayout(pedido);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }*/




    private void mostrarPedidos(JSONArray response) {
        pedidosArray = response; // Almacenar el JSONArray globalmente

        try {
            linearLayoutContainer.removeAllViews();

            for (int i = 0; i < response.length(); i++) {
                final JSONObject pedido = response.getJSONObject(i);
                String estado = pedido.getString("ESTADO");
                // Mostrar solo los pedidos entregados o cancelados
                if (estado.equals("ACTIVO") || estado.equals("EN RUTA")|| estado.equals("REPROGRAMADO")|| estado.equals("EN TIENDA")) {
                    agregarPedidoALayout(pedido);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void agregarPedidoALayout(final JSONObject pedido) throws JSONException {
        // Crear un CardView para cada pedido
        CardView cardView = new CardView(requireContext());

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16); // Agregar margen inferior entre los pedidos
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(16); // Establecer el radio de las esquinas del CardView

        // Obtener el estado del pedido
        String estado = pedido.getString("ESTADO");


        if (estado.equals("ACTIVO")) {
            cardView.setCardBackgroundColor(Color.parseColor("#CCE5FF")); // Azul
        } else if (estado.equals("EN RUTA")) {
            cardView.setCardBackgroundColor(Color.parseColor("#FFD699")); // Naranja
        } else if (estado.equals("REPROGRAMADO")) {
            cardView.setCardBackgroundColor(Color.parseColor("#E6CCFF")); // Morado
        }else if (estado.equals("EN TIENDA")) {
            cardView.setCardBackgroundColor(Color.parseColor("#FFFFCC")); // Amarillo
        }else {
            cardView.setCardBackgroundColor(Color.WHITE); // Fondo blanco por defecto
        }


        // Establecer la elevación para que los pedidos tengan sombra
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cardView.setElevation(8);
        }

        // Crear un LinearLayout para contener la información del pedido
        LinearLayout linearLayout = new LinearLayout(requireContext());
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(16, 16, 16, 16);

        // TextView para mostrar el ID del pedido
        TextView textOrderId = new TextView(requireContext());
        textOrderId.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        textOrderId.setText("ID: " + pedido.getString("ID"));
        textOrderId.setTextSize(16);

        // TextView para mostrar la sucursal del pedido
        TextView textOrderTitle = new TextView(requireContext());
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
            case "ACTIVO":
                filterColor = Color.parseColor("#576977"); // Azul
                break;
            case "EN RUTA":
                filterColor = Color.parseColor("#7A5D3D"); // Naranja
                break;
            case "REPROGRAMADO":
                filterColor = Color.parseColor("#715C5B"); // Morado
                break;
            case "EN TIENDA":
                filterColor = Color.parseColor("#78785E"); // Amarillo
                break;
            default:
                filterColor = Color.WHITE; // Fondo blanco por defecto
                break;
        }

        imageOrderTitle.setColorFilter(filterColor, PorterDuff.Mode.SRC_IN);


        // Configurar textOrderTitle
        //  textOrderTitle.setText("Sucursal: " + pedido.getString("SUCURSAL"));
      //  textOrderTitle.setText("Sucursal:"+ imageParams);
        textOrderTitle.setTextSize(18);
        textOrderTitle.setTypeface(null, Typeface.BOLD);


// Agregar imageOrderTitle al layout




        // TextView para mostrar el nombre del cliente
        TextView textOrderDetails = new TextView(requireContext());
        textOrderDetails.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        textOrderDetails.setText("Cliente: " + pedido.getString("NOMBRE_CLIENTE"));
        textOrderDetails.setTextSize(16);

        // TextView para mostrar el estado del pedido
        TextView textOrderState = new TextView(requireContext());
        textOrderState.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        textOrderState.setText("Estado: " + pedido.getString("ESTADO"));
        textOrderState.setTextSize(16);

        // TextView para mostrar la fecha de recepción del pedido
        TextView textOrderDate = new TextView(requireContext());
        textOrderDate.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        textOrderDate.setText("Fecha Recepción: " + pedido.getString("FECHA_RECEPCION_FACTURA"));
        textOrderDate.setTextSize(16);

        // Crear el botón Ver Detalles
        Button btnVerDetalle = new Button(requireContext());
        btnVerDetalle.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        btnVerDetalle.setText("Ver Detalles");
        btnVerDetalle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Obtener el pedido actual según la posición del botón
                    JSONObject pedidoSeleccionado = pedidosArray.getJSONObject(linearLayoutContainer.indexOfChild((View) v.getParent().getParent()));
                    // Verificar si todos los campos necesarios están presentes en el JSON
                    if (pedidoSeleccionado.has("ID") && pedidoSeleccionado.has("SUCURSAL")
                            && pedidoSeleccionado.has("NOMBRE_CLIENTE") && pedidoSeleccionado.has("ESTADO")
                            && pedidoSeleccionado.has("FECHA_RECEPCION_FACTURA")) {

                        // Intent para abrir la actividad de detalles del pedido
                        Intent intent = new Intent(requireContext(), DetallePedidoActivity.class);
                        intent.putExtra("ID", pedidoSeleccionado.getString("ID"));
                        intent.putExtra("SUCURSAL", pedidoSeleccionado.getString("SUCURSAL"));
                        intent.putExtra("NOMBRE_CLIENTE", pedidoSeleccionado.getString("NOMBRE_CLIENTE"));
                        intent.putExtra("ESTADO", pedidoSeleccionado.getString("ESTADO"));
                        intent.putExtra("FECHA_RECEPCION_FACTURA", pedidoSeleccionado.getString("FECHA_RECEPCION_FACTURA"));
                        intent.putExtra("FECHA_ENTREGA_CLIENTE",pedidoSeleccionado.getString("FECHA_ENTREGA_CLIENTE"));
                        intent.putExtra("CHOFER_ASIGNADO", pedidoSeleccionado.getString("CHOFER_ASIGNADO"));
                        intent.putExtra("VENDEDOR", pedidoSeleccionado.getString("VENDEDOR"));
                        intent.putExtra("FACTURA", pedidoSeleccionado.getString("FACTURA"));
                        intent.putExtra("DIRECCION", pedidoSeleccionado.getString("DIRECCION"));
                        intent.putExtra("FECHA_MIN_ENTREGA", pedidoSeleccionado.getString("FECHA_MIN_ENTREGA"));
                        intent.putExtra("FECHA_MAX_ENTREGA", pedidoSeleccionado.getString("FECHA_MAX_ENTREGA"));
                        intent.putExtra("MIN_VENTANA_HORARIA_1", pedidoSeleccionado.getString("MIN_VENTANA_HORARIA_1"));
                        intent.putExtra("MAX_VENTANA_HORARIA_1", pedidoSeleccionado.getString("MAX_VENTANA_HORARIA_1"));
                        intent.putExtra("TELEFONO", pedidoSeleccionado.getString("TELEFONO"));
                        intent.putExtra("CONTACTO", pedidoSeleccionado.getString("CONTACTO"));
                        intent.putExtra("COMENTARIOS", pedidoSeleccionado.getString("COMENTARIOS"));
                        intent.putExtra("Ruta", pedidoSeleccionado.getString("Ruta"));
                        intent.putExtra("Coord_Origen", pedidoSeleccionado.getString("Coord_Origen"));
                        intent.putExtra("Coord_Destino", pedidoSeleccionado.getString("Coord_Destino"));
                        startActivity(intent);
                    } else {
                        // Mostrar un mensaje de error si algún campo está faltante
                        Log.e("HomeFragment", "Algunos campos del pedido faltan en el JSON");
                        // Aquí puedes mostrar un mensaje al usuario si lo deseas
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    // Manejar la excepción JSONException si ocurre
                    Log.e("HomeFragment", "Error al procesar JSON: " + e.getMessage());
                    // Aquí puedes mostrar un mensaje de error al usuario si lo deseas
                }
            }
        });

        // Agregar los elementos al LinearLayout
        linearLayout.addView(imageOrderTitle);
     //   linearLayout.addView(textOrderTitle);

        linearLayout.addView(textOrderId);
        linearLayout.addView(textOrderDetails);
        linearLayout.addView(textOrderState);
        linearLayout.addView(textOrderDate);

        // Agregar el botón al LinearLayout
        linearLayout.addView(btnVerDetalle);

        // Agregar el LinearLayout al CardView
        cardView.addView(linearLayout);

        // Agregar el CardView al contenedor principal
        linearLayoutContainer.addView(cardView);
    }

    private void mostrarMensaje(String mensaje) {
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show();
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
