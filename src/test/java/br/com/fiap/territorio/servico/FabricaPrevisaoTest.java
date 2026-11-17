package br.com.fiap.territorio.servico;

import br.com.fiap.territorio.dominio.EstrategiaPrevisao;
import br.com.fiap.territorio.dominio.PrevisaoComparacao;
import br.com.fiap.territorio.dominio.PrevisaoMediaMovel;
import br.com.fiap.territorio.excecao.DadosInvalidosException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricaPrevisaoTest {

    private final ConfiguracaoSistema configuracao = ConfiguracaoSistema.getInstancia();

    @Test
    void deveCriarMediaMovelComAJanelaDaConfiguracao() {
        // Arrange
        String chave = "media_movel";

        // Act
        EstrategiaPrevisao estrategia = FabricaPrevisao.criar(chave, configuracao);

        // Assert
        PrevisaoMediaMovel mediaMovel = assertInstanceOf(PrevisaoMediaMovel.class, estrategia);
        assertEquals(configuracao.getJanelaMediaMovel(), mediaMovel.getJanela());
    }

    @Test
    void deveCriarAsEstrategiasAtivasNaOrdemDoArquivoDeConfiguracao() {
        // Arrange
        List<String> chaves = configuracao.getEstrategiasAtivas();

        // Act
        List<EstrategiaPrevisao> estrategias = FabricaPrevisao.criarAtivas(configuracao);

        // Assert
        assertEquals(chaves.size(), estrategias.size());
        assertInstanceOf(PrevisaoMediaMovel.class, estrategias.get(0));
        assertInstanceOf(PrevisaoComparacao.class, estrategias.get(1));
    }

    @Test
    void deveRejeitarChaveDeEstrategiaDesconhecida() {
        // Arrange
        String chaveInexistente = "ADIVINHACAO";

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> FabricaPrevisao.criar(chaveInexistente, configuracao));

        // Assert
        assertTrue(erro.getMessage().contains("ADIVINHACAO"));
    }

    @Test
    void configuracaoDeveSerSempreAMesmaInstancia() {
        // Arrange
        ConfiguracaoSistema primeira = ConfiguracaoSistema.getInstancia();

        // Act
        ConfiguracaoSistema segunda = ConfiguracaoSistema.getInstancia();

        // Assert
        assertSame(primeira, segunda);
    }
}
