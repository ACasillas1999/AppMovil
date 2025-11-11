package com.example.app_pedidos.ui.Estadisticas;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.example.app_pedidos.network.Utf8JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.StringRequest;
import com.example.app_pedidos.network.Utf8StringRequest;
import com.example.app_pedidos.R;
import com.example.app_pedidos.ApiConfig;
import com.example.app_pedidos.databinding.FragmentEstBinding;
import com.example.app_pedidos.ui.Login.LoginActivity;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.net.URLEncoder;

public class EstadisticasFragment extends Fragment {

    private FragmentEstBinding binding;
    private Timer timer;
    private PieChart pieChart;
    private SharedPreferences sharedPreferences;
    private TableLayout tableLayout;
    private TextView monthLabel;
    private Button previousMonthButton;
    private Button currentMonthButton;
    private String currentMonth;
    private AlertDialog noVehiculoDialog;

    // Colores para las porciones del grÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡fico
    private final int[] chartColors = new int[]{0xFFCCE5FF, 0xFFFFCCCC, 0xFFCCFFCC, 0xFFFFD699, 0xFFFFFFCC, 0xFFCC99FF};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEstBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        pieChart = root.findViewById(R.id.pie_chart);
        tableLayout = root.findViewById(R.id.table_layout);
        monthLabel = root.findViewById(R.id.month_label);
        previousMonthButton = root.findViewById(R.id.previous_month_button);
        currentMonthButton = root.findViewById(R.id.current_month_button);

        // Obtener SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE);

        // Obtener el nombre de usuario de SharedPreferences
        String username = sharedPreferences.getString("username", "");

        // Configurar el mes actual
        currentMonth = getCurrentMonth();
        monthLabel.setText(currentMonth);

        // URL del archivo PHP para el mes actual
     //   String currentMonthURL = "https://pedidos.grupoascencio.com.mx/Pedidos_GA/App/Pedidos_Mes.php?username=" + encode(username) + "&mes=" + encode(currentMonth);

        String currentMonthURL = ApiConfig.BASE_URL + "/Pedidos_GA/App/Pedidos_Mes.php?username=" + encode(username) + "&mes=" + encode(currentMonth);

        // Cargar los datos del mes actual por defecto solo si tiene vehÃƒÆ’Ã‚Â­culo asignado
        verificarVehiculoAsignadoThen(() -> obtenerEstadosPedidos(currentMonthURL));

