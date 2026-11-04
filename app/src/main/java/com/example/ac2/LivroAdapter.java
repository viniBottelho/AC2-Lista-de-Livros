package com.example.ac2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LivroAdapter extends RecyclerView.Adapter<LivroAdapter.ViewHolder> {

    private List<Livro> livros;

    public interface OnItemClickListener {
        void onItemClick(Livro livro);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Livro livro);
    }

    private OnItemClickListener listener;
    private OnItemLongClickListener longListener;

    public LivroAdapter(List<Livro> livros) {
        this.livros = livros;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Livro livro = livros.get(position);

        holder.txt1.setText(livro.getTitulo());

        holder.txt2.setText(
                "Autor: " + livro.getAutor()
                        + "\nGênero: " + livro.getGenero()
                        + " | Ano: " + livro.getAno()
                        + "\nStatus: " + livro.getStatus()
                        + "\nFavorito: " + (livro.isFavorito() ? "Sim" : "Não")
        );

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(livro);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longListener != null) {
                longListener.onItemLongClick(livro);
            }

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return livros.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView txt1;
        TextView txt2;

        public ViewHolder(View itemView) {
            super(itemView);

            txt1 = itemView.findViewById(android.R.id.text1);
            txt2 = itemView.findViewById(android.R.id.text2);
        }
    }
}