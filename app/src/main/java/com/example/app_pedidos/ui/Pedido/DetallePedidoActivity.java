package com.example.app_pedidos.ui.Pedido;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.app_pedidos.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.Priority;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.CancellationTokenSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DetallePedidoActivity extends AppCompatActivity {

    private String coordenadasOrigen;
    private String coordenadasDestino;
    private String rutaDocumento;
    private Uri selectedImageUri;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2001;
    private static final int REQUEST_LOCATION_PERMISSION = 2002;
    private static final int REQUEST_CHECK_SETTINGS = 3001;


    private long downloadId; // ID de la descarga
    private String pedidoId; // ID del pedido
    private boolean isDownloadReceiverRegistered = false;
    private String pendingEstado = null; // Estado pendiente si falta permiso de ubicación



    // Eliminado LocationManager/LocationListener: usaremos solo FusedLocationProviderClient

    private final BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == downloadId) {
                // Manejar la descarga completada aquí
                Toast.makeText(DetallePedidoActivity.this, "Documento descargado correctamente", Toast.LENGTH_SHORT).show();
            }
        }
    };


    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_pedido);

        // Registro del BroadcastReceiver para el evento de descarga completa
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(downloadCompleteReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            isDownloadReceiverRegistered = true;
        } else {
            registerReceiver(downloadCompleteReceiver, filter);
            isDownloadReceiverRegistered = true;
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


        Button btnDetalles = findViewById(R.id.btnDetalles);
        btnDetalles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(pedidoId)) {
                    Intent intent = new Intent(DetallePedidoActivity.this, Detalle_Actualizaciones.class);
                    intent.putExtra("ID_PEDIDO", pedidoId);
                    startActivity(intent);
                } else {
                    Toast.makeText(DetallePedidoActivity.this, "Por favor, ingrese un ID de pedido válido.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Eliminado manejo de LocationManager; se usará FusedLocation al solicitar coordenadas

        Bundle extras = getIntent().getExtras();
        if (extras != null && !extras.isEmpty()) {
            mostrarDatosPedido(extras);
        } else {
            mostrarError();
        }

       /* Button btnVolver = findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });*/

        Button btnAbrirGoogleMaps = findViewById(R.id.btnAbrirGoogleMaps);
        btnAbrirGoogleMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(coordenadasOrigen) && !TextUtils.isEmpty(coordenadasDestino)) {
                    openGoogleMaps(coordenadasOrigen, coordenadasDestino);
                } else {
                    Toast.makeText(DetallePedidoActivity.this, "No se encontraron coordenadas para abrir Google Maps", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button btnDescargarDocumento = findViewById(R.id.btnDescargarDocumento);
        verificarDocumento(btnDescargarDocumento);
        btnDescargarDocumento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                descargarDocumento(rutaDocumento);
            }
        });

        Button btnTomarFoto = findViewById(R.id.btnTomarFoto);

        btnTomarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               checkStoragePermission();
            }
        });

        verificarFoto(btnTomarFoto);
        verificarFotoDesdeServidor(btnTomarFoto);

        // New Button to update status
        Button btnActualizarEstado = findViewById(R.id.btnActualizarEstado);
        btnActualizarEstado.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoActualizarEstado();
            }
        });
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isDownloadReceiverRegistered) {
            try {
                unregisterReceiver(downloadCompleteReceiver);
            } catch (IllegalArgumentException ignored) {
                // En caso de que ya no esté registrado
            }
            isDownloadReceiverRegistered = false;
        }
    }

    private void verificarDocumento(Button btnDescargarDocumento) {
        if (rutaDocumento != null && !rutaDocumento.isEmpty()) {
            btnDescargarDocumento.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green));
        } else {
            btnDescargarDocumento.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.black));
        }
    }

    private void verificarFoto(Button btnTomarFoto) {
        if (isPhotoUploaded()) {
            btnTomarFoto.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green));
        } else {
            btnTomarFoto.setBackgroundResource(R.drawable.tomfotgr); // Usar una imagen diferente cuando no hay foto
        }
    }


    private void verificarFotoDesdeServidor(final Button btnTomarFoto) {
        if (!TextUtils.isEmpty(pedidoId)) {
            OkHttpClient client = new OkHttpClient();
            //String url = "https://pedidos.grupoascencio.com.mx/Pedidos_GA/App/verificar_foto.php?id_pedido=" + pedidoId;
             String url = "http://192.168.60.194/Pedidos_GA/App/verificar_foto.php?id_pedido=" + pedidoId;

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    Log.e("Verificar Foto", "Error al verificar la imagen: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DetallePedidoActivity.this, "Error al verificar la imagen", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    final String responseData = response.body().string();
                    try {
                        JSONArray jsonResponse = new JSONArray(responseData);
                        JSONObject firstElement = jsonResponse.getJSONObject(0);
                        boolean found = firstElement.getBoolean("found");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (found) {
                                    btnTomarFoto.setBackgroundTintList(ContextCompat.getColorStateList(DetallePedidoActivity.this, R.color.green));
                                } else {
                                    btnTomarFoto.setBackgroundTintList(ContextCompat.getColorStateList(DetallePedidoActivity.this, R.color.black));
                                }
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(DetallePedidoActivity.this, "Error al procesar la respuesta del servidor", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        } else {
            Toast.makeText(this, "ID del pedido no válido", Toast.LENGTH_SHORT).show();
        }
    }


    private boolean isPhotoUploaded() {
        SharedPreferences sharedPreferences = getSharedPreferences("DetallePedidoPrefs", MODE_PRIVATE);
        return sharedPreferences.getBoolean("isPhotoUploaded_" + pedidoId, false); // Recuperar estado de la foto con el ID del pedido
    }

    private void savePhotoStatus(boolean isPhotoUploaded) {
        SharedPreferences sharedPreferences = getSharedPreferences("DetallePedidoPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isPhotoUploaded_" + pedidoId, isPhotoUploaded); // Guardar estado de la foto con el ID del pedido
        editor.apply();
    }


    private void descargarDocumento(String rutaDocumento) {
        if (!TextUtils.isEmpty(rutaDocumento)) {
            ///String urlBase = "https://pedidos.grupoascencio.com.mx/Pedidos_GA/";
            String urlBase = "http://192.168.60.194/Pedidos_GA/";
            
            String urlCompleta = urlBase + rutaDocumento;

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(urlCompleta));
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setTitle("Descargando archivo");
            request.setDescription("Descargando archivo del pedido");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, rutaDocumento);

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                downloadId = manager.enqueue(request);
            } else {
                Toast.makeText(DetallePedidoActivity.this, "No se pudo iniciar la descarga", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(DetallePedidoActivity.this, "No se encontró la ruta del documento", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDatosPedido(Bundle datos) {
        ((TextView) findViewById(R.id.textId)).setText(getStringFromBundle(datos, "ID"));
        ((TextView) findViewById(R.id.textSucursal)).setText(getStringFromBundle(datos, "SUCURSAL"));
        ((TextView) findViewById(R.id.textCliente)).setText(getStringFromBundle(datos, "NOMBRE_CLIENTE"));
        ((TextView) findViewById(R.id.textEstado)).setText(getStringFromBundle(datos, "ESTADO"));
        ((TextView) findViewById(R.id.textFechaRecepcion)).setText(getStringFromBundle(datos, "FECHA_RECEPCION_FACTURA"));
        ((TextView) findViewById(R.id.textFechaEntrega)).setText(getStringFromBundle(datos, "FECHA_ENTREGA_CLIENTE"));
        ((TextView) findViewById(R.id.textChofer)).setText(getStringFromBundle(datos, "CHOFER_ASIGNADO"));
        ((TextView) findViewById(R.id.textVendedor)).setText(getStringFromBundle(datos, "VENDEDOR"));
        ((TextView) findViewById(R.id.textFactura)).setText(getStringFromBundle(datos, "FACTURA"));
        ((TextView) findViewById(R.id.textDireccion)).setText(getStringFromBundle(datos, "DIRECCION"));
        ((TextView) findViewById(R.id.textFechaMinEntrega)).setText(getStringFromBundle(datos, "FECHA_MIN_ENTREGA"));
        ((TextView) findViewById(R.id.textFechaMaxEntrega)).setText(getStringFromBundle(datos, "FECHA_MAX_ENTREGA"));
        ((TextView) findViewById(R.id.textVentanaHoraria1)).setText(getVentanaHorariaString(datos, "MIN_VENTANA_HORARIA_1", "MAX_VENTANA_HORARIA_1"));
        ((TextView) findViewById(R.id.textNombreCliente)).setText(getStringFromBundle(datos, "NOMBRE_CLIENTE"));
        ((TextView) findViewById(R.id.textTelefono)).setText(getStringFromBundle(datos, "TELEFONO"));
        ((TextView) findViewById(R.id.textContacto)).setText(getStringFromBundle(datos, "CONTACTO"));
        ((TextView) findViewById(R.id.textComentarios)).setText(getStringFromBundle(datos, "COMENTARIOS"));

        rutaDocumento = getStringFromBundle(datos, "Ruta");

        Button btnDescargarDocumento = findViewById(R.id.btnDescargarDocumento);
        verificarDocumento(btnDescargarDocumento);

        coordenadasOrigen = getStringFromBundle(datos, "Coord_Origen");
        coordenadasDestino = getStringFromBundle(datos, "Coord_Destino");

        pedidoId = getStringFromBundle(datos, "ID"); // Guardar el ID del pedido
    }

    private String getStringFromBundle(Bundle extras, String key) {
        return extras.containsKey(key) ? extras.getString(key) : "No disponible";
    }

    private String getVentanaHorariaString(Bundle extras, String minKey, String maxKey) {
        String minVentanaHoraria = getStringFromBundle(extras, minKey);
        String maxVentanaHoraria = getStringFromBundle(extras, maxKey);
        return TextUtils.isEmpty(minVentanaHoraria) || TextUtils.isEmpty(maxVentanaHoraria) ?
                "No definido" : minVentanaHoraria + " - " + maxVentanaHoraria;
    }

    private void mostrarError() {
        Toast.makeText(this, "Error: No se recibieron los detalles del pedido.", Toast.LENGTH_SHORT).show();
    }

    private void openGoogleMaps(String startCoordinates, String endCoordinates) {
        if (!TextUtils.isEmpty(startCoordinates) && !TextUtils.isEmpty(endCoordinates)) {
            String[] start = startCoordinates.split(",");
            String[] end = endCoordinates.split(",");

            double latitudStart = Double.parseDouble(start[0].trim());
            double longitudStart = Double.parseDouble(start[1].trim());
            double latitudEnd = Double.parseDouble(end[0].trim());
            double longitudEnd = Double.parseDouble(end[1].trim());

            String googleMapsURL = "https://www.google.com/maps/dir/" + latitudStart + "," + longitudStart + "/" + latitudEnd + "," + longitudEnd;
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(googleMapsURL)));
        } else {
            Toast.makeText(this, "No se encontraron coordenadas para abrir Google Maps", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            seleccionarImagenDeGaleria();
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            obtenerCoordenadaActual(new OnUbicacionObtenidaListener() {
                @Override
                public void onUbicacionObtenida(double latitude, double longitude) {
                    // Manejar las coordenadas obtenidas
                    Toast.makeText(DetallePedidoActivity.this, "Ubicación obtenida: Latitud = " + latitude + ", Longitud = " + longitude, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


  /*  @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                seleccionarImagenDeGaleria();
            } else {
                Toast.makeText(this, "Se necesitan permisos de almacenamiento para seleccionar una imagen.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (locationManager != null) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                } else {
                    // Handle case where locationManager is null
                }
            } else {
                Toast.makeText(this, "Se necesitan permisos de ubicación para obtener la ubicación actual.", Toast.LENGTH_SHORT).show();
            }
        }
    }*/



    private void seleccionarImagenDeGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            Button btnTomarFoto = findViewById(R.id.btnTomarFoto);
            verificarFotoDesdeServidor(btnTomarFoto);
            subirFotoAlServidor();
        } else if (requestCode == REQUEST_CHECK_SETTINGS) {
            // El usuario volvió del diálogo de activar ubicación
            if (resultCode == RESULT_OK) {
                // Reintentar la acción pendiente si existe
                if (pendingEstado != null) {
                    obtenerCoordenadaActualForzar((lat, lon) -> {
                        String coord = lat + ", " + lon;
                        actualizarEstadoPedido(pendingEstado, coord);
                        pendingEstado = null;
                    });
                }
            } else {
                Toast.makeText(this, "Para continuar, activa la ubicación", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) {
            return null;
        }
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(columnIndex);
        cursor.close();
        return path;
    }

    private void subirFotoAlServidor() {
        if (selectedImageUri != null) {
            String imagePath = getPathFromUri(selectedImageUri);
            if (imagePath != null) {
                File imageFile = new File(imagePath);

                if (!TextUtils.isEmpty(pedidoId)) {
                    OkHttpClient client = new OkHttpClient();
                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("image", imageFile.getName(), RequestBody.create(MediaType.parse("image/*"), imageFile))
                            .addFormDataPart("id", pedidoId) // Usar el ID del pedido obtenido en mostrarDatosPedido
                            .build();

                    Request request = new Request.Builder()
                           // .url("https://pedidos.grupoascencio.com.mx/Pedidos_GA/App/guardar_foto.php")
                            .url("http://192.168.60.194/Pedidos_GA/App/guardar_foto.php")
                            .post(requestBody)
                            .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            e.printStackTrace();
                            Log.e("Subida Imagen", "Error al subir la imagen: " + e.getMessage());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(DetallePedidoActivity.this, "Error al subir la imagen", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            final String responseData = response.body().string();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    savePhotoStatus(true); // Guardar estado de la foto cargada
                                    Toast.makeText(DetallePedidoActivity.this, responseData, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                } else {
                    Toast.makeText(this, "ID del pedido no válido", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No se pudo obtener la ruta de la imagen", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No se seleccionó ninguna imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogoActualizarEstado() {
        final String[] estados = {"EN RUTA", "CANCELADO", "ENTREGADO", "EN TIENDA", "REPROGRAMADO"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Actualizar Estado")
                .setItems(estados, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String nuevoEstado = estados[which];
                        confirmarCambioDeEstado(nuevoEstado);
                    }
                });
        builder.create().show();
    }

    private void confirmarCambioDeEstado(final String nuevoEstado) {
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this);
        confirmDialog.setTitle("Confirmar Cambio de Estado");
        confirmDialog.setMessage("¿Estás seguro de que deseas cambiar el estado a " + nuevoEstado + "?");
        confirmDialog.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                obtenerCoordenadasParaActualizarEstado(nuevoEstado);
            }
        });
        confirmDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // Cierra el diálogo de confirmación si se elige "No"
            }
        });
        confirmDialog.create().show();
    }


    private void obtenerCoordenadaActual(OnUbicacionObtenidaListener listener) {
        // Verificar permisos antes de intentar obtener la ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Mostrar una explicación si es necesario
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                mostrarExplicacionYSolicitarPermiso();
            } else {
                // Solicitar permisos si no están concedidos
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            }
            return; // Salir del método mientras se esperan los permisos
        }

        // Utilizar Fused Location Provider para obtener la última ubicación
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        // Notificar al listener con las coordenadas
                        listener.onUbicacionObtenida(latitude, longitude);
                    } else {
                        // Manejar el caso en que la ubicación es nula
                        Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Manejar errores
                    Toast.makeText(this, "Error al obtener ubicación: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    public void onUbicacionObtenida(double latitude, double longitude) {
        // Aquí manejas las coordenadas obtenidas, por ejemplo, actualizando la UI
        Toast.makeText(this, "Ubicación obtenida: Latitud = " + latitude + ", Longitud = " + longitude, Toast.LENGTH_SHORT).show();
    }

    // Interfaz para manejar la ubicación obtenida
    public interface OnUbicacionObtenidaListener {
        void onUbicacionObtenida(double latitude, double longitude);
    }






    private void mostrarExplicacionYSolicitarPermiso() {
        // Mostrar una explicación personalizada
        new AlertDialog.Builder(this)
                .setTitle("Permiso de ubicación necesario")
                .setMessage("Esta aplicación necesita acceso a tu ubicación para poder ofrecerte todas sus funciones.")
                .setPositiveButton("Aceptar", (dialog, which) ->
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION))
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    // Acción cuando el usuario cancela
                    dialog.dismiss();
                })
                .create()
                .show();
    }





    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso de almacenamiento concedido
                seleccionarImagenDeGaleria();
            } else {
                // Permiso de almacenamiento denegado
                Toast.makeText(this, "Se necesitan permisos de almacenamiento para seleccionar una imagen.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso de ubicación concedido: obtenemos coordenada y retomamos acción pendiente
                obtenerCoordenadaActual((latitude, longitude) -> {
                    String coordenada = latitude + ", " + longitude;
                    if (pendingEstado != null) {
                        actualizarEstadoPedido(pendingEstado, coordenada);
                        pendingEstado = null;
                    }
                });
            } else {
                // Permiso de ubicación denegado
                Toast.makeText(this, "Se necesitan permisos de ubicación para obtener la ubicación actual.", Toast.LENGTH_SHORT).show();
            }
        }
    }




    /*
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, puedes intentar obtener la ubicación nuevamente
                obtenerCoordenadaActual();
            } else {
                // Permiso denegado, manejar el caso de no tener permiso
                Toast.makeText(this, "Se necesitan permisos de ubicación para continuar", Toast.LENGTH_SHORT).show();
            }
        }
    }*/


    private void obtenerCoordenadasParaActualizarEstado(final String nuevoEstado) {
        // Guardar el estado solicitado por si hay que pedir permisos primero
        pendingEstado = nuevoEstado;
        // Intentar obtener la ubicación de manera robusta (forzando una lectura si es necesario)
        obtenerCoordenadaActualForzar(new OnUbicacionObtenidaListener() {
            @Override
            public void onUbicacionObtenida(double latitude, double longitude) {
                String coordenada = latitude + ", " + longitude;
                if (pendingEstado != null) {
                    actualizarEstadoPedido(pendingEstado, coordenada);
                    pendingEstado = null;
                }
            }
        });
    }

    // Variante robusta: si no hay última ubicación, intenta obtener una lectura actual o solicitar una actualización única
    private void obtenerCoordenadaActualForzar(OnUbicacionObtenidaListener listener) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                mostrarExplicacionYSolicitarPermiso();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            }
            return;
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Intentar obtener ubicación actual directa
        com.google.android.gms.tasks.CancellationTokenSource cts = new com.google.android.gms.tasks.CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        listener.onUbicacionObtenida(loc.getLatitude(), loc.getLongitude());
                    } else {
                        solicitarUnSoloUpdateUbicacion(fusedLocationClient, listener);
                    }
                })
                .addOnFailureListener(e -> solicitarUnSoloUpdateUbicacion(fusedLocationClient, listener));
    }

    private void solicitarUnSoloUpdateUbicacion(FusedLocationProviderClient fusedLocationClient, OnUbicacionObtenidaListener listener) {
        com.google.android.gms.location.LocationRequest request = new com.google.android.gms.location.LocationRequest.Builder(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setMaxUpdates(1)
                .build();

        com.google.android.gms.location.SettingsClient settingsClient = com.google.android.gms.location.LocationServices.getSettingsClient(this);
        com.google.android.gms.location.LocationSettingsRequest settingsRequest = new com.google.android.gms.location.LocationSettingsRequest.Builder()
                .addLocationRequest(request)
                .build();

        settingsClient.checkLocationSettings(settingsRequest)
                .addOnSuccessListener(unused -> {
                    com.google.android.gms.location.LocationCallback callback = new com.google.android.gms.location.LocationCallback() {
                        @Override
                        public void onLocationResult(com.google.android.gms.location.LocationResult locationResult) {
                            if (locationResult == null || locationResult.getLastLocation() == null) {
                                Toast.makeText(DetallePedidoActivity.this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            double lat = locationResult.getLastLocation().getLatitude();
                            double lon = locationResult.getLastLocation().getLongitude();
                            listener.onUbicacionObtenida(lat, lon);
                            fusedLocationClient.removeLocationUpdates(this);
                        }
                    };
                    fusedLocationClient.requestLocationUpdates(request, callback, getMainLooper());
                })
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.android.gms.common.api.ResolvableApiException) {
                        try {
                            ((com.google.android.gms.common.api.ResolvableApiException) e).startResolutionForResult(DetallePedidoActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (Exception ex) {
                            Toast.makeText(DetallePedidoActivity.this, "Activa la ubicación para continuar", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(DetallePedidoActivity.this, "Activa la ubicación para continuar", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void actualizarEstadoPedido(String nuevoEstado, String coordenada) {
        if (!TextUtils.isEmpty(pedidoId)) {
            OkHttpClient client = new OkHttpClient();
            RequestBody formBody = new FormBody.Builder()
                    .add("id", pedidoId)
                    .add("estado", nuevoEstado)
                    .add("coordenada", coordenada) // Agregar la coordenada aquí
                    .build();

            Request request = new Request.Builder()
                 //   .url("https://pedidos.grupoascencio.com.mx/Pedidos_GA/App/actualizar_estado.php")
                    .url("http://192.168.60.194/Pedidos_GA/App/actualizar_estado.php")
                    .post(formBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    Log.e("Actualizar Estado", "Error al actualizar el estado: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DetallePedidoActivity.this, "Error al actualizar el estado", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String responseData = response.body().string();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DetallePedidoActivity.this, responseData, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } else {
            Toast.makeText(this, "ID del pedido no válido", Toast.LENGTH_SHORT).show();
        }
    }

}
