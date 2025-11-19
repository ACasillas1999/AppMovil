package com.example.app_pedidos;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.Locale;

public class UpdateManager {

    private static final String CHECK_UPDATE_PATH = "/Pedidos_GA/App/check_update.php";
    private static Long lastDownloadId = null;
    private static BroadcastReceiver downloadReceiver = null;

    public static void checkForUpdate(Activity activity) {
        String url = ApiConfig.BASE_URL + CHECK_UPDATE_PATH;
        Log.d("UpdateManager", "checkForUpdate URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d("UpdateManager", "Respuesta de actualización: " + response.toString());
                    handleUpdateResponse(activity, response);
                },
                error -> {
                    Log.e("UpdateManager", "Error al verificar actualización", error);
                    Toast.makeText(activity, "Error al verificar actualización", Toast.LENGTH_SHORT).show();
                }
        );

        Volley.newRequestQueue(activity.getApplicationContext()).add(request);
    }

    private static void handleUpdateResponse(Activity activity, JSONObject response) {
        int currentCode = 1;
        try {
            currentCode = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("UpdateManager", "No se pudo obtener versionCode actual", e);
        }
        int serverCode = response.optInt("versionCode", currentCode);
        Log.d("UpdateManager", "currentCode=" + currentCode + " serverCode=" + serverCode);
        if (serverCode <= currentCode) {
            Log.d("UpdateManager", "No hay actualización disponible");
            return;
        }

        String versionName = response.optString("versionName", "");
        String changelog = response.optString("changelog", "");
        String apkUrl = response.optString("apkUrl", "");
        if (apkUrl == null || apkUrl.trim().isEmpty()) {
            Log.w("UpdateManager", "apkUrl vacío en respuesta de actualización");
            return;
        }

        String message;
        if (!changelog.isEmpty()) {
            message = String.format(Locale.getDefault(),
                    "Nueva versión %s disponible.\n\nCambios:\n%s\n\n¿Deseas actualizar ahora?",
                    versionName.isEmpty() ? String.valueOf(serverCode) : versionName,
                    changelog);
        } else {
            message = String.format(Locale.getDefault(),
                    "Hay una nueva versión (%s) disponible.\n¿Deseas actualizar ahora?",
                    versionName.isEmpty() ? String.valueOf(serverCode) : versionName);
        }

        new AlertDialog.Builder(activity)
                .setTitle("Actualización disponible")
                .setMessage(message)
                .setPositiveButton("Actualizar", (d, w) -> startDownload(activity, apkUrl))
                .setNegativeButton("Más tarde", null)
                .show();
    }

    private static void startDownload(Activity activity, String apkUrl) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setTitle("Descargando actualización");
        request.setDescription("Actualización de App Pedidos");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "app_pedidos_update.apk");
        request.setMimeType("application/vnd.android.package-archive");

        DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null) return;

        lastDownloadId = manager.enqueue(request);
        registerReceiver(activity, manager);
    }

    private static void registerReceiver(Activity activity, DownloadManager manager) {
        if (downloadReceiver != null) return;

        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
                if (lastDownloadId == null || id != lastDownloadId) return;

                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(id);
                Cursor cursor = null;
                try {
                    cursor = manager.query(query);
                    if (cursor != null && cursor.moveToFirst()) {
                        int statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (statusIdx != -1 && cursor.getInt(statusIdx) == DownloadManager.STATUS_SUCCESSFUL) {
                            Uri uri = manager.getUriForDownloadedFile(id);
                            if (uri != null) installApk(context, uri);
                        }
                    }
                } finally {
                    if (cursor != null) cursor.close();
                }
            }
        };

        activity.registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private static void installApk(Context context, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }

    public static void unregister(Activity activity) {
        if (downloadReceiver != null) {
            try {
                activity.unregisterReceiver(downloadReceiver);
            } catch (IllegalArgumentException ignored) { }
            downloadReceiver = null;
        }
    }
}
