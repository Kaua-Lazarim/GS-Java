package br.com.fiap.territorio.dominio;

import br.com.fiap.territorio.excecao.DadosInvalidosException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicaoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 10, 14, 30);

    @Test
    void deveCriarMedicaoValidaAindaSemId() {
        // Arrange
        double valor = 18.4;

        // Act
        Medicao medicao = new Medicao("PLU-01", SensorPluviometro.TIPO, valor, "mm/h", AGORA);

        // Assert
        assertNull(medicao.getId());
        assertEquals("PLU-01", medicao.getCodigoSensor());
        assertEquals(18.4, medicao.getValor(), 0.0001);
        assertEquals(AGORA, medicao.getDataHora());
    }

    @Test
    void deveRejeitarMedicaoNegativa() {
        // Arrange
        double chuvaNegativa = -3.0;

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> new Medicao("PLU-01", SensorPluviometro.TIPO, chuvaNegativa, "mm/h", AGORA));

        // Assert
        assertTrue(erro.getMessage().contains("negativa"));
        assertTrue(erro.getMessage().contains("PLU-01"));
    }

    @Test
    void deveRejeitarMedicaoSemCodigoDeSensor() {
        // Arrange
        String codigoEmBranco = "   ";

        // Act + Assert
        assertThrows(DadosInvalidosException.class,
                () -> new Medicao(codigoEmBranco, SensorPluviometro.TIPO, 10.0, "mm/h", AGORA));
    }

    @Test
    void deveManterOsDadosAoReceberIdDoBanco() {
        // Arrange
        Medicao nova = new Medicao("NIV-01", SensorNivelCorrego.TIPO, 132.0, "cm", AGORA);

        // Act
        Medicao gravada = nova.comId(42L);

        // Assert
        assertEquals(42L, gravada.getId());
        assertEquals(nova.getValor(), gravada.getValor(), 0.0001);
        assertEquals(nova.getCodigoSensor(), gravada.getCodigoSensor());
    }
}
