package br.com.fiap.territorio.dominio;

import br.com.fiap.territorio.excecao.DadosInvalidosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DadosTerritorioTest {

    private static final String CODIGO_CORRETO = "VILA2026";

    @Mock
    private CatalogoLimiares catalogoLimiares;

    private DadosTerritorio territorio;

    @BeforeEach
    void preparar() {
        territorio = new DadosTerritorio("Vila Esperança", "Zona Leste", 12500, 480, CODIGO_CORRETO,
                catalogoLimiares);
    }

    @Test
    void deveRejeitarPopulacaoNegativaEManterOValorAnterior() {
        // Arrange
        int populacaoInvalida = -1;

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> territorio.setPopulacaoEstimada(populacaoInvalida));

        // Assert
        assertTrue(erro.getMessage().contains("negativa"));
        assertEquals(12500, territorio.getPopulacaoEstimada());
    }

    @Test
    void naoDeveAlterarLimiaresComCodigoDeAcessoErrado() {
        // Arrange
        String codigoErrado = "1234";

        // Act
        assertThrows(DadosInvalidosException.class,
                () -> territorio.alterarLimiares(codigoErrado, SensorPluviometro.TIPO, 10, 20, 30));

        // Assert
        verify(catalogoLimiares, never()).definirLimiares(anyString(), any(Limiares.class));
    }

    @Test
    void deveAlterarLimiaresQuandoOCodigoEstaCorreto() {
        // Arrange
        ArgumentCaptor<Limiares> captor = ArgumentCaptor.forClass(Limiares.class);

        // Act
        territorio.alterarLimiares(CODIGO_CORRETO, SensorPluviometro.TIPO, 15, 30, 45);

        // Assert
        verify(catalogoLimiares).definirLimiares(eq(SensorPluviometro.TIPO), captor.capture());
        assertEquals(15.0, captor.getValue().getAtencao(), 0.0001);
        assertEquals(30.0, captor.getValue().getAlerta(), 0.0001);
        assertEquals(45.0, captor.getValue().getCritico(), 0.0001);
    }

    @Test
    void naoDeveGravarLimiaresForaDeOrdemMesmoComCodigoCorreto() {
        // Arrange
        double atencao = 45;
        double alerta = 30;
        double critico = 15;

        // Act
        assertThrows(DadosInvalidosException.class,
                () -> territorio.alterarLimiares(CODIGO_CORRETO, SensorPluviometro.TIPO, atencao, alerta, critico));

        // Assert
        verify(catalogoLimiares, never()).definirLimiares(anyString(), any(Limiares.class));
    }
}
