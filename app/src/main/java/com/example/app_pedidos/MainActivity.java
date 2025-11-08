package com.example.app_pedidos;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.app_pedidos.databinding.ActivityMainBinding;
import com.example.app_pedidos.ApiConfig;
import com.example.app_pedidos.ui.Login.LoginActivity;
import com.google.android.material.navigation.NavigationView;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import android.widget.EditText;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements ConexionPHP.PedidoListener {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private String username;
    private TextView navHeaderSubtitle;
    private ImageView toolbarLogo;
    private Integer lastKmFinal = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflar el layout principal y establecer el content view
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configurar la Toolbar
        setSupportActionBar(binding.appBarMain.toolbar);
        toolbarLogo = binding.appBarMain.toolbar.findViewById(R.id.toolbar_logo);

        // Configurar el Navigation Drawer
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_est, R.id.nav_hist, R.id.nav_vehicle, R.id.detallePedidoActivity)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Cambiar la imagen de la Toolbar en funciÃ³n del fragmento
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();

            if (destinationId == R.id.nav_home) {
                toolbarLogo.setImageResource(R.drawable.pedprobl);
            } else if (destinationId == R.id.nav_est) {
                toolbarLogo.setImageResource(R.drawable.estbl);
            } else if (destinationId == R.id.nav_hist) {
                toolbarLogo.setImageResource(R.drawable.histbl);
            } else if (destinationId == R.id.nav_vehicle) {
                toolbarLogo.setImageResource(R.drawable.gabl); // Logo para vehÃ­culo
            }  else {
                toolbarLogo.setImageResource(R.drawable.gabl); // Imagen por defecto
            }
        });


        SharedPreferences sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        username = sharedPreferences.getString("username", "");

        // Mostrar el nombre de usuario en el subtÃ­tulo del encabezado de navegaciÃ³n
        navHeaderSubtitle = navigationView.getHeaderView(0).findViewById(R.id.NombreLogin);
        navHeaderSubtitle.setText(username);

        // Validar vehÃ­culo asignado al iniciar sesiÃ³n
        verificarVehiculoAsignado(username);

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void verificarVehiculoAsignado(final String username) {
       // final String urlVerificar = "https://pedidos.grupoascencio.com.mx/Pedidos_GA/App/verificar_vehiculo.php";
        final String urlVerificar = ApiConfig.BASE_URL + "/Pedidos_GA/App/verificar_vehiculo.php";
       
        StringRequest request = new StringRequest(Request.Method.POST, urlVerificar,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String body = response == null ? "" : response.trim().toUpperCase();
                        // Solo permitir continuar si la respuesta es EXACTAMENTE "ASIGNADO"
                        if (!"ASIGNADO".equals(body)) {
                            mostrarDialogoSinVehiculo();
                        } else {
                            verificarEstadoKilometraje(username);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Si no se puede verificar, por seguridad impedir continuar
                mostrarDialogoSinVehiculo();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private void verificarEstadoKilometraje(final String username) {
        final String url = ApiConfig.BASE_URL + "/Pedidos_GA/App/estado_kilometraje.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (!obj.optBoolean("ok", false)) return;
                        boolean assigned = obj.optBoolean("assigned", false);
                        boolean needs = obj.optBoolean("needs_km", false);
                        // Guardar el ltimo km del vehculo si viene
                        if (!obj.isNull("Km_Total")) {
                            try { lastKmFinal = obj.getInt("Km_Total"); } catch (Exception ignore) { lastKmFinal = null; }
                        } else {
                            lastKmFinal = null;
                        }
                        if (assigned && needs) {
                            mostrarDialogoCapturaKilometraje(username, lastKmFinal);
                        }
                    } catch (Exception ignored) { }
                },
                error -> { /* ignorar */ }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("username", username);
                return p;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    private void mostrarDialogoCapturaKilometraje(final String username) {
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Kilometraje actual");

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_Pedidos_MaterialAlertDialog)
                .setTitle("Captura de kilometraje semanal")
                .setMessage("Es necesario registrar el kilometraje semanal. Ingresa el parametro actual del vehÃ­culo.")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Guardar", (d, w) -> {
                    String val = input.getText().toString().trim();
                    if (val.isEmpty()) {
                        Toast.makeText(this, "Ingresa un valor", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    registrarKilometraje(username, val);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void registrarKilometraje(final String username, final String km) {
        final String url = ApiConfig.BASE_URL + "/Pedidos_GA/App/registrar_kilometraje.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.optBoolean("ok", false)) {
                            com.example.app_pedidos.ui.common.Notifier.success(this, "Kilometraje registrado");
                        } else {
                            com.example.app_pedidos.ui.common.Notifier.error(this, "No se pudo registrar");
                        }
                    } catch (Exception e) {
                        com.example.app_pedidos.ui.common.Notifier.error(this, "Error al guardar");
                    }
                },
                error -> Toast.makeText(this, "Error de conexiÃ³n", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("username", username);
                p.put("km", km);
                return p;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    private void mostrarDialogoCapturaKilometraje(final String username, final Integer kmActual) {
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Kilometraje a registrar");

        String kmTexto = (kmActual == null) ? "No disponible" : String.valueOf(kmActual);

        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_Pedidos_MaterialAlertDialog)
                .setTitle("Captura de kilometraje semanal")
                .setMessage("Es necesario registrar el kilometraje semanal.\nKilometraje actual del veh\u00edculo: " + kmTexto)
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Guardar", null)
                .create();
        dialog.setCanceledOnTouchOutside(false);

        dialog.setOnShowListener(dlg -> {
            android.widget.Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btn.setOnClickListener(v -> {
                String val = input.getText().toString().trim();
                if (val.isEmpty()) {
                    input.setError("Ingresa un valor");
                    return;
                }
                int ingresado;
                try {
                    ingresado = Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    input.setError("Valor inv\u00e1lido");
                    return;
                }
                if (kmActual != null && ingresado < kmActual) {
                    input.setError("No puede ser menor a " + kmActual);
                    Toast.makeText(this, "No puedes ingresar un kilometraje menor al actual (" + kmActual + ")", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
                registrarKilometrajeForzado(username, val);
            });
        });

        dialog.show();
    }
    private void registrarKilometrajeForzado(final String username, final String km) {
        final String url = ApiConfig.BASE_URL + "/Pedidos_GA/App/registrar_kilometraje.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.optBoolean("ok", false)) {
                            com.example.app_pedidos.ui.common.Notifier.success(this, "Kilometraje registrado");
                        } else {
                            String msg = obj.optString("error", "No se pudo registrar");
                            com.example.app_pedidos.ui.common.Notifier.error(this, msg);
                            mostrarDialogoCapturaKilometraje(username, lastKmFinal);
                        }
                    } catch (Exception e) {
                        com.example.app_pedidos.ui.common.Notifier.error(this, "Error al guardar");
                        mostrarDialogoCapturaKilometraje(username, lastKmFinal);
                    }
                },
                error -> {
                    String mensaje = "Error de conexi\u00f3n";
                    if (error != null && error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String body = new String(error.networkResponse.data, java.nio.charset.StandardCharsets.UTF_8);
                            JSONObject j = new JSONObject(body);
                            String srv = j.optString("error", "");
                            if (!srv.isEmpty()) mensaje = srv;
                        } catch (Exception ignore) { }
                    }
                    com.example.app_pedidos.ui.common.Notifier.error(this, mensaje);
                    mostrarDialogoCapturaKilometraje(username, lastKmFinal);
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("username", username);
                p.put("km", km);
                return p;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    private void mostrarDialogoSinVehiculo() {
        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_Pedidos_MaterialAlertDialog)
                .setTitle("VehÃ­culo no asignado")
                .setMessage("Solicita a tu Jefe de choferes de Sucursal que te asigne un vehÃ­culo para continuar")
                .setCancelable(false)
                .setPositiveButton("Cerrar sesiÃ³n", (d, which) -> cerrarSesion())
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            cerrarSesion();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void cerrarSesion() {
        Log.d("CerrarSesion", "Cerrando sesiÃ³n...");

        // Eliminar el nombre de usuario de SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("username");
        editor.apply();

        // Mostrar un mensaje de Ã©xito
        Toast.makeText(this, "SesiÃ³n cerrada exitosamente", Toast.LENGTH_SHORT).show();

        // Iniciar la actividad de inicio de sesiÃ³n y cerrar esta actividad
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish(); // Esta lÃ­nea deberÃ­a cerrar la actividad actual
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onPedidoObtenido(String jsonResponse) {
        
        runOnUiThread(() -> {
            
        });
    }

    @Override
    public void onPedidoError() {
        
        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al obtener los pedidos", Toast.LENGTH_SHORT).show());
    }
}
