package br.com.fiap.territorio.persistencia;

import br.com.fiap.territorio.dominio.Alerta;
import br.com.fiap.territorio.dominio.NivelAlerta;
import br.com.fiap.territorio.excecao.PersistenciaException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso à tabela alerta, no mesmo esquema do MedicaoDAO:
 * SQL fixo, parâmetros com ? e recursos fechados por try-with-resources.
 */
public class AlertaDAO {

    private static final String SQL_INSERIR = """
            INSERT INTO alerta (codigo_sensor, tipo_sensor, nivel, valor_previsto, limite_estourado,
                                estrategia, justificativa, data_hora)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_LISTAR_TODOS = """
            SELECT id, codigo_sensor, tipo_sensor, nivel, valor_previsto, limite_estourado,
                   estrategia, justificativa, data_hora
            FROM alerta
            ORDER BY data_hora DESC, id DESC
            """;

    private static final String SQL_CONTAR = """
            SELECT COUNT(*) FROM alerta
            """;

    private final ConexaoBanco conexaoBanco;

    public AlertaDAO(ConexaoBanco conexaoBanco) {
        this.conexaoBanco = conexaoBanco;
    }

    /** Grava o alerta e devolve uma cópia com o id gerado pelo banco. */
    public Alerta salvar(Alerta alerta) {
        try (Connection conexao = conexaoBanco.abrir();
             PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, alerta.getCodigoSensor());
            ps.setString(2, alerta.getTipoSensor());
            ps.setString(3, alerta.getNivel().name());
            ps.setDouble(4, alerta.getValorPrevisto());
            ps.setDouble(5, alerta.getLimiteEstourado());
            ps.setString(6, alerta.getEstrategia());
            ps.setString(7, alerta.getJustificativa());
            ps.setTimestamp(8, Timestamp.valueOf(alerta.getDataHora()));
            ps.executeUpdate();

            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new PersistenciaException(
                            "O banco não devolveu o id do alerta do sensor " + alerta.getCodigoSensor() + ".");
                }
                return alerta.comId(chaves.getLong(1));
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao gravar o alerta do sensor " + alerta.getCodigoSensor() + ": " + e.getMessage(), e);
        }
    }

    /** Todos os alertas já emitidos, do mais recente para o mais antigo. */
    public List<Alerta> listarTodos() {
        try (Connection conexao = conexaoBanco.abrir();
             PreparedStatement ps = conexao.prepareStatement(SQL_LISTAR_TODOS);
             ResultSet rs = ps.executeQuery()) {
            List<Alerta> alertas = new ArrayList<>();
            while (rs.next()) {
                alertas.add(new Alerta(
                        rs.getLong("id"),
                        rs.getString("codigo_sensor"),
                        rs.getString("tipo_sensor"),
                        NivelAlerta.valueOf(rs.getString("nivel")),
                        rs.getDouble("valor_previsto"),
                        rs.getDouble("limite_estourado"),
                        rs.getString("estrategia"),
                        rs.getString("justificativa"),
                        rs.getTimestamp("data_hora").toLocalDateTime()));
            }
            return alertas;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar os alertas emitidos: " + e.getMessage(), e);
        }
    }

    public int contar() {
        try (Connection conexao = conexaoBanco.abrir();
             PreparedStatement ps = conexao.prepareStatement(SQL_CONTAR);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao contar os alertas emitidos: " + e.getMessage(), e);
        }
    }
}
