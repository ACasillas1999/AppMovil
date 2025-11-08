package com.example.app_pedidos.ui.vehicle;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.app_pedidos.ApiConfig;
import com.example.app_pedidos.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

public class VehicleFragment extends Fragment {

    private static final String TAG = "VehicleFragment";
    private static final String URL = ApiConfig.BASE_URL + "/Pedidos_GA/App/Consultar.php";

    // Referencias a las vistas
    private TextView tvTipoVehiculo;
    private TextView tvPlaca;
    private TextView tvNumeroSerie;
    private TextView tvIdVehiculo;
    private TextView tvSucursal;
    private TextView tvKmActual;
    private TextView tvNoVehiculo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.vehiculos, container, false);

        // Inicializar las vistas
        tvTipoVehiculo = root.findViewById(R.id.tv_tipo_vehiculo);
        tvPlaca = root.findViewById(R.id.tv_placa);
        tvNumeroSerie = root.findViewById(R.id.tv_numero_serie);
        tvIdVehiculo = root.findViewById(R.id.tv_id_vehiculo);
        tvSucursal = root.findViewById(R.id.tv_sucursal);
        tvKmActual = root.findViewById(R.id.tv_km_actual);
        tvNoVehiculo = root.findViewById(R.id.tv_no_vehiculo);

        // Obtener datos del vehículo
        obtenerDatosVehiculo();

        return root;
    }

    private void obtenerDatosVehiculo() {
        // Obtener el nombre de usuario de SharedPreferences
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");

        // Construir URL con parámetros
        String urlWithParams = URL + "?username=" + encode(username) + "&v2=1";

        Log.d(TAG, "Consultando vehículo para usuario: " + username);
        Log.d(TAG, "URL: " + urlWithParams);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                urlWithParams,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.d(TAG, "Respuesta recibida: " + response.toString());

                            boolean vehiculoAsignado = response.optBoolean("vehiculo_asignado", false);

                            if (vehiculoAsignado && response.has("vehiculo")) {
                                JSONObject vehiculo = response.getJSONObject("vehiculo");
                                mostrarDatosVehiculo(vehiculo);
                            } else {
                                mostrarSinVehiculo();
                            }

                        } catch (JSONException e) {
                            Log.e(TAG, "Error al parsear JSON: " + e.getMessage());
                            mostrarError("Error al procesar los datos del vehículo");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error en la solicitud: " + error.toString());
                        mostrarError("Error al obtener datos del vehículo");
                    }
                }
        );

        // Agregar la solicitud a la cola de Volley
        Volley.newRequestQueue(requireContext()).add(jsonObjectRequest);
    }

    private void mostrarDatosVehiculo(JSONObject vehiculo) {
        try {
            // Extraer los datos del JSON
            int idVehiculo = vehiculo.optInt("id_vehiculo", 0);
            String placa = vehiculo.optString("placa", "N/A");
            String numeroSerie = vehiculo.optString("numero_serie", "N/A");
            String tipo = vehiculo.optString("tipo", "N/A");
            int kmActual = vehiculo.optInt("Km_Actual", 0);
            String sucursal = vehiculo.optString("Sucursal", "N/A");

            // Actualizar las vistas en el hilo principal
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvTipoVehiculo.setText(tipo);
                        tvPlaca.setText(placa);
                        tvNumeroSerie.setText(numeroSerie);
                        tvIdVehiculo.setText(String.valueOf(idVehiculo));
                        tvSucursal.setText(sucursal);
                        tvKmActual.setText(String.format("%,d KM", kmActual));

                        // Mostrar las tarjetas de información
                        tvNoVehiculo.setVisibility(View.GONE);

                        Log.d(TAG, "Datos del vehículo mostrados correctamente");
                    }
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error al mostrar datos del vehículo: " + e.getMessage());
            mostrarError("Error al mostrar los datos");
        }
    }

    private void mostrarSinVehiculo() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Ocultar los datos y mostrar el mensaje
                    tvNoVehiculo.setVisibility(View.VISIBLE);

                    // Limpiar los campos
                    tvTipoVehiculo.setText("N/A");
                    tvPlaca.setText("N/A");
                    tvNumeroSerie.setText("N/A");
                    tvIdVehiculo.setText("N/A");
                    tvSucursal.setText("N/A");
                    tvKmActual.setText("0 KM");

                    Log.d(TAG, "No hay vehículo asignado");
                }
            });
        }
    }

    private void mostrarError(String mensaje) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    com.example.app_pedidos.ui.common.Notifier.info(requireActivity(), mensaje);
                    Log.e(TAG, mensaje);
                }
            });
        }
    }

    // Método auxiliar para codificar URL
    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "Error al codificar URL: " + e.getMessage());
            return value;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "VehicleFragment destruido");
    }
}
