package br.com.fiap.territorio.persistencia;

import br.com.fiap.territorio.dominio.Medicao;
import br.com.fiap.territorio.excecao.DadosInvalidosException;
import br.com.fiap.territorio.excecao.PersistenciaException;
import br.com.fiap.territorio.util.Formato;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exporta o histórico consultado para um arquivo CSV (item de bônus).
 * Usa ponto e vírgula como separador e vírgula decimal, que é como o
 * Excel em português abre sem precisar configurar nada.
 */
public class ExportadorCsv {

    private static final String CABECALHO = "id;codigo_sensor;tipo_sensor;valor;unidade;data_hora";
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Path exportar(List<Medicao> medicoes, Path arquivo) {
        if (medicoes == null || medicoes.isEmpty()) {
            throw new DadosInvalidosException("Não há medições para exportar.");
        }
        if (arquivo == null) {
            throw new DadosInvalidosException("Informe o arquivo de destino da exportação.");
        }

        Path destino = arquivo.toAbsolutePath();
        try {
            if (destino.getParent() != null) {
                Files.createDirectories(destino.getParent());
            }
            try (BufferedWriter escritor = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
                escritor.write(CABECALHO);
                escritor.newLine();
                for (Medicao medicao : medicoes) {
                    escritor.write(montarLinha(medicao));
                    escritor.newLine();
                }
            }
        } catch (IOException e) {
            throw new PersistenciaException(
                    "Não foi possível gravar o arquivo CSV em " + destino + ": " + e.getMessage(), e);
        }
        return destino;
    }

    private String montarLinha(Medicao medicao) {
        return String.join(";",
                String.valueOf(medicao.getId()),
                medicao.getCodigoSensor(),
                medicao.getTipoSensor(),
                Formato.numero(medicao.getValor()),
                medicao.getUnidade(),
                medicao.getDataHora().format(DATA_HORA));
    }
}
