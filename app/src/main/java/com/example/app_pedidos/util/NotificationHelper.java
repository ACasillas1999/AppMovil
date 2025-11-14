package com.example.app_pedidos.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.app_pedidos.MainActivity;
import com.example.app_pedidos.R;
import com.example.app_pedidos.ui.Pedido.DetallePedidoActivity;

import org.json.JSONObject;

public class NotificationHelper {
    public static final String CHANNEL_ID = "pedidos_estado";

    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Cambios de pedidos",
                    NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription("Notificaciones sobre pedidos en ruta");
            ch.enableLights(true);
            ch.enableVibration(true);
            ch.setLightColor(Color.BLUE);
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    public static void notifyEnRuta(Context ctx, int idPedido, String factura, String cliente) {
        ensureChannel(ctx);
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return;
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                ctx, idPedido, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        String title = "Pedido en ruta" + (factura != null && !factura.isEmpty() ? (" • " + factura) : "");
        String text = (cliente == null || cliente.isEmpty()) ? "Tu pedido comenzó el recorrido" : cliente;

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.mipmap.iconoappna)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);
        NotificationManagerCompat.from(ctx).notify(10000 + idPedido, b.build());
    }

    public static void notifyEnRuta(Context ctx, JSONObject pedido) {
        try {
            ensureChannel(ctx);
            if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return;

            int id = safeInt(pedido.optString("ID", "0"));
            String factura = pedido.optString("FACTURA", "");
            String cliente = pedido.optString("NOMBRE_CLIENTE", "");

            Intent intent;
            org.json.JSONObject g = pedido.optJSONObject("grupo");
            if (g != null && g.optInt("id", 0) > 0) {
                intent = new Intent(ctx, com.example.app_pedidos.ui.Pedido.GrupoRutaActivity.class);
                intent.putExtra("GRUPO_ID", g.optInt("id", 0));
                intent.putExtra("GRUPO_NOMBRE", g.optString("nombre", ""));
                intent.putExtra("OPEN_MAP", true);
            } else {
                intent = new Intent(ctx, DetallePedidoActivity.class);
                // Pasar los mismos extras que usa HomeFragment
                intent.putExtra("ID", pedido.optString("ID", ""));
                intent.putExtra("SUCURSAL", pedido.optString("SUCURSAL", ""));
                intent.putExtra("NOMBRE_CLIENTE", pedido.optString("NOMBRE_CLIENTE", ""));
                intent.putExtra("ESTADO", pedido.optString("ESTADO", ""));
                intent.putExtra("FECHA_RECEPCION_FACTURA", pedido.optString("FECHA_RECEPCION_FACTURA", ""));
                intent.putExtra("FECHA_ENTREGA_CLIENTE", pedido.optString("FECHA_ENTREGA_CLIENTE", ""));
                intent.putExtra("CHOFER_ASIGNADO", pedido.optString("CHOFER_ASIGNADO", ""));
                intent.putExtra("VENDEDOR", pedido.optString("VENDEDOR", ""));
                intent.putExtra("FACTURA", pedido.optString("FACTURA", ""));
                intent.putExtra("DIRECCION", pedido.optString("DIRECCION", ""));
                intent.putExtra("FECHA_MIN_ENTREGA", pedido.optString("FECHA_MIN_ENTREGA", ""));
                intent.putExtra("FECHA_MAX_ENTREGA", pedido.optString("FECHA_MAX_ENTREGA", ""));
                intent.putExtra("MIN_VENTANA_HORARIA_1", pedido.optString("MIN_VENTANA_HORARIA_1", ""));
                intent.putExtra("MAX_VENTANA_HORARIA_1", pedido.optString("MAX_VENTANA_HORARIA_1", ""));
                intent.putExtra("TELEFONO", pedido.optString("TELEFONO", ""));
                intent.putExtra("CONTACTO", pedido.optString("CONTACTO", ""));
                intent.putExtra("COMENTARIOS", pedido.optString("COMENTARIOS", ""));
                intent.putExtra("Ruta", pedido.optString("Ruta", ""));
                intent.putExtra("Coord_Origen", pedido.optString("Coord_Origen", ""));
                intent.putExtra("Coord_Destino", pedido.optString("Coord_Destino", ""));
            }

            PendingIntent pi = PendingIntent.getActivity(
                    ctx, id,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            String title = "Pedido en ruta" + (factura != null && !factura.isEmpty() ? (" • " + factura) : "");
            String text = (cliente == null || cliente.isEmpty()) ? "Tu pedido comenzó el recorrido" : cliente;

            NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.iconoappna)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(false)
                    .setContentIntent(pi);
            NotificationManagerCompat.from(ctx).notify(20000 + id, b.build());
        } catch (Exception ignored) { }
    }

    private static int safeInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    public static void cancelForPedido(Context ctx, int idPedido) {
        try {
            NotificationManagerCompat.from(ctx).cancel(20000 + idPedido);
            NotificationManagerCompat.from(ctx).cancel(10000 + idPedido);
        } catch (Exception ignored) {}
    }

    public static void notifyEstado(Context ctx, JSONObject pedido) {
        try {
            ensureChannel(ctx);
            if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return;
            int id = safeInt(pedido.optString("ID", "0"));
            String factura = pedido.optString("FACTURA", "");
            String cliente = pedido.optString("NOMBRE_CLIENTE", "");
            String estado = pedido.optString("ESTADO", "");

            Intent intent;
            JSONObject g = pedido.optJSONObject("grupo");
            if (g != null && g.optInt("id", 0) > 0) {
                intent = new Intent(ctx, com.example.app_pedidos.ui.Pedido.GrupoRutaActivity.class);
                intent.putExtra("GRUPO_ID", g.optInt("id", 0));
                intent.putExtra("GRUPO_NOMBRE", g.optString("nombre", ""));
                intent.putExtra("OPEN_MAP", true);
            } else {
                intent = new Intent(ctx, DetallePedidoActivity.class);
                // Reutiliza el builder de extras de notifyEnRuta
                intent.putExtra("ID", pedido.optString("ID", ""));
                intent.putExtra("SUCURSAL", pedido.optString("SUCURSAL", ""));
                intent.putExtra("NOMBRE_CLIENTE", pedido.optString("NOMBRE_CLIENTE", ""));
                intent.putExtra("ESTADO", pedido.optString("ESTADO", ""));
                intent.putExtra("FECHA_RECEPCION_FACTURA", pedido.optString("FECHA_RECEPCION_FACTURA", ""));
                intent.putExtra("FECHA_ENTREGA_CLIENTE", pedido.optString("FECHA_ENTREGA_CLIENTE", ""));
                intent.putExtra("CHOFER_ASIGNADO", pedido.optString("CHOFER_ASIGNADO", ""));
                intent.putExtra("VENDEDOR", pedido.optString("VENDEDOR", ""));
                intent.putExtra("FACTURA", pedido.optString("FACTURA", ""));
                intent.putExtra("DIRECCION", pedido.optString("DIRECCION", ""));
                intent.putExtra("FECHA_MIN_ENTREGA", pedido.optString("FECHA_MIN_ENTREGA", ""));
                intent.putExtra("FECHA_MAX_ENTREGA", pedido.optString("FECHA_MAX_ENTREGA", ""));
                intent.putExtra("MIN_VENTANA_HORARIA_1", pedido.optString("MIN_VENTANA_HORARIA_1", ""));
                intent.putExtra("MAX_VENTANA_HORARIA_1", pedido.optString("MAX_VENTANA_HORARIA_1", ""));
                intent.putExtra("TELEFONO", pedido.optString("TELEFONO", ""));
                intent.putExtra("CONTACTO", pedido.optString("CONTACTO", ""));
                intent.putExtra("COMENTARIOS", pedido.optString("COMENTARIOS", ""));
                intent.putExtra("Ruta", pedido.optString("Ruta", ""));
                intent.putExtra("Coord_Origen", pedido.optString("Coord_Origen", ""));
                intent.putExtra("Coord_Destino", pedido.optString("Coord_Destino", ""));
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(
                    ctx, id,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            String title = "Pedido " + estado + (factura != null && !factura.isEmpty() ? (" • " + factura) : "");
            String text = (cliente == null || cliente.isEmpty()) ? "Actualización de estado" : cliente;
            NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.iconoappna)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(false)
                    .setContentIntent(pi);
            NotificationManagerCompat.from(ctx).notify(20000 + id, b.build());
        } catch (Exception ignored) {}
    }
}
