package com.example.app_pedidos.ui.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_pedidos.R;
import com.example.app_pedidos.ui.Pedido.DetallePedidoActivity;

class PedidoDiff extends DiffUtil.ItemCallback<Pedido> {
    @Override
    public boolean areItemsTheSame(@NonNull Pedido oldItem, @NonNull Pedido newItem) {
        return oldItem.id.equals(newItem.id);
    }

    @Override
    public boolean areContentsTheSame(@NonNull Pedido oldItem, @NonNull Pedido newItem) {
        return oldItem.sucursal.equals(newItem.sucursal)
                && oldItem.nombreCliente.equals(newItem.nombreCliente)
                && oldItem.estado.equals(newItem.estado)
                && oldItem.fechaRecepcion.equals(newItem.fechaRecepcion);
    }
}

public class PedidosAdapter extends ListAdapter<Pedido, PedidosAdapter.PedidoViewHolder> {

    public interface OnPedidoClickListener {
        void onVerDetallesClicked(int position);
    }

    private final OnPedidoClickListener listener;

    public PedidosAdapter(OnPedidoClickListener listener) {
        super(new PedidoDiff());
        this.listener = listener;
    }

    @NonNull
    @Override
    public PedidoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pedido, parent, false);
        return new PedidoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PedidoViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class PedidoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgSucursal;
        private final TextView txtId;
        private final TextView txtCliente;
        private final TextView txtEstado;
        private final TextView txtFecha;
        private final Button btnVerDetalles;

        PedidoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSucursal = itemView.findViewById(R.id.imgSucursal);
            txtId = itemView.findViewById(R.id.txtId);
            txtCliente = itemView.findViewById(R.id.txtCliente);
            txtEstado = itemView.findViewById(R.id.txtEstado);
            txtFecha = itemView.findViewById(R.id.txtFecha);
            btnVerDetalles = itemView.findViewById(R.id.btnVerDetalles);
        }

        void bind(Pedido pedido, OnPedidoClickListener listener) {
            Context ctx = itemView.getContext();
            txtId.setText("ID: " + pedido.id);
            txtCliente.setText("Cliente: " + pedido.nombreCliente);
            txtEstado.setText("Estado: " + pedido.estado);
            txtFecha.setText("Fecha Recepcion: " + pedido.fechaRecepcion);

            int imgRes = mapSucursalToDrawable(pedido.sucursal);
            imgSucursal.setImageResource(imgRes);

            int filterColor = mapEstadoColor(pedido.estado);
            imgSucursal.setColorFilter(filterColor, PorterDuff.Mode.SRC_IN);

            btnVerDetalles.setOnClickListener(v -> listener.onVerDetallesClicked(getBindingAdapterPosition()));
        }

        private int mapSucursalToDrawable(String sucursal) {
            if (sucursal == null) return R.drawable.gabl;
            switch (sucursal) {
                case "DEASA":
                    return R.drawable.deasaazz;
                case "DIMEGSA":
                    return R.drawable.dimegsa;
                case "AIESA":
                    return R.drawable.aiesa;
                case "SEGSA":
                    return R.drawable.segsa;
                case "FESA":
                    return R.drawable.fesa;
                case "TAPATIA":
                    return R.drawable.eitsa;
                case "GABSA":
                    return R.drawable.gabl;
                case "ILUMINACION":
                    return R.drawable.ilum;
                case "VALLARTA":
                    return R.drawable.gabl;
                default:
                    return R.drawable.gabl;
            }
        }

        private int mapEstadoColor(String estado) {
            if (estado == null) return Color.WHITE;
            switch (estado) {
                case "ACTIVO":
                    return Color.parseColor("#576977");
                case "EN RUTA":
                    return Color.parseColor("#7A5D3D");
                case "REPROGRAMADO":
                    return Color.parseColor("#715C5B");
                case "EN TIENDA":
                    return Color.parseColor("#78785E");
                default:
                    return Color.WHITE;
            }
        }
    }
}

