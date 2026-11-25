package com.taskhabit.academic.tareas;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.taskhabit.academic.R;

import java.util.ArrayList;
import java.util.List;

public class TareaAdapter extends RecyclerView.Adapter<TareaAdapter.Fila> {

    /** La Activity implementa esto para enterarse de lo que el usuario toca en una fila. */
    public interface Escuchador {
        void onCambiarCompletada(Tarea tarea, boolean completada);
        void onEditar(Tarea tarea);
        void onEliminar(Tarea tarea);
    }

    private final List<Tarea> datos = new ArrayList<>();
    private final Escuchador escuchador;

    public TareaAdapter(Escuchador escuchador) {
        this.escuchador = escuchador;
    }

    public void setDatos(List<Tarea> nuevos) {
        datos.clear();
        datos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View vista = LayoutInflater.from(padre.getContext()).inflate(R.layout.item_tarea, padre, false);
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
        private final CheckBox cbCompletada;
        private final TextView tvTitulo, tvEtiquetas;
        private final ImageButton btnEliminar;

        Fila(@NonNull View vista) {
            super(vista);
            cbCompletada = vista.findViewById(R.id.cbCompletada);
            tvTitulo = vista.findViewById(R.id.tvTitulo);
            tvEtiquetas = vista.findViewById(R.id.tvEtiquetas);
            btnEliminar = vista.findViewById(R.id.btnEliminar);
        }

        void enlazar(Tarea tarea, Escuchador escuchador) {
            // Evita que el toggle anterior dispare el listener mientras reciclamos la vista.
            cbCompletada.setOnCheckedChangeListener(null);
            cbCompletada.setChecked(tarea.isCompletada());
            cbCompletada.setOnCheckedChangeListener((boton, marcado) ->
                    escuchador.onCambiarCompletada(tarea, marcado));

            tvTitulo.setText(tarea.getTitulo());
            tvTitulo.setPaintFlags(tarea.isCompletada()
                    ? tvTitulo.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                    : tvTitulo.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

            Context contexto = itemView.getContext();
            String tipo = tarea.esHabito()
                    ? contexto.getString(R.string.tipo_habito)
                    : contexto.getString(R.string.tipo_tarea);
            String prioridad = textoPrioridad(contexto, tarea.getPrioridad());
            tvEtiquetas.setText(contexto.getString(R.string.etiquetas_fila, tipo, prioridad));

            itemView.setOnClickListener(v -> escuchador.onEditar(tarea));
            btnEliminar.setOnClickListener(v -> escuchador.onEliminar(tarea));
        }

        private static String textoPrioridad(Context c, String prioridad) {
            if (Tarea.PRIORIDAD_ALTA.equals(prioridad)) return c.getString(R.string.prioridad_alta);
            if (Tarea.PRIORIDAD_MEDIA.equals(prioridad)) return c.getString(R.string.prioridad_media);
            return c.getString(R.string.prioridad_baja);
        }
    }
}
