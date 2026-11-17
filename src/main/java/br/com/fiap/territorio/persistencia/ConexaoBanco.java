package br.com.fiap.territorio.persistencia;

import br.com.fiap.territorio.excecao.PersistenciaException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Sabe abrir conexões com o banco e criar as tabelas na primeira execução.
 * Os DAOs pedem uma conexão nova a cada operação e fecham com try-with-resources.
 */
public class ConexaoBanco {

    private static final String ARQUIVO_ESQUEMA = "/schema.sql";

    private final String url;
    private final String usuario;
    private final String senha;

    public ConexaoBanco(String url, String usuario, String senha) {
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
    }

    public Connection abrir() throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }

    /** Executa o schema.sql. Como usa IF NOT EXISTS, pode rodar em toda inicialização. */
    public void criarTabelas() {
        List<String> comandos = lerComandosDoEsquema();
        try (Connection conexao = abrir();
             Statement statement = conexao.createStatement()) {
            for (String comando : comandos) {
                statement.execute(comando);
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Não foi possível preparar o banco de dados em " + url + ": " + e.getMessage(), e);
        }
    }

    private List<String> lerComandosDoEsquema() {
        String conteudo;
        try (InputStream entrada = ConexaoBanco.class.getResourceAsStream(ARQUIVO_ESQUEMA)) {
            if (entrada == null) {
                throw new PersistenciaException(
                        "O arquivo " + ARQUIVO_ESQUEMA + " não foi encontrado em src/main/resources.");
            }
            conteudo = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenciaException("Falha ao ler o arquivo " + ARQUIVO_ESQUEMA + ": " + e.getMessage(), e);
        }

        // tira as linhas de comentário e separa os comandos pelo ponto e vírgula
        StringBuilder semComentarios = new StringBuilder();
        for (String linha : conteudo.split("\\R")) {
            if (!linha.trim().startsWith("--")) {
                semComentarios.append(linha).append('\n');
            }
        }

        List<String> comandos = new ArrayList<>();
        for (String comando : semComentarios.toString().split(";")) {
            if (!comando.isBlank()) {
                comandos.add(comando.trim());
            }
        }
        return comandos;
    }
}
