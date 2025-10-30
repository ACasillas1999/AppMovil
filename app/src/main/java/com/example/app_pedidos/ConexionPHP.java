package com.example.app_pedidos;

import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class ConexionPHP {

   // private static final String URL_PEDIDOS = "https://pedidos.grupoascencio.com.mx/Pedidos_GA/App/Consultar.php";
   private static final String URL_PEDIDOS = ApiConfig.BASE_URL + "/Pedidos_GA/App/Consultar.php";
    

    public static void obtenerPedidos(final PedidoListener listener) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;
                String jsonResponse = null;

                try {
                    URL url = new URL(URL_PEDIDOS);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // Obtener la respuesta del servidor
                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuilder buffer = new StringBuilder();
                    if (inputStream == null) {
                        // No hay datos, devolver null
                        return null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line).append("\n");
                    }

                    if (buffer.length() == 0) {
                        // El stream estaba vacío, devolver null
                        return null;
                    }
                    jsonResponse = buffer.toString();
                } catch (IOException e) {
                    Log.e("ConexionPHP", "Error de conexión", e);
                    // Si hay un error, devolver null
                    return null;
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (final IOException e) {
                            Log.e("ConexionPHP", "Error cerrando el stream", e);
                        }
                    }
                }
                return jsonResponse;
            }

            @Override
            protected void onPostExecute(String jsonResponse) {
                if (jsonResponse != null) {
                    listener.onPedidoObtenido(jsonResponse);
                } else {
                    listener.onPedidoError();
                }
            }
        }.execute();
    }

    public interface PedidoListener {
        void onPedidoObtenido(String jsonResponse);
        void onPedidoError();
    }
}
