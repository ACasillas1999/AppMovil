package com.example.app_pedidos.ui.home;

public class Pedido {
    public final String id;
    public final String sucursal;
    public final String nombreCliente;
    public final String estado;
    public final String fechaRecepcion;

    public Pedido(String id, String sucursal, String nombreCliente, String estado, String fechaRecepcion) {
        this.id = id;
        this.sucursal = sucursal;
        this.nombreCliente = nombreCliente;
        this.estado = estado;
        this.fechaRecepcion = fechaRecepcion;
    }
}