        // Configurar el botÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n para consultar el mes pasado
        previousMonthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentMonth = getPreviousMonth();
                monthLabel.setText(currentMonth);
              //  String previousMonthURL = "https://pedidos.grupoascencio.com.mx/Pedidos_GA/App/Pedidos_MesAnterior.php?username=" + encode(username) + "&mes=" + encode(currentMonth);
                String previousMonthURL = ApiConfig.BASE_URL + "/Pedidos_GA/App/Pedidos_MesAnterior.php?username=" + encode(username) + "&mes=" + encode(currentMonth);

                
                verificarVehiculoAsignadoThen(() -> obtenerEstadosPedidos(previousMonthURL));
                previousMonthButton.setBackgroundResource(R.drawable.mespana);
                currentMonthButton.setBackgroundResource(R.drawable.mesactna);



            }
        });

        // Configurar el botÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n para volver al mes actual
        currentMonthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentMonth = getCurrentMonth();
                monthLabel.setText(currentMonth);
              //  String currentMonthURL = "https://pedidos.grupoascencio.com.mx/Pedidos_GA/App/Pedidos_Mes.php?username=" + encode(username) + "&mes=" + encode(currentMonth);
                String currentMonthURL = ApiConfig.BASE_URL + "/Pedidos_GA/App/Pedidos_Mes.php?username=" + encode(username) + "&mes=" + encode(currentMonth);
  
              verificarVehiculoAsignadoThen(() -> obtenerEstadosPedidos(currentMonthURL));
                currentMonthButton.setBackgroundResource(R.drawable.mesactna);
                previousMonthButton.setBackgroundResource(R.drawable.mespana);

            }
        });

        // Iniciar la actualizaciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n automÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡tica al crear la vista
       // iniciarActualizacionPeriodica();

        return root;
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }


 /*   private void iniciarActualizacionPeriodica() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                obtenerEstadosPedidos(currentMonth);
            }
        }, 0, 5000); // Actualizar cada 5 segundos
    }*/

    /*

     private void obtenerEstadosPedidos(String mes) {
        // Obtener el nombre de usuario de SharedPreferences
        String username = sharedPreferences.getString("username", "");

        // URL del archivo PHP que maneja la consulta de pedidos
        String URL = "https://pedidos.grupoascencio.com.mx/Pedidos_GA/App/Pedidos.php?username=" + username + "&mes=" + mes;

        JsonArrayRequest jsonArrayRequest = new com.example.app_pedidos.network.Utf8JsonArrayRequest(
                Request.Method.GET,
                URL,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            mostrarGrafico(response);
                            mostrarTabla(response);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            mostrarError("Error al procesar los datos JSON");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        mostrarError("Error en la solicitud HTTP");
                    }
                }
        );

        Volley.newRequestQueue(requireContext()).add(jsonArrayRequest);
    }

    */


    private void obtenerEstadosPedidos(String url) {
        JsonArrayRequest jsonArrayRequest = new com.example.app_pedidos.network.Utf8JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            if (response == null || response.length() == 0) {
                                mostrarSinDatos();
                            } else {
                                mostrarGrafico(response);
                                mostrarTabla(response);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            mostrarError("Error al procesar los datos JSON");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        if (error.networkResponse != null) {
                            int code = error.networkResponse.statusCode;
                            byte[] data = error.networkResponse.data;
                            String body = null;
                            if (data != null && data.length > 0) {
                                try {
                                    body = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                                } catch (Exception ignored) {
                                    body = new String(data);
                                }
                            }
                            // Tratar 200/204/404 vacÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â­os o cuerpo [] como "sin datos"
                            if (code == 204 || code == 404 || code == 200) {
                                if (data == null || data.length == 0 || (body != null && body.trim().equals("[]"))) {
                                    mostrarSinDatos();
                                    return;
                                }
                            }
                        }
                        if (error instanceof com.android.volley.ParseError) {
                            // Cuerpo vacio o no JSON: tratalo como sin datos
                            mostrarSinDatos();
                            return;
                        }
                        mostrarError("Problema de conexión. Intenta de nuevo.");
                    }
                }
        );

        Volley.newRequestQueue(requireContext()).add(jsonArrayRequest);
    }

    private void mostrarSinDatos() {
        // Dejar el grÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡fico con mensaje de sin datos y una fila informativa en tabla
        if (pieChart != null) {
            pieChart.clear();
            pieChart.setNoDataText("No hay datos disponibles para mostrar.");
            pieChart.invalidate();
        }
        if (tableLayout != null) {
            tableLayout.removeAllViews();
            TableRow row = new TableRow(requireContext());
            TextView tv = new TextView(requireContext());
            tv.setText("No hay datos disponibles");
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(16,16,16,16);
            tv.setTextColor(Color.DKGRAY);
            row.addView(tv);
            tableLayout.addView(row);
        }
    }


    private void mostrarGrafico(JSONArray data) throws JSONException {
        ArrayList<PieEntry> entries = new ArrayList<>();
        String[] estados = {"ACTIVO", "CANCELADO", "ENTREGADO", "EN RUTA", "EN TIENDA", "REPROGRAMADO"};

        // Definir los colores para cada estado
        HashMap<String, Integer> colorEstados = new HashMap<>();
        colorEstados.put("ACTIVO", 0xFFCCE5FF);
        colorEstados.put("CANCELADO", 0xFFFFCCCC);
        colorEstados.put("ENTREGADO", 0xFFCCFFCC);
        colorEstados.put("EN RUTA", 0xFFFFD699);
        colorEstados.put("EN TIENDA", 0xFFFFFFCC);
        colorEstados.put("REPROGRAMADO", 0xFFCC99FF);

        // Inicializar todos los estados con cantidad 0
        HashMap<String, Integer> cantidades = new HashMap<>();
        for (String estado : estados) {
            cantidades.put(estado, 0);
        }

        // Actualizar las cantidades con los datos recibidos
        for (int i = 0; i < data.length(); i++) {
            JSONObject estadoPedido = data.getJSONObject(i);
            String estado = estadoPedido.getString("estado");
            int cantidad = estadoPedido.getInt("cantidad");

            if (cantidades.containsKey(estado)) {
                cantidades.put(estado, cantidad);
            }
        }

        // AÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â±adir las entradas al ArrayList y asociar colores solo si la cantidad es mayor a 0
        ArrayList<Integer> colors = new ArrayList<>();
        for (String estado : estados) {
            int cantidad = cantidades.get(estado);
            if (cantidad > 0) { // Solo agregar si hay registros para ese estado
                entries.add(new PieEntry(cantidad, estado));
                colors.add(colorEstados.get(estado));
            }
        }

        // Si no hay entradas vÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡lidas, mostrar un mensaje de "No hay datos disponibles"
        if (entries.isEmpty()) {
            pieChart.clear(); // Limpia el grÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡fico si no hay datos
            pieChart.setNoDataText("No hay datos disponibles para mostrar.");
            pieChart.invalidate(); // Redibujar el grÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡fico con el mensaje de "No hay datos"
            return;
        }

        PieDataSet pieDataSet = new PieDataSet(entries, "Cantidad de Pedidos");
        pieDataSet.setColors(colors);
        pieDataSet.setValueTextSize(12f);
        pieDataSet.setValueTextColor(Color.BLACK); // Cambiar el color del texto a negro

        // Asegurar que las etiquetas de los segmentos se vean en negro
        pieDataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        pieDataSet.setValueTextColor(Color.BLACK);

        PieData pieData = new PieData(pieDataSet);
        pieData.setValueTextColor(Color.BLACK); // Asegurar que el color del texto tambiÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©n sea negro
        pieChart.setData(pieData);

        // ConfiguraciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n del fondo del grÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡fico
        pieChart.setDrawHoleEnabled(false); // Elimina el agujero en el centro si estÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ habilitado
        pieChart.setDrawEntryLabels(true); // Asegura que las etiquetas estÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©n visibles
        pieChart.setEntryLabelColor(Color.BLACK); // Cambia el color de las etiquetas a negro
        pieChart.setBackgroundColor(Color.WHITE); // Establece un fondo blanco para la grÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡fica

        pieChart.invalidate();

        // ConfiguraciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n de la leyenda
        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(Color.BLACK); // Cambiar el color de la leyenda a negro
    }





    private void mostrarTabla(JSONArray data) throws JSONException {
        tableLayout.removeAllViews(); // Limpiar la tabla antes de actualizarla

        // Crear un estilo para las celdas
        TableRow.LayoutParams cellParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        );
        cellParams.setMargins(8, 8, 8, 8); // Margen entre celdas

        // AÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â±adir las filas de datos
        for (int i = 0; i < data.length(); i++) {
            JSONObject estadoPedido = data.getJSONObject(i);
            String estado = estadoPedido.getString("estado");
            int cantidad = estadoPedido.getInt("cantidad");

            TableRow tableRow = new TableRow(requireContext());

            // Alternar color de fondo para filas
            if (i % 2 == 0) {
                tableRow.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.naranja));
            } else {
                tableRow.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.naranja));
            }

            TextView textViewEstado = new TextView(requireContext());
            textViewEstado.setText(estado);
            textViewEstado.setTextColor(Color.WHITE);
            textViewEstado.setGravity(Gravity.CENTER); // Centrar contenido
            textViewEstado.setPadding(16, 16, 16, 16); // Ajustar el relleno
            textViewEstado.setLayoutParams(cellParams);
            tableRow.addView(textViewEstado);

            TextView textViewCantidad = new TextView(requireContext());
            textViewCantidad.setText(String.valueOf(cantidad));
            textViewCantidad.setTextColor(Color.WHITE);
            textViewCantidad.setGravity(Gravity.CENTER); // Centrar contenido
            textViewCantidad.setPadding(16, 16, 16, 16); // Ajustar el relleno
            textViewCantidad.setLayoutParams(cellParams);
            tableRow.addView(textViewCantidad);

            tableLayout.addView(tableRow);
        }
    }



    private void mostrarError(final String mensaje) {
        // Mostrar el Toast en el hilo principal
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                com.example.app_pedidos.ui.common.Notifier.error(requireActivity(), mensaje);
            }
        });
    }

    private void verificarVehiculoAsignadoThen(Runnable onOk) {
        if (getActivity() == null) return;
        final String urlVerificar = ApiConfig.BASE_URL + "/Pedidos_GA/App/verificar_vehiculo.php";
        final String username = sharedPreferences.getString("username", "");

        StringRequest request = new com.example.app_pedidos.network.Utf8StringRequest(Request.Method.POST, urlVerificar,
                response -> {
                    String body = response == null ? "" : response.trim().toUpperCase();
                    if ("ASIGNADO".equals(body)) {
                        if (noVehiculoDialog != null && noVehiculoDialog.isShowing()) {
                            noVehiculoDialog.dismiss();
                        }
                        if (onOk != null) onOk.run();
                    } else {
                        mostrarDialogoSinVehiculo();
                    }
                },
                error -> mostrarError("Error de conexion")) {
            @Override
            protected java.util.Map<String, String> getParams() {
                java.util.Map<String, String> p = new java.util.HashMap<>();
                p.put("username", username);
                return p;
            }
        };

        Volley.newRequestQueue(requireContext()).add(request);
    }

    private void mostrarDialogoSinVehiculo() {
        if (noVehiculoDialog != null && noVehiculoDialog.isShowing()) return;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_Pedidos_MaterialAlertDialog);
builder.setTitle("Vehiculo no asignado");
builder.setMessage("Solicita a tu Jefe de choferes de Sucursal que te asigne un vehiculo para continuar");
builder.setCancelable(false);
builder.setPositiveButton("Cerrar sesion", (dialog, which) -> cerrarSesion());
noVehiculoDialog = builder.create();
noVehiculoDialog.setCanceledOnTouchOutside(false);
noVehiculoDialog.show();
    }

    private void cerrarSesion() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("username");
        editor.apply();
        android.content.Intent intent = new android.content.Intent(requireContext(), LoginActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

    private String getCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }

    private String getPreviousMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Detener la actualización periódica
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (noVehiculoDialog != null && noVehiculoDialog.isShowing()) { noVehiculoDialog.dismiss(); }
        binding = null;
    }
}



