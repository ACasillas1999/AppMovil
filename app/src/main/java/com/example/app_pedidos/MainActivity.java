package com.example.app_pedidos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.app_pedidos.databinding.ActivityMainBinding;
import com.example.app_pedidos.network.Utf8JsonObjectRequest;
import com.example.app_pedidos.ui.Login.LoginActivity;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ConexionPHP.PedidoListener {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private String username;
    private TextView navHeaderSubtitle;
    private ImageView toolbarLogo;
    private Integer lastKmFinal = null;
    private boolean inspeccionLanzada = false;
    private static final int REQ_INSPECCION = 1001;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        toolbarLogo = binding.appBarMain.toolbar.findViewById(R.id.toolbar_logo);

        DrawerLayout drawer = binding.drawerLayout;
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_est, R.id.nav_hist, R.id.nav_vehicle, R.id.detallePedidoActivity)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Navegar a un destino solicitado externamente (por ejemplo, desde InspeccionHoyActivity)
        int openDest = getIntent().getIntExtra("OPEN_DEST", -1);
        if (openDest != -1) {
            try { navController.navigate(openDest); } catch (Exception ignored) {}
        }

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();
            if (destinationId == R.id.nav_home) toolbarLogo.setImageResource(R.drawable.pedprobl);
            else if (destinationId == R.id.nav_est) toolbarLogo.setImageResource(R.drawable.estbl);
            else if (destinationId == R.id.nav_hist) toolbarLogo.setImageResource(R.drawable.histbl);
            else if (destinationId == R.id.nav_vehicle) toolbarLogo.setImageResource(R.drawable.gabl);
            else toolbarLogo.setImageResource(R.drawable.gabl);
        });

        SharedPreferences sp = getSharedPreferences("login_prefs", MODE_PRIVATE);
        username = sp.getString("username", "");
        navHeaderSubtitle = navigationView.getHeaderView(0).findViewById(R.id.NombreLogin);
        navHeaderSubtitle.setText(username);

        verificarVehiculoAsignado(username);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (username != null && !username.isEmpty()) {
            verificarCambioVehiculoLigero(username);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void verificarVehiculoAsignado(final String username) {
        final String urlVerificar = ApiConfig.BASE_URL + "/Pedidos_GA/App/verificar_vehiculo.php";
        StringRequest request = new StringRequest(Request.Method.POST, urlVerificar,
                response -> {
                    String body = response == null ? "" : response.trim().toUpperCase();
                    if (!"ASIGNADO".equals(body)) {
                        mostrarDialogoSinVehiculo();
                    } else {
                        verificarEstadoKilometraje(username);
                    }
                }, error -> mostrarDialogoSinVehiculo()) {
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
                        int idVeh = obj.optInt("id_vehiculo", -1);
                        if (assigned && idVeh > 0) {
                            if (detectaCambioVehiculo(idVeh)) {
                                mostrarDialogoCambioVehiculoYSalir();
                                return;
                            } else {
                                guardarVehiculoAsignado(idVeh);
                            }
                        }
                        if (!obj.isNull("Km_Total")) {
                            try { lastKmFinal = obj.getInt("Km_Total"); } catch (Exception ignore) { lastKmFinal = null; }
                        } else { lastKmFinal = null; }
                        if (assigned && needs) {
                            mostrarDialogoCapturaKilometraje(username, lastKmFinal);
                        } else if (assigned) {
                            verificarInspeccionHoyYQuizasLanzar(lastKmFinal);
                        }
                    } catch (Exception ignored) { }
                }, error -> {}) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("username", username);
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private void mostrarDialogoCapturaKilometraje(final String username, @Nullable final Integer kmActual) {
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Kilometraje a registrar");
        String kmTexto = (kmActual == null) ? "No disponible" : String.valueOf(kmActual);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Captura de kilometraje diario")
                .setMessage("Es necesario registrar el kilometraje del día.\nKilometraje actual del vehículo: " + kmTexto)
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Guardar", null)
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(dlg -> {
            android.widget.Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btn.setOnClickListener(v -> {
                String val = input.getText().toString().trim();
                if (val.isEmpty()) { input.setError("Ingresa un valor"); return; }
                int ingresado;
                try { ingresado = Integer.parseInt(val); } catch (NumberFormatException e) { input.setError("Valor inválido"); return; }
                if (kmActual != null && ingresado < kmActual) { input.setError("No puede ser menor a " + kmActual); return; }
                dialog.dismiss();
                registrarKilometrajeForzado(username, val);
            });
        });
        dialog.show();
    }

    private void registrarKilometraje(final String username, final String km) {
        final String url = ApiConfig.BASE_URL + "/Pedidos_GA/App/registrar_kilometraje.php";
        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.optBoolean("ok", false)) {
                            com.example.app_pedidos.ui.common.Notifier.success(this, "Kilometraje registrado");
                            try { verificarInspeccionHoyYQuizasLanzar(Integer.parseInt(km)); } catch (Exception ignore) { verificarInspeccionHoyYQuizasLanzar(null); }
                        } else { com.example.app_pedidos.ui.common.Notifier.error(this, "No se pudo registrar"); }
                    } catch (Exception e) { com.example.app_pedidos.ui.common.Notifier.error(this, "Error al guardar"); }
                }, error -> Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show()) {
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

    private void registrarKilometrajeForzado(final String username, final String km) {
        final String url = ApiConfig.BASE_URL + "/Pedidos_GA/App/registrar_kilometraje.php";
        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.optBoolean("ok", false)) {
                            com.example.app_pedidos.ui.common.Notifier.success(this, "Kilometraje registrado");
                            try { verificarInspeccionHoyYQuizasLanzar(Integer.parseInt(km)); } catch (Exception ignore) { verificarInspeccionHoyYQuizasLanzar(null); }
                        } else {
                            String msg = obj.optString("error", "No se pudo registrar");
                            com.example.app_pedidos.ui.common.Notifier.error(this, msg);
                            mostrarDialogoCapturaKilometraje(username, lastKmFinal);
                        }
                    } catch (Exception e) {
                        com.example.app_pedidos.ui.common.Notifier.error(this, "Error al guardar");
                        mostrarDialogoCapturaKilometraje(username, lastKmFinal);
                    }
                }, error -> {
                    com.example.app_pedidos.ui.common.Notifier.error(this, "Error de conexión");
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

    private boolean isAfterEightExceptSunday() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int day = cal.get(java.util.Calendar.DAY_OF_WEEK);
        boolean isSunday = (day == java.util.Calendar.SUNDAY);
        if (isSunday) return false;
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        return hour >= 8;
    }

    private void lanzarInspeccion(@Nullable Integer km) {
        if (inspeccionLanzada) return;
        inspeccionLanzada = true;
        Intent i = new Intent(this, com.example.app_pedidos.ui.Inspeccion.InspeccionVehicularActivity.class);
        if (km != null) i.putExtra(com.example.app_pedidos.ui.Inspeccion.InspeccionVehicularActivity.EXTRA_KILOMETRAJE, km);
        startActivityForResult(i, REQ_INSPECCION);
    }

    private void verificarInspeccionHoyYQuizasLanzar(@Nullable Integer km) {
        if (inspeccionLanzada) return;
        // Solo exigir inspección diario a partir de las 08:00, excepto domingos
        if (!isAfterEightExceptSunday()) { desbloquearUsoApp(); return; }
        String url = ApiConfig.BASE_URL + "/Pedidos_GA/App/obtener_checklist_hoy.php?username=" + android.net.Uri.encode(username);
        Utf8JsonObjectRequest req = new Utf8JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    boolean ok = response.optBoolean("ok", false);
                    if (!ok) {
                        String err = response.optString("error", "");
                        if ("SIN_DATOS".equalsIgnoreCase(err)) {
                            lanzarInspeccion(km);
                        } else {
                            mostrarDialogoInspeccionObligatoria("No se pudo verificar la inspección de hoy.", () -> verificarInspeccionHoyYQuizasLanzar(km));
                        }
                    } else {
                        desbloquearUsoApp();
                    }
                }, error -> mostrarDialogoInspeccionObligatoria("Error de conexión al verificar inspección.", () -> verificarInspeccionHoyYQuizasLanzar(km))
        );
        Volley.newRequestQueue(this).add(req);
    }

    private void mostrarDialogoInspeccionObligatoria(String mensaje, Runnable reintentar) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Inspección requerida")
                .setMessage(mensaje + "\nNo puedes continuar hasta verificar o completar la inspección de hoy.")
                .setCancelable(false)
                .setPositiveButton("Reintentar", (d, w) -> reintentar.run())
                .setNegativeButton("Cerrar sesión", (d, w) -> cerrarSesion())
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void desbloquearUsoApp() {
        if (binding != null && binding.drawerLayout != null) {
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_INSPECCION) {
            inspeccionLanzada = false;
            if (resultCode == RESULT_OK) {
                verificarInspeccionHoyYQuizasLanzar(lastKmFinal);
            } else {
                mostrarDialogoInspeccionObligatoria("Debes completar la inspección del día.", () -> lanzarInspeccion(lastKmFinal));
            }
        }
    }

    private void verificarCambioVehiculoLigero(final String username) {
        final String url = ApiConfig.BASE_URL + "/Pedidos_GA/App/estado_kilometraje.php";
        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (!obj.optBoolean("ok", false)) return;
                        boolean assigned = obj.optBoolean("assigned", false);
                        int idVeh = obj.optInt("id_vehiculo", -1);
                        if (assigned && idVeh > 0 && detectaCambioVehiculo(idVeh)) {
                            mostrarDialogoCambioVehiculoYSalir();
                        }
                    } catch (Exception ignored) { }
                }, error -> {}) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("username", username);
                return p;
            }
        };
        Volley.newRequestQueue(this).add(req);
    }

    private boolean detectaCambioVehiculo(int nuevoId) {
        SharedPreferences sp = getSharedPreferences("login_prefs", MODE_PRIVATE);
        int actual = sp.getInt("vehiculo_id_asignado", -1);
        return (actual != -1 && actual != nuevoId);
    }

    private void guardarVehiculoAsignado(int idVehiculo) {
        SharedPreferences sp = getSharedPreferences("login_prefs", MODE_PRIVATE);
        sp.edit().putInt("vehiculo_id_asignado", idVehiculo).apply();
    }

    private void mostrarDialogoCambioVehiculoYSalir() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Vehículo cambiado")
                .setMessage("Tu vehículo asignado ha cambiado. Debes volver a iniciar sesión para validar el kilometraje e inspección del nuevo vehículo.")
                .setCancelable(false)
                .setPositiveButton("Aceptar", (d, w) -> {
                    SharedPreferences sp = getSharedPreferences("login_prefs", MODE_PRIVATE);
                    sp.edit().putBoolean("vehicle_changed", true).apply();
                    cerrarSesion();
                })
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void mostrarDialogoSinVehiculo() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Vehículo no asignado")
                .setMessage("Solicita a tu Jefe de choferes de Sucursal que te asigne un vehículo para continuar")
                .setCancelable(false)
                .setPositiveButton("Cerrar sesion", (d, which) -> cerrarSesion())
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) { cerrarSesion(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void cerrarSesion() {
        SharedPreferences sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("username");
        editor.remove("vehiculo_id_asignado");
        editor.apply();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onPedidoObtenido(String jsonResponse) {
        runOnUiThread(() -> {});
    }

    @Override
    public void onPedidoError() {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al obtener los pedidos", Toast.LENGTH_SHORT).show());
    }
}
