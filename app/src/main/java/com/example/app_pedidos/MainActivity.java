package com.example.app_pedidos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AlertDialog;
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
                R.id.nav_home, R.id.nav_est, R.id.nav_hist, R.id.detallePedidoActivity)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Cambiar la imagen de la Toolbar en función del fragmento
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();

            if (destinationId == R.id.nav_home) {
                toolbarLogo.setImageResource(R.drawable.pedprobl);
            } else if (destinationId == R.id.nav_est) {
                toolbarLogo.setImageResource(R.drawable.estbl);
            } else if (destinationId == R.id.nav_hist) {
                toolbarLogo.setImageResource(R.drawable.histbl);
            }  else {
                toolbarLogo.setImageResource(R.drawable.gabl); // Imagen por defecto
            }
        });


        SharedPreferences sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        username = sharedPreferences.getString("username", "");

        // Mostrar el nombre de usuario en el subtítulo del encabezado de navegación
        navHeaderSubtitle = navigationView.getHeaderView(0).findViewById(R.id.NombreLogin);
        navHeaderSubtitle.setText(username);

        // Validar vehículo asignado al iniciar sesión
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
                        if (assigned && needs) {
                            mostrarDialogoCapturaKilometraje(username);
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

        new AlertDialog.Builder(this)
                .setTitle("Captura de kilometraje")
                .setMessage("No hay registro de los últimos 3 días. Ingresa el odómetro actual.")
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
                            Toast.makeText(this, "Kilometraje registrado", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "No se pudo registrar", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show()) {
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
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Vehículo no asignado")
                .setMessage("Solicita a tu Jefe de choferes de Sucursal que te asigne un vehículo para continuar")
                .setCancelable(false)
                .setPositiveButton("Cerrar sesión", (d, which) -> cerrarSesion())
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
        Log.d("CerrarSesion", "Cerrando sesión...");

        // Eliminar el nombre de usuario de SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("username");
        editor.apply();

        // Mostrar un mensaje de éxito
        Toast.makeText(this, "Sesión cerrada exitosamente", Toast.LENGTH_SHORT).show();

        // Iniciar la actividad de inicio de sesión y cerrar esta actividad
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish(); // Esta línea debería cerrar la actividad actual
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onPedidoObtenido(String jsonResponse) {
        // Procesa la respuesta JSON
        // Por ejemplo, aquí puedes actualizar la interfaz de usuario
        runOnUiThread(() -> {
            // Actualiza la interfaz de usuario según los pedidos recibidos
            // Por ejemplo:
            // textView.setText(jsonResponse);
        });
    }

    @Override
    public void onPedidoError() {
        // Maneja el error
        // Por ejemplo, muestra un mensaje de error
        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al obtener los pedidos", Toast.LENGTH_SHORT).show());
    }
}
