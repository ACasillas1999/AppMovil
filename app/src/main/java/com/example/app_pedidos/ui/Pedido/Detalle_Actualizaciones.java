package com.example.app_pedidos.ui.Pedido;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.app_pedidos.R;
import com.example.app_pedidos.ApiConfig;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Detalle_Actualizaciones extends AppCompatActivity {



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detalle_actualizaciones);

      /*  Button btnVolver = findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });*/

        // Obtener el ID del pedido enviado desde el intent
        String pedidoId = getIntent().getStringExtra("ID_PEDIDO");

        // Mostrar el ID del pedido en un TextView
        TextView txtPedidoId = findViewById(R.id.txtPedidoId);
        txtPedidoId.setText("ID del Pedido: " + pedidoId);

        // Hacer una solicitud al servidor para obtener los detalles del pedido
        obtenerDetallesPedido(pedidoId);
        obtenerImagenPedido(pedidoId);

        // Pull-to-refresh para recargar datos del pedido
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe = findViewById(R.id.swipeDetalleAct);
        if (swipe != null) {
            swipe.setOnRefreshListener(() -> {
                obtenerDetallesPedido(pedidoId);
                obtenerImagenPedido(pedidoId);
            });
            swipe.setColorSchemeResources(
                    android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light
            );
        }


        Toolbar toolbar = findViewById(R.id.toolbar_Pedidos);
        ImageButton yourButton = toolbar.findViewById(R.id.VOlverBton);

        yourButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Acción del botón
                finish(); // Volver a la ventana anterior
            }
        });


    }

    private void obtenerDetallesPedido(String pedidoId) {
        //String url = "https://pedidos.grupoascencio.com.mx/Pedidos_GA/App/Detalle_Actualizaciones.php?id_pedido=" + pedidoId;
        String url = ApiConfig.BASE_URL + "/Pedidos_GA/App/Detalle_Actualizaciones.php?id_pedido=" + pedidoId;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        // Procesar la respuesta y mostrar los datos en la tabla
                        mostrarDetallesPedido(response);
                        try { ((androidx.swiperefreshlayout.widget.SwipeRefreshLayout)findViewById(R.id.swipeDetalleAct)).setRefreshing(false); } catch (Exception ignore) {}
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Manejar el error
                        com.example.app_pedidos.ui.common.Notifier.error(Detalle_Actualizaciones.this, "Error al obtener los detalles del pedido: " + error.getMessage());
                        try { ((androidx.swiperefreshlayout.widget.SwipeRefreshLayout)findViewById(R.id.swipeDetalleAct)).setRefreshing(false); } catch (Exception ignore) {}
                    }
                });

        // Añadir la solicitud a la cola
        Volley.newRequestQueue(this).add(request);
    }

    private void mostrarDetallesPedido(JSONArray detalles) {
        TableLayout tableLayout = findViewById(R.id.tableLayoutDetalles);

        try {
            // Crear una fila de cabecera
            TableRow headerRow = new TableRow(this);
            headerRow.setLayoutParams(new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            headerRow.setBackgroundColor(Color.parseColor("#005aa3")); // Fondo azul para la cabecera

            String[] headers = {"Estado", "Fecha", "Hora", "Ver en Maps"};
            for (String header : headers) {
                TextView textView = new TextView(this);
                textView.setText(header);
                textView.setPadding(10, 30, 30, 30);
                textView.setTextColor(Color.WHITE);
                textView.setTypeface(null, Typeface.BOLD);
                textView.setGravity(Gravity.CENTER);
                textView.setBackgroundResource(R.drawable.cell_border); // Asignar borde a la cabecera
                headerRow.addView(textView);
            }
            tableLayout.addView(headerRow);

            for (int i = 0; i < detalles.length(); i++) {
                JSONObject detalle = detalles.getJSONObject(i);

                TableRow tableRow = new TableRow(this);
                tableRow.setLayoutParams(new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                tableRow.setBackgroundResource(R.drawable.cell_border); // Asignar borde a cada fila

                String[] values = {detalle.getString("Estado"), detalle.getString("Fecha"), detalle.getString("Hora")};
                for (String value : values) {
                    TextView textView = new TextView(this);
                    textView.setText(value);
                    textView.setPadding(10, 30, 30, 30);
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextColor(Color.parseColor("#005aa3"));
                    textView.setBackgroundResource(R.drawable.cell_border); // Asignar borde a cada celda
                    tableRow.addView(textView);
                }

                // Crear un botón para ver la coordenada en Google Maps
                ImageButton btnVerEnMaps = new ImageButton(this);
                btnVerEnMaps.setImageResource(R.drawable.vermapsgr);
                TableRow.LayoutParams params = new TableRow.LayoutParams(100, 100);
                params.gravity = Gravity.TOP; // Centrar el botón en la fila
                btnVerEnMaps.setLayoutParams(params);
                btnVerEnMaps.setScaleType(ImageView.ScaleType.CENTER_INSIDE);


                btnVerEnMaps.setBackgroundColor(Color.TRANSPARENT);
                btnVerEnMaps.setTag(detalle.getString("Coordenada"));

                btnVerEnMaps.setOnClickListener(v -> {
                    String coordenada = (String) v.getTag();
                    Uri gmmIntentUri = Uri.parse("geo:" + coordenada + "?q=" + coordenada);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + coordenada);
                        Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
                        startActivity(webIntent);
                    }
                });

                // Agregar el botón a la misma fila que la información
                tableRow.addView(btnVerEnMaps);
                tableLayout.addView(tableRow);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            com.example.app_pedidos.ui.common.Notifier.error(this, "Error al procesar los detalles del pedido");
        }
    }


    private void obtenerImagenPedido(String pedidoId) {
       // String url = "https://pedidos.grupoascencio.com.mx/Pedidos_GA/App/Ver_Foto.php?id_pedido=" + pedidoId;
        String url = ApiConfig.BASE_URL + "/Pedidos_GA/App/Ver_Foto.php?id_pedido=" + pedidoId;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        // Procesar la respuesta y mostrar la imagen
                        mostrarImagenPedido(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Manejar el error
                        com.example.app_pedidos.ui.common.Notifier.error(Detalle_Actualizaciones.this, "Error al obtener la imagen del pedido: " + error.getMessage());
                    }
                });

        // Añadir la solicitud a la cola
        Volley.newRequestQueue(this).add(request);
    }

    private void mostrarImagenPedido(JSONArray response) {
        try {
            JSONObject jsonObject = response.getJSONObject(0);
            String rutaImagen = jsonObject.getString("ruta_imagen");

            // Cargar la imagen en el ImageView usando Picasso
            ImageView imageView = findViewById(R.id.imageViewPedido);
            Picasso.get()
                    .load(rutaImagen)
                    .resize(400, 400)  // Ajusta el tamaño según tus necesidades
                    .centerCrop()
                    .into(imageView, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d("Picasso", "Imagen cargada exitosamente");
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("Picasso", "Error al cargar la imagen", e);
                        }
                    });

            // Configurar el click listener para mostrar la imagen completa
            imageView.setOnClickListener(v -> mostrarImagenCompleta(rutaImagen));
        } catch (JSONException e) {
            e.printStackTrace();
            com.example.app_pedidos.ui.common.Notifier.error(this, "Error al procesar la respuesta para la imagen del pedido");
        }
    }

    private void mostrarImagenCompleta(String rutaImagen) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_imagen_completa);
        ImageView imageView = dialog.findViewById(R.id.imageViewCompleta);

        // Usar Picasso para cargar la imagen completa
        Picasso.get()
                .load(rutaImagen)
                .into(imageView);

        dialog.show();
    }



}
