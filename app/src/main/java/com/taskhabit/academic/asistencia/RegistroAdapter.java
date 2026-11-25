package com.taskhabit.academic.asistencia;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.taskhabit.academic.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Filas de la lista de asistencia: nombre, hora y distancia; si fue rechazada, también el motivo. */
public class RegistroAdapter extends RecyclerView.Adapter<RegistroAdapter.Fila> {

    private final List<RegistroAsistencia> datos = new ArrayList<>();

    public void setDatos(List<RegistroAsistencia> nuevos) {
        datos.clear();
        datos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View vista = LayoutInflater.from(padre.getContext()).inflate(R.layout.item_registro, padre, false);
        return new Fila(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        fila.enlazar(datos.get(posicion));
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    static class Fila extends RecyclerView.ViewHolder {
        private final ImageView ivEstado;
        private final TextView tvNombre, tvDetalle, tvMotivo;

        Fila(@NonNull View vista) {
            super(vista);
            ivEstado = vista.findViewById(R.id.ivEstado);
            tvNombre = vista.findViewById(R.id.tvNombre);
            tvDetalle = vista.findViewById(R.id.tvDetalle);
            tvMotivo = vista.findViewById(R.id.tvMotivo);
        }

        void enlazar(RegistroAsistencia r) {
            Context c = itemView.getContext();
            boolean ok = r.isValida();
            int color = ContextCompat.getColor(c, ok ? R.color.lista_ok : R.color.lista_error);

            ivEstado.setImageResource(ok ? R.drawable.ic_check_circle : R.drawable.ic_error);
            ivEstado.setImageTintList(ColorStateList.valueOf(color));

            tvNombre.setText(r.getNombreEstudiante());
            String hora = DateFormat.getTimeFormat(c).format(new Date(r.getFechaHora()));
            tvDetalle.setText(c.getString(R.string.fila_detalle, hora, Math.round(r.getDistanciaMetros())));

            if (ok) {
                tvMotivo.setVisibility(View.GONE);
            } else {
                tvMotivo.setText(textoMotivo(c, r.getMotivo()));
                tvMotivo.setTextColor(color);
                tvMotivo.setVisibility(View.VISIBLE);
            }
        }

        private static String textoMotivo(Context c, String motivo) {
            try {
                switch (AttendanceValidator.Motivo.valueOf(motivo)) {
                    case QR_VENCIDO:
                        return c.getString(R.string.lista_motivo_qr_vencido);
                    case FUERA_DE_RADIO:
                        return c.getString(R.string.lista_motivo_fuera_de_radio);
                    case UBICACION_SIMULADA:
                        return c.getString(R.string.lista_motivo_ubicacion_simulada);
                    default:
                        return "";
                }
            } catch (IllegalArgumentException e) {
                return motivo;
            }
        }
    }
}
