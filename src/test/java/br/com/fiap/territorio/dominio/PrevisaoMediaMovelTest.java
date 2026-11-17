package br.com.fiap.territorio.dominio;

import br.com.fiap.territorio.excecao.DadosInvalidosException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrevisaoMediaMovelTest {

    @Test
    void deveCalcularAMediaSomenteDasUltimasLeiturasDaJanela() {
        // Arrange
        PrevisaoMediaMovel estrategia = new PrevisaoMediaMovel(5, 5);
        List<Medicao> historico = historico(10, 20, 30, 40, 50, 60, 70);

        // Act
        Previsao previsao = estrategia.prever(historico);

        // Assert: (30 + 40 + 50 + 60 + 70) / 5 = 50; as duas primeiras ficam de fora
        assertTrue(previsao.isConfiavel());
        assertEquals(50.0, previsao.getValorPrevisto(), 0.0001);
        assertEquals("Média móvel", previsao.getEstrategia());
    }

    @Test
    void deveFicarSemPrevisaoQuandoHaMenosLeiturasQueOMinimo() {
        // Arrange
        PrevisaoMediaMovel estrategia = new PrevisaoMediaMovel(5, 5);
        List<Medicao> historico = historico(80, 90, 95, 99);

        // Act
        Previsao previsao = estrategia.prever(historico);

        // Assert
        assertFalse(previsao.isConfiavel());
        assertTrue(Double.isNaN(previsao.getValorPrevisto()));
        assertEquals(4, previsao.getLeiturasDisponiveis());
    }

    @Test
    void deveLancarExcecaoQuandoOHistoricoForNulo() {
        // Arrange
        PrevisaoMediaMovel estrategia = new PrevisaoMediaMovel(5, 5);

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> estrategia.prever(null));
    }

    @Test
    void deveRejeitarJanelaMenorQueDuasLeituras() {
        // Arrange
        int janelaInvalida = 1;

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> new PrevisaoMediaMovel(janelaInvalida, 5));
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
