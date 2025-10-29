package com.example.app_pedidos.ui.Login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.app_pedidos.MainActivity;
import com.example.app_pedidos.R;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword;
    private Button buttonLogin;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Forzar tema claro en toda la app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.fragment_login);

        // Referencias a los elementos de la interfaz
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);

        // Acción cuando se hace clic en el botón de inicio de sesión
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Obtener el nombre de usuario y la contraseña ingresados
                String username = editTextUsername.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();

                // Realizar la solicitud HTTP para la autenticación
                autenticarUsuario(username, password);
            }
        });
    }

    // Método para autenticar al usuario utilizando la API PHP
    private void autenticarUsuario(final String username, String password) {
        // URL de tu API PHP de autenticación
        ///String url = "https://pedidos.grupoascencio.com.mx/Pedidos_GA/App/login.php";
        String url = "http://192.168.60.194/Pedidos_GA/App/login.php";

        // Realizar una solicitud HTTP POST usando Volley
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Verificar la respuesta de la API
                        if (response.trim().equals("OK")) {
                            // Guardar el nombre de usuario en SharedPreferences
                            guardarUsuarioEnSharedPreferences(username);
                            // Iniciar MainActivity si la autenticación es exitosa
                            iniciarMainActivity();
                        } else {
                            // Mostrar un mensaje de error si la autenticación falla
                            Toast.makeText(LoginActivity.this, "Nombre de usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Mostrar un mensaje de error si hay un error de conexión
                        Toast.makeText(LoginActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                }) {
            // Parámetros POST
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("password", password);
                return params;
            }
        };

        // Agregar la solicitud a la cola de Volley
        Volley.newRequestQueue(this).add(stringRequest);
    }

    // Método para guardar el nombre de usuario en SharedPreferences
    private void guardarUsuarioEnSharedPreferences(String username) {
        SharedPreferences sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.apply();
    }

    // Método para iniciar MainActivity después de un inicio de sesión exitoso
    private void iniciarMainActivity() {
        // Mostrar un mensaje de éxito
        Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();

        // Crear un intent para iniciar MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("USERNAME", username);

        // Iniciar MainActivity
        startActivity(intent);

        // Finalizar LoginActivity para que no se pueda volver atrás
        finish();
    }
}
