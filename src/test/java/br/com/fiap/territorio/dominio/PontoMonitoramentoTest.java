package br.com.fiap.territorio.dominio;

import br.com.fiap.territorio.excecao.DadosInvalidosException;
import br.com.fiap.territorio.excecao.FonteIndisponivelException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PontoMonitoramentoTest {

    @Test
    void deveFicarOperacionalQuandoTodosOsSensoresFuncionam() {
        // Arrange
        PontoMonitoramento ponto = novoPonto();
        ponto.adicionarSensor(new SensorPluviometro("PLU-01"));
        ponto.adicionarSensor(new SensorNivelCorrego("NIV-01"));

        // Act
        StatusComponente status = ponto.verificarStatus();

        // Assert
        assertEquals(StatusComponente.OPERACIONAL, status);
    }

    @Test
    void deveFicarDegradadoQuandoParteDosSensoresEstaDesativada() {
        // Arrange
        PontoMonitoramento ponto = novoPonto();
        SensorPluviometro pluviometro = new SensorPluviometro("PLU-01");
        ponto.adicionarSensor(pluviometro);
        ponto.adicionarSensor(new SensorNivelCorrego("NIV-01"));
        pluviometro.desativar();

        // Act
        StatusComponente status = ponto.verificarStatus();

        // Assert
        assertEquals(StatusComponente.DEGRADADO, status);
        assertEquals(StatusComponente.DEGRADADO, ponto.getStatus());
    }

    @Test
    void deveFicarInoperanteQuandoNaoTemSensor() {
        // Arrange
        PontoMonitoramento ponto = novoPonto();

        // Act
        StatusComponente status = ponto.verificarStatus();

        // Assert
        assertEquals(StatusComponente.INOPERANTE, status);
    }

    @Test
    void naoDeveAceitarDoisSensoresComOMesmoCodigo() {
        // Arrange
        PontoMonitoramento ponto = novoPonto();
        ponto.adicionarSensor(new SensorPluviometro("PLU-01"));

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> ponto.adicionarSensor(new SensorPluviometro("plu-01")));
    }

    @Test
    void sensorDesativadoDeveLancarFonteIndisponivelAoSerLido() {
        // Arrange
        SensorUmidadeSolo sensor = new SensorUmidadeSolo("UMI-01");
        sensor.desativar();

        // Act + Assert
        assertFalse(sensor.verificarFuncionamento());
        assertThrows(FonteIndisponivelException.class, sensor::lerValor);
    }

    private static PontoMonitoramento novoPonto() {
        return new PontoMonitoramento(1, "Ponte da Rua das Palmeiras", "Baixada do Córrego Azul",
                "A rua alaga quando o córrego transborda.");
    }
}
