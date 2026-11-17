package br.com.fiap.territorio.persistencia;

import br.com.fiap.territorio.dominio.Medicao;
import br.com.fiap.territorio.excecao.PersistenciaException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Acesso à tabela medicao. Todo SQL é fixo e os valores entram por parâmetro
 * (?) no PreparedStatement. Conexão, statement e result set são fechados
 * pelo try-with-resources.
 */
public class MedicaoDAO {

    private static final String SQL_INSERIR = """
            INSERT INTO medicao (codigo_sensor, tipo_sensor, valor, unidade, data_hora)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String SQL_LISTAR_POR_SENSOR = """
            SELECT id, codigo_sensor, tipo_sensor, valor, unidade, data_hora
            FROM medicao
            WHERE codigo_sensor = ?
            ORDER BY data_hora, id
            """;

    private static final String SQL_ULTIMAS_POR_SENSOR = """
            SELECT id, codigo_sensor, tipo_sensor, valor, unidade, data_hora
            FROM medicao
            WHERE codigo_sensor = ?
            ORDER BY data_hora DESC, id DESC
            LIMIT ?
            """;

    private static final String SQL_CONTAR_POR_SENSOR = """
            SELECT COUNT(*) FROM medicao WHERE codigo_sensor = ?
            """;

    private final ConexaoBanco conexaoBanco;

    public MedicaoDAO(ConexaoBanco conexaoBanco) {
        this.conexaoBanco = conexaoBanco;
    }

    /** Grava a medição e devolve uma cópia com o id gerado pelo banco. */
    public Medicao salvar(Medicao medicao) {
        try (Connection conexao = conexaoBanco.abrir();
             PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, medicao.getCodigoSensor());
            ps.setString(2, medicao.getTipoSensor());
            ps.setDouble(3, medicao.getValor());
            ps.setString(4, medicao.getUnidade());
            ps.setTimestamp(5, Timestamp.valueOf(medicao.getDataHora()));
            ps.executeUpdate();

            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new PersistenciaException(
                            "O banco não devolveu o id da medição do sensor " + medicao.getCodigoSensor() + ".");
                }
                return medicao.comId(chaves.getLong(1));
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao gravar a medição do sensor " + medicao.getCodigoSensor() + ": " + e.getMessage(), e);
        }
    }

    /** Histórico completo de um sensor, da medição mais antiga para a mais recente. */
    public List<Medicao> listarPorSensor(String codigoSensor) {
        try (Connection conexao = conexaoBanco.abrir();
             PreparedStatement ps = conexao.prepareStatement(SQL_LISTAR_POR_SENSOR)) {
            ps.setString(1, codigoSensor);
            try (ResultSet rs = ps.executeQuery()) {
                return montarLista(rs);
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao listar o histórico do sensor " + codigoSensor + ": " + e.getMessage(), e);
        }
    }

    /**
     * As N medições mais recentes de um sensor, devolvidas em ordem
     * cronológica (mais antiga primeiro), que é o que as previsões esperam.
     */
    public List<Medicao> buscarUltimasPorSensor(String codigoSensor, int limite) {
        try (Connection conexao = conexaoBanco.abrir();
             PreparedStatement ps = conexao.prepareStatement(SQL_ULTIMAS_POR_SENSOR)) {
            ps.setString(1, codigoSensor);
            ps.setInt(2, limite);
            try (ResultSet rs = ps.executeQuery()) {
                List<Medicao> medicoes = montarLista(rs);
                Collections.reverse(medicoes);
                return medicoes;
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao buscar as últimas medições do sensor " + codigoSensor + ": " + e.getMessage(), e);
        }
    }

    public int contarPorSensor(String codigoSensor) {
        try (Connection conexao = conexaoBanco.abrir();
             PreparedStatement ps = conexao.prepareStatement(SQL_CONTAR_POR_SENSOR)) {
            ps.setString(1, codigoSensor);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao contar as medições do sensor " + codigoSensor + ": " + e.getMessage(), e);
        }
    }

    private List<Medicao> montarLista(ResultSet rs) throws SQLException {
        List<Medicao> medicoes = new ArrayList<>();
        while (rs.next()) {
            medicoes.add(new Medicao(
                    rs.getLong("id"),
                    rs.getString("codigo_sensor"),
                    rs.getString("tipo_sensor"),
                    rs.getDouble("valor"),
                    rs.getString("unidade"),
                    rs.getTimestamp("data_hora").toLocalDateTime()));
        }
        return medicoes;
    }
}
