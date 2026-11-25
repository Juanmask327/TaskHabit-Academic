package com.taskhabit.academic.reportes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.taskhabit.academic.R;

import java.util.ArrayList;
import java.util.List;

public class ReporteAdapter extends RecyclerView.Adapter<ReporteAdapter.Fila> {

    /** La Activity implementa esto para enterarse de lo que el usuario toca en una fila. */
    public interface Escuchador {
        void onEditar(Reporte reporte);
        void onEliminar(Reporte reporte);
    }

    private final List<Reporte> datos = new ArrayList<>();
    private final Escuchador escuchador;

    public ReporteAdapter(Escuchador escuchador) {
        this.escuchador = escuchador;
    }

    public void setDatos(List<Reporte> nuevos) {
        datos.clear();
        datos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View vista = LayoutInflater.from(padre.getContext()).inflate(R.layout.item_reporte, padre, false);
        return new Fila(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        fila.enlazar(datos.get(posicion), escuchador);
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    static class Fila extends RecyclerView.ViewHolder {
        private final TextView tvTitulo, tvPeriodo, tvPorcentaje, tvDetalle;
        private final ImageButton btnEliminar;

        Fila(@NonNull View vista) {
            super(vista);
            tvTitulo = vista.findViewById(R.id.tvTitulo);
            tvPeriodo = vista.findViewById(R.id.tvPeriodo);
            tvPorcentaje = vista.findViewById(R.id.tvPorcentaje);
            tvDetalle = vista.findViewById(R.id.tvDetalle);
            btnEliminar = vista.findViewById(R.id.btnEliminar);
        }

        void enlazar(Reporte reporte, Escuchador escuchador) {
            Context contexto = itemView.getContext();
            tvTitulo.setText(reporte.getTitulo());
            tvPeriodo.setText(contexto.getString(R.string.reporte_periodo,
                    FormatoFechas.paraPantalla(reporte.getFechaInicio()),
                    FormatoFechas.paraPantalla(reporte.getFechaFin())));
            tvPorcentaje.setText(contexto.getString(R.string.reporte_porcentaje,
                    Math.round(reporte.getPorcentajeCumplimiento())));
            tvDetalle.setText(contexto.getString(R.string.reporte_detalle,
                    reporte.getTareasCompletadas(), reporte.getTotalTareas(),
                    reporte.getHabitosCompletados(), reporte.getTotalHabitos()));

            itemView.setOnClickListener(v -> escuchador.onEditar(reporte));
            btnEliminar.setOnClickListener(v -> escuchador.onEliminar(reporte));
        }
    }
}
