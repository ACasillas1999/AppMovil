package com.example.app_pedidos.ui.Pedido;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.app_pedidos.ApiConfig;

import com.example.app_pedidos.R;

public class GrupoMapaDialogFragment extends DialogFragment {

    private static final String ARG_URL = "url";
    private static final String ARG_GROUP_ID = "group_id";

    public static GrupoMapaDialogFragment newInstance(String url) {
        GrupoMapaDialogFragment f = new GrupoMapaDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_URL, url);
        f.setArguments(b);
        return f;
    }

    public static GrupoMapaDialogFragment newWithGroupId(int groupId) {
        GrupoMapaDialogFragment f = new GrupoMapaDialogFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_GROUP_ID, groupId);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            Window w = getDialog().getWindow();
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Remove default title
        if (getDialog() != null) getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        return inflater.inflate(R.layout.dialog_grupo_mapa, container, false);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String url = getArguments() != null ? getArguments().getString(ARG_URL, "") : "";
        int groupId = getArguments() != null ? getArguments().getInt(ARG_GROUP_ID, 0) : 0;
        WebView web = view.findViewById(R.id.webGrupoMapa);
        ProgressBar progress = view.findViewById(R.id.progressGrupoMapa);
        ImageButton close = view.findViewById(R.id.btnCloseGrupoMapa);
        ImageButton btnCenter = view.findViewById(R.id.btnCenter);
        ImageButton btnOpenGMaps = view.findViewById(R.id.btnOpenGMaps);
        close.setOnClickListener(v -> dismiss());

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view, String url) { progress.setVisibility(View.GONE); }
        });
        progress.setVisibility(View.VISIBLE);

        btnCenter.setOnClickListener(v -> {
            if (web != null) web.evaluateJavascript("if(window.App&&App.center) App.center();", null);
        });
        final String[] gmapsUrlHolder = new String[]{""};
        btnOpenGMaps.setOnClickListener(v -> {
            String gm = gmapsUrlHolder[0];
            if (gm != null && !gm.isEmpty()) {
                try {
                    startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(gm)));
                } catch (Exception ignored) {}
            }
        });

        if (url != null && !url.isEmpty()) {
            web.loadUrl(url);
            return;
        }

        if (groupId > 0) {
            String api = ApiConfig.BASE_URL + "/Pedidos_GA/App/Consultar.php?grupo_id=" + groupId + "&limit=1000";
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, api, null,
                    response -> {
                        try {
                            // Construir HTML con Mapbox y datos del grupo
                            String token = getString(com.example.app_pedidos.R.string.mapbox_public_token);
                            String html = HtmlBuilder.build(token, response);
                            web.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);

                            // Preparar URL para Google Maps con waypoints
                            gmapsUrlHolder[0] = HtmlBuilder.buildGoogleMapsUrl(response);
                        } catch (Exception e) {
                            progress.setVisibility(View.GONE);
                        }
                    },
                    error -> progress.setVisibility(View.GONE));
            Volley.newRequestQueue(requireContext()).add(req);
        } else {
            progress.setVisibility(View.GONE);
        }
    }
}

