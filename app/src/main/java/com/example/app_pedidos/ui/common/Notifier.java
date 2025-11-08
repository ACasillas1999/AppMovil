package com.example.app_pedidos.ui.common;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.example.app_pedidos.R;
import com.google.android.material.snackbar.Snackbar;

public final class Notifier {
    private Notifier() {}

    private static final int COLOR_SUCCESS = Color.parseColor("#2E7D32"); // Green 800
    private static final int COLOR_INFO    = Color.parseColor("#1565C0"); // Blue 800
    private static final int COLOR_WARN    = Color.parseColor("#EF6C00"); // Orange 800
    private static final int COLOR_ERROR   = Color.parseColor("#C62828"); // Red 800

    private static View root(Activity activity) {
        return activity.findViewById(android.R.id.content);
    }

    private static void styleSnackbar(Snackbar snackbar, int bgColor) {
        View sbView = snackbar.getView();
        sbView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
        TextView tv = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (tv != null) {
            tv.setMaxLines(4);
            tv.setTextColor(Color.WHITE);
        }

        // Anchor to bottom navigation if present
        View anchor = sbView.getRootView().findViewById(R.id.nav_view);
        if (anchor != null) {
            snackbar.setAnchorView(anchor);
        }
    }

    private static void show(View anchor, String message, int bgColor, int duration) {
        Snackbar sb = Snackbar.make(anchor, message, duration);
        styleSnackbar(sb, bgColor);
        sb.show();
    }

    public static void success(Activity activity, String message) {
        show(root(activity), message, COLOR_SUCCESS, Snackbar.LENGTH_LONG);
    }

    public static void success(View anchor, String message) {
        show(anchor, message, COLOR_SUCCESS, Snackbar.LENGTH_LONG);
    }

    public static void info(Activity activity, String message) {
        show(root(activity), message, COLOR_INFO, Snackbar.LENGTH_LONG);
    }

    public static void info(View anchor, String message) {
        show(anchor, message, COLOR_INFO, Snackbar.LENGTH_LONG);
    }

    public static void warn(Activity activity, String message) {
        show(root(activity), message, COLOR_WARN, Snackbar.LENGTH_LONG);
    }

    public static void warn(View anchor, String message) {
        show(anchor, message, COLOR_WARN, Snackbar.LENGTH_LONG);
    }

    public static void error(Activity activity, String message) {
        show(root(activity), message, COLOR_ERROR, Snackbar.LENGTH_LONG);
    }

    public static void error(View anchor, String message) {
        show(anchor, message, COLOR_ERROR, Snackbar.LENGTH_LONG);
    }

    public static void connectionLost(Activity activity, String message, String action, Runnable onRetry) {
        View anchor = root(activity);
        Snackbar sb = Snackbar.make(anchor, message, Snackbar.LENGTH_INDEFINITE)
                .setAction(action, v -> { if (onRetry != null) onRetry.run(); });
        styleSnackbar(sb, COLOR_ERROR);
        sb.show();
    }
}

