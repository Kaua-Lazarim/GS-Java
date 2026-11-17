package br.com.fiap.territorio.dominio;

import br.com.fiap.territorio.excecao.DadosInvalidosException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimiaresTest {

    @Test
    void deveClassificarCadaFaixaNoNivelCorreto() {
        // Arrange
        Limiares limiares = new Limiares(20, 35, 50);

        // Act
        Optional<NivelAlerta> atencao = limiares.classificar(20.0);
        Optional<NivelAlerta> alerta = limiares.classificar(49.9);
        Optional<NivelAlerta> critico = limiares.classificar(50.0);

        // Assert
        assertEquals(Optional.of(NivelAlerta.ATENCAO), atencao);
        assertEquals(Optional.of(NivelAlerta.ALERTA), alerta);
        assertEquals(Optional.of(NivelAlerta.CRITICO), critico);
    }

    @Test
    void naoDeveClassificarValorAbaixoDoLimiarDeAtencao() {
        // Arrange
        Limiares limiares = new Limiares(20, 35, 50);

        // Act
        Optional<NivelAlerta> nivel = limiares.classificar(19.99);

        // Assert
        assertTrue(nivel.isEmpty());
    }

    @Test
    void deveRejeitarLimiaresForaDeOrdem() {
        // Arrange
        double atencao = 50;
        double alerta = 35;
        double critico = 20;

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> new Limiares(atencao, alerta, critico));

        // Assert
        assertTrue(erro.getMessage().contains("fora de ordem"));
    }

    @Test
    void deveInformarOLimiteDeCadaNivel() {
        // Arrange
        Limiares limiares = new Limiares(150, 220, 300);

        // Act
        double limiteAlerta = limiares.getLimite(NivelAlerta.ALERTA);

        // Assert
        assertEquals(220.0, limiteAlerta, 0.0001);
    }
}