class HtmlBuilder {
    static String build(String mapboxToken, org.json.JSONObject response) throws org.json.JSONException {
        org.json.JSONArray arr = response.optJSONArray("pedidos");
        org.json.JSONObject grupo = response.optJSONObject("grupo");
        org.json.JSONArray points = new org.json.JSONArray();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject p = arr.getJSONObject(i);
                // Normalizar coordenadas a {lng,lat}
                double[] lonlat = parseCoord(p.optString("Coord_Destino", ""));
                if (lonlat != null) {
                    org.json.JSONObject o = new org.json.JSONObject();
                    o.put("lng", lonlat[0]);
                    o.put("lat", lonlat[1]);
                    o.put("factura", p.optString("FACTURA", ""));
                    o.put("cliente", p.optString("NOMBRE_CLIENTE", ""));
                    o.put("direccion", p.optString("DIRECCION", ""));
                    o.put("estado", p.optString("ESTADO", ""));
                    points.put(o);
                }
            }
        }

        String color = "#1565C0";
        if (grupo != null && grupo.optString("estado", "").equalsIgnoreCase("Cancelado")) {
            color = "#c82333";
        }

        String dataJson = points.toString();
        String title = grupo != null ? (grupo.optString("nombre", "Ruta") ) : "Ruta";

        String html = "<!DOCTYPE html>"+
                "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"+
                "<style>html,body{margin:0;padding:0;height:100%} #map{position:absolute;top:0;left:0;right:0;bottom:0}</style>"+
                "<link href='https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.css' rel='stylesheet' />"+
                "<script src='https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.js'></script>"+
                "</head><body>"+
                "<div id='map'></div>"+
                "<script>\n"+
                "mapboxgl.accessToken='"+mapboxToken+"';\n"+
                "const points = "+dataJson+";\n"+
                "const defaultColor='"+color+"';\n"+
                "function colorForStatus(s){ s=(s||'').toUpperCase();\n"+
                " if(s==='ACTIVO') return '#1976D2';\n"+
                " if(s==='EN RUTA') return '#EF6C00';\n"+
                " if(s==='REPROGRAMADO') return '#8E24AA';\n"+
                " if(s==='EN TIENDA') return '#FBC02D';\n"+
                " if(s==='ENTREGADO') return '#2E7D32';\n"+
                " if(s==='CANCELADO') return '#C62828';\n"+
                " return defaultColor; }\n"+
                "const map = new mapboxgl.Map({container:'map', style:'mapbox://styles/mapbox/streets-v12', center:[-103.3494,20.6737], zoom:11});\n"+
                "map.on('load', async ()=>{\n"+
                " const bounds = new mapboxgl.LngLatBounds();\n"+
                " const coords=[];\n"+
                " points.forEach((p,i)=>{ if(!isNaN(p.lng)&&!isNaN(p.lat)){\n"+
                "   const el=document.createElement('div'); el.style.cssText='background:'+colorForStatus(p.estado)+';color:#fff;width:28px;height:28px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-weight:bold;border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,.3)'; el.innerHTML=(i+1);\n"+
                "   new mapboxgl.Marker(el).setLngLat([p.lng,p.lat]).setPopup(new mapboxgl.Popup().setHTML('<b>'+ (p.factura||'') +'</b><br/>'+(p.cliente||'')+'<br/>'+(p.direccion||''))).addTo(map);\n"+
                "   bounds.extend([p.lng,p.lat]); coords.push([p.lng,p.lat]);}});\n"+
                " if(coords.length>0){ map.fitBounds(bounds,{padding:40}); }\n"+
                " // Trazo general gris claro (opcional)\n"+
                " if(coords.length>1){\n"+
                "   const limited = coords.slice(0,25);\n"+
                "   const qs = limited.map(c=>c.join(',')).join(';');\n"+
                "   const url = `https://api.mapbox.com/directions/v5/mapbox/driving/${qs}?geometries=geojson&overview=full&access_token=${mapboxgl.accessToken}`;\n"+
                "   try { const r = await fetch(url); const d = await r.json();\n"+
                "     if(d.routes && d.routes.length>0){ const route=d.routes[0].geometry;\n"+
                "       map.addSource('route_bg',{type:'geojson', data:{type:'Feature', geometry:route}});\n"+
                "       map.addLayer({id:'route_bg', type:'line', source:'route_bg', paint:{'line-color':'#B0BEC5','line-width':3,'line-opacity':0.6}}); } } catch(e){}\n"+
                " }\n"+
                " // Segmentos por estado (recta entre puntos consecutivos, coloreada por estado del destino)\n"+
                " for(let i=0;i<points.length-1;i++){ const a=points[i], b=points[i+1];\n"+
                "   const seg = { type:'Feature', geometry:{ type:'LineString', coordinates:[[a.lng,a.lat],[b.lng,b.lat]] } };\n"+
                "   const srcId='leg_'+i; const layerId='leg_'+i;\n"+
                "   if(!isNaN(a.lng)&&!isNaN(a.lat)&&!isNaN(b.lng)&&!isNaN(b.lat)){\n"+
                "     if(map.getSource(srcId)) map.removeSource(srcId); if(map.getLayer(layerId)) map.removeLayer(layerId);\n"+
                "     map.addSource(srcId,{type:'geojson', data:seg});\n"+
                "     map.addLayer({id:layerId, type:'line', source:srcId, paint:{'line-color':colorForStatus(b.estado),'line-width':5,'line-opacity':0.9}});\n"+
                "   }\n"+
                " }\n"+
                " window.App={ center: ()=> map.fitBounds(bounds,{padding:40}) };\n"+
                "});\n"+
                "</script></body></html>";
        return html;
    }

    static String buildGoogleMapsUrl(org.json.JSONObject response) {
        try {
            org.json.JSONArray arr = response.optJSONArray("pedidos");
            java.util.ArrayList<double[]> coords = new java.util.ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject p = arr.getJSONObject(i);
                    double[] lonlat = parseCoord(p.optString("Coord_Destino", ""));
                    if (lonlat != null && !Double.isNaN(lonlat[0]) && !Double.isNaN(lonlat[1])) {
                        // Convert to lat,lng for Google
                        coords.add(new double[]{ lonlat[1], lonlat[0] });
                    }
                }
            }
            if (coords.size() == 0) return "";
            StringBuilder sb = new StringBuilder("https://www.google.com/maps/dir/?api=1&travelmode=driving");
            // Use first as destination if single; for multiple: last as destination, intermediates as waypoints
            if (coords.size() == 1) {
                sb.append("&destination=").append(coords.get(0)[0]).append(",").append(coords.get(0)[1]);
            } else {
                double[] last = coords.get(coords.size()-1);
                sb.append("&destination=").append(last[0]).append(",").append(last[1]);
                java.util.List<String> wps = new java.util.ArrayList<>();
                for (int i = 0; i < coords.size()-1; i++) {
                    double[] c = coords.get(i);
                    wps.add(c[0] + "," + c[1]);
                }
                // Google limits length; cap to first 20 waypoints
                if (wps.size() > 20) wps = wps.subList(0, 20);
                sb.append("&waypoints=").append(android.text.TextUtils.join("|", wps));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    static double[] parseCoord(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String s = raw.trim();
        try { // JSON {lng,lat}
            org.json.JSONObject o = new org.json.JSONObject(s);
            if (o.has("lng") && o.has("lat")) {
                return new double[]{ o.optDouble("lng", Double.NaN), o.optDouble("lat", Double.NaN) };
            }
        } catch (Exception ignore) { }
        if (s.contains(",")) {
            String[] parts = s.split(",");
            if (parts.length>=2) {
                try {
                    double lat = Double.parseDouble(parts[0].trim());
                    double lng = Double.parseDouble(parts[1].trim());
                    return new double[]{ lng, lat };
                } catch (Exception ignore) {}
            }
        }
        return null;
    }
}

