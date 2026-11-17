package br.com.fiap.territorio.dominio;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrevisaoComparacaoTest {

    @Test
    void deveProjetarAltaQuandoOPeriodoAtualSuperaOAnterior() {
        // Arrange
        PrevisaoComparacao estrategia = new PrevisaoComparacao(3, 5);
        List<Medicao> historico = historico(10, 10, 10, 20, 20, 20);

        // Act
        Previsao previsao = estrategia.prever(historico);

        // Assert: média atual 20, anterior 10, variação +10 -> previsto 30
        assertTrue(previsao.isConfiavel());
        assertEquals(30.0, previsao.getValorPrevisto(), 0.0001);
    }

    @Test
    void deveRepetirAMediaQuandoNaoHaVariacaoEntreOsPeriodos() {
        // Arrange
        PrevisaoComparacao estrategia = new PrevisaoComparacao(3, 5);
        List<Medicao> historico = historico(15, 15, 15, 15, 15, 15);

        // Act
        Previsao previsao = estrategia.prever(historico);

        // Assert
        assertEquals(15.0, previsao.getValorPrevisto(), 0.0001);
    }

    @Test
    void naoDevePreverValorNegativoEmQuedaForte() {
        // Arrange
        PrevisaoComparacao estrategia = new PrevisaoComparacao(3, 5);
        List<Medicao> historico = historico(30, 30, 30, 5, 5, 5);

        // Act
        Previsao previsao = estrategia.prever(historico);

        // Assert: 5 + (5 - 30) daria -20, mas chuva não fica negativa
        assertEquals(0.0, previsao.getValorPrevisto(), 0.0001);
    }

    @Test
    void deveExigirDoisPeriodosCompletosParaComparar() {
        // Arrange
        PrevisaoComparacao estrategia = new PrevisaoComparacao(3, 1);
        List<Medicao> historico = historico(40, 45, 50, 55, 60);

        // Act
        Previsao previsao = estrategia.prever(historico);

        // Assert
        assertEquals(6, estrategia.getMinimoLeituras());
        assertFalse(previsao.isConfiavel());
    }

    private static List<Medicao> historico(double... valores) {
        List<Medicao> medicoes = new ArrayList<>();
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 10, 8, 0);
        for (int i = 0; i < valores.length; i++) {
            medicoes.add(new Medicao("PLU-01", SensorPluviometro.TIPO, valores[i], "mm/h", inicio.plusHours(i)));
        }
        return medicoes;
    }
}
