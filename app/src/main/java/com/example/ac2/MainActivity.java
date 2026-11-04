package com.example.ac2;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private EditText edtTitulo, edtAutor, edtAno;
    private Spinner spGenero, spStatus, spFiltroGenero;
    private CheckBox chkFavorito, chkSomenteFavoritos;
    private Button btnSalvar;
    private RecyclerView recyclerLivros;

    private List<Livro> listaLivros = new ArrayList<>();
    private List<Livro> listaFiltrada = new ArrayList<>();

    private LivroAdapter adapter;
    private Livro livroEditando = null;

    private boolean operacaoEmAndamento = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    Insets teclado = insets.getInsets(
                            WindowInsetsCompat.Type.ime()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            Math.max(systemBars.bottom, teclado.bottom)
                    );

                    return insets;
                }
        );

        db = FirebaseFirestore.getInstance();

        edtTitulo = findViewById(R.id.edtTitulo);
        edtAutor = findViewById(R.id.edtAutor);
        edtAno = findViewById(R.id.edtAno);

        spGenero = findViewById(R.id.spGenero);
        spStatus = findViewById(R.id.spStatus);
        spFiltroGenero = findViewById(R.id.spFiltroGenero);

        chkFavorito = findViewById(R.id.chkFavorito);
        chkSomenteFavoritos = findViewById(R.id.chkSomenteFavoritos);

        btnSalvar = findViewById(R.id.btnSalvar);
        recyclerLivros = findViewById(R.id.recyclerLivros);

        configurarSpinner(spGenero, R.array.generos);
        configurarSpinner(spStatus, R.array.status_leitura);
        configurarSpinner(spFiltroGenero, R.array.filtro_generos);

        recyclerLivros.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LivroAdapter(listaFiltrada);
        recyclerLivros.setAdapter(adapter);

        adapter.setOnItemClickListener(livro -> editarLivro(livro));
        adapter.setOnItemLongClickListener(livro -> confirmarExclusao(livro));

        btnSalvar.setOnClickListener(v -> salvarLivro());

        spFiltroGenero.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        filtrarLivros();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

        chkSomenteFavoritos.setOnCheckedChangeListener(
                (buttonView, isChecked) -> filtrarLivros()
        );

        carregarLivros();
    }

    private void configurarSpinner(Spinner spinner, int opcoes) {
        ArrayAdapter<CharSequence> opcoesAdapter =
                ArrayAdapter.createFromResource(
                        this,
                        opcoes,
                        android.R.layout.simple_spinner_item
                );

        opcoesAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(opcoesAdapter);
    }

    private void salvarLivro() {
        if (operacaoEmAndamento) {
            return;
        }

        String titulo = edtTitulo.getText().toString().trim();
        String autor = edtAutor.getText().toString().trim();
        String textoAno = edtAno.getText().toString().trim();

        edtTitulo.setError(null);
        edtAutor.setError(null);
        edtAno.setError(null);

        if (titulo.isEmpty()) {
            edtTitulo.setError("Informe o título.");
            edtTitulo.requestFocus();
            return;
        }

        if (autor.isEmpty()) {
            edtAutor.setError("Informe o autor.");
            edtAutor.requestFocus();
            return;
        }

        if (spGenero.getSelectedItemPosition() <= 0) {
            Toast.makeText(
                    this,
                    "Selecione o gênero.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (textoAno.isEmpty()) {
            edtAno.setError("Informe o ano de publicação.");
            edtAno.requestFocus();
            return;
        }

        int ano;

        try {
            ano = Integer.parseInt(textoAno);
        } catch (NumberFormatException e) {
            edtAno.setError("Informe um ano numérico válido.");
            edtAno.requestFocus();
            return;
        }

        if (spStatus.getSelectedItemPosition() <= 0) {
            Toast.makeText(
                    this,
                    "Selecione o status de leitura.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String id = livroEditando == null
                ? null
                : livroEditando.getId();

        Livro livro = new Livro(
                id,
                titulo,
                autor,
                spGenero.getSelectedItem().toString(),
                ano,
                spStatus.getSelectedItem().toString(),
                chkFavorito.isChecked()
        );

        operacaoEmAndamento = true;
        btnSalvar.setEnabled(false);

        if (livroEditando == null) {
            db.collection("livros")
                    .add(livro)
                    .addOnSuccessListener(doc -> {
                        livro.setId(doc.getId());
                        finalizarSalvamento("Livro cadastrado!");
                    })
                    .addOnFailureListener(e -> mostrarErro("salvar", e));
        } else {
            db.collection("livros")
                    .document(livro.getId())
                    .set(livro)
                    .addOnSuccessListener(aVoid ->
                            finalizarSalvamento("Livro atualizado!")
                    )
                    .addOnFailureListener(e -> mostrarErro("atualizar", e));
        }
    }

    private void finalizarSalvamento(String mensagem) {
        operacaoEmAndamento = false;
        btnSalvar.setEnabled(true);

        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();

        limparCampos();
        carregarLivros();
    }

    private void limparCampos() {
        edtTitulo.setText("");
        edtAutor.setText("");
        edtAno.setText("");

        edtTitulo.setError(null);
        edtAutor.setError(null);
        edtAno.setError(null);

        spGenero.setSelection(0);
        spStatus.setSelection(0);

        chkFavorito.setChecked(false);

        livroEditando = null;
        btnSalvar.setText("Salvar livro");
    }

    private void carregarLivros() {
        db.collection("livros")
                .get()
                .addOnSuccessListener(query -> {
                    listaLivros.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        Livro livro = doc.toObject(Livro.class);
                        livro.setId(doc.getId());
                        listaLivros.add(livro);
                    }

                    filtrarLivros();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Erro ao carregar: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void filtrarLivros() {
        listaFiltrada.clear();

        String genero = spFiltroGenero.getSelectedItem().toString();

        for (Livro livro : listaLivros) {
            boolean generoCorreto =
                    spFiltroGenero.getSelectedItemPosition() == 0
                            || genero.equals(livro.getGenero());

            boolean favoritoCorreto =
                    !chkSomenteFavoritos.isChecked()
                            || livro.isFavorito();

            if (generoCorreto && favoritoCorreto) {
                listaFiltrada.add(livro);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void editarLivro(Livro livro) {
        if (operacaoEmAndamento) {
            return;
        }

        limparCampos();

        edtTitulo.setText(livro.getTitulo());
        edtAutor.setText(livro.getAutor());
        edtAno.setText(String.valueOf(livro.getAno()));

        selecionarOpcao(spGenero, livro.getGenero());
        selecionarOpcao(spStatus, livro.getStatus());

        chkFavorito.setChecked(livro.isFavorito());

        livroEditando = livro;
        btnSalvar.setText("Atualizar livro");

        ScrollView scrollFormulario = findViewById(R.id.scrollFormulario);
        scrollFormulario.smoothScrollTo(0, 0);
    }

    private void selecionarOpcao(Spinner spinner, String valor) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(valor)) {
                spinner.setSelection(i);
                return;
            }
        }

        spinner.setSelection(0);
    }

    private void confirmarExclusao(Livro livro) {
        if (operacaoEmAndamento) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Excluir livro")
                .setMessage("Deseja excluir " + livro.getTitulo() + "?")
                .setPositiveButton(
                        "Excluir",
                        (dialog, which) -> excluirLivro(livro)
                )
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void excluirLivro(Livro livro) {
        if (operacaoEmAndamento) {
            return;
        }

        operacaoEmAndamento = true;
        btnSalvar.setEnabled(false);

        db.collection("livros")
                .document(livro.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    operacaoEmAndamento = false;
                    btnSalvar.setEnabled(true);

                    if (livroEditando != null
                            && livroEditando.getId().equals(livro.getId())) {
                        limparCampos();
                    }

                    Toast.makeText(
                            this,
                            "Livro excluído!",
                            Toast.LENGTH_SHORT
                    ).show();

                    carregarLivros();
                })
                .addOnFailureListener(e -> mostrarErro("excluir", e));
    }

    private void mostrarErro(String acao, Exception erro) {
        operacaoEmAndamento = false;
        btnSalvar.setEnabled(true);

        Toast.makeText(
                this,
                "Erro ao " + acao + ": " + erro.getMessage(),
                Toast.LENGTH_LONG
        ).show();
    }
}