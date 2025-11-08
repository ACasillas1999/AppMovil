package com.example.app_pedidos.network;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;

public class Utf8JsonArrayRequest extends JsonArrayRequest {

    public Utf8JsonArrayRequest(int method, String url, JSONArray jsonRequest,
                                Response.Listener<JSONArray> listener,
                                Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
    }

    public Utf8JsonArrayRequest(String url, Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
        super(url, listener, errorListener);
    }

    @Override
    protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
        try {
            String utf8 = new String(response.data, "UTF-8");
            return Response.success(new JSONArray(utf8), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            return Response.error(new ParseError(e));
        }
    }
}

